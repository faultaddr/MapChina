#!/usr/bin/env ruby

require "json"
require "nokogiri"

if ARGV.empty? || ARGV.length > 2
  warn "Usage: analyze_xctrace.rb <time-profile.xml> [potential-hangs.xml]"
  exit 2
end

time_profile_path = File.expand_path(ARGV[0])
hangs_path = ARGV[1] && File.expand_path(ARGV[1])

unless File.file?(time_profile_path)
  warn "Missing Time Profiler export: #{time_profile_path}"
  exit 2
end

thread_names = {}
weights_ns = {}
frame_names = {}
backtraces = {}
leaf_weights = Hash.new(0)
application_weights = Hash.new(0)
category_weights = Hash.new(0)
main_thread_weight_ns = 0
main_thread_samples = 0

categories = {
  "douglas_peucker" => /com\.mapchina\.map\.DouglasPeucker/,
  "geo_path_precompute" => /com\.mapchina\.map\.GeoPathCache\.precomputeRings/,
  "geo_path_build_paths" => /com\.mapchina\.map\.GeoPathCache\.buildPaths/,
  "geo_path_build_if_changed" => /com\.mapchina\.map\.GeoPathCache#buildIfChanged/,
  "text_measure" => /TextMeasurer|MultiParagraph|Paragraph.*measure/i,
  "china_map_draw" => /com\.mapchina\.map\.ChinaMapView/
}.freeze

document = Nokogiri::XML(File.read(time_profile_path)) { |config| config.strict.nonet }
rows = document.xpath("//row")

rows.each do |row|
  thread_node = row.at_xpath("./thread")
  thread_names[thread_node["id"]] = thread_node["fmt"] if thread_node&.[]("id")

  weight_node = row.at_xpath("./weight")
  weights_ns[weight_node["id"]] = weight_node.text.to_i if weight_node&.[]("id")

  backtrace_node = row.at_xpath("./backtrace")
  next unless backtrace_node&.[]("id")

  frame_ids = backtrace_node.xpath("./frame").map do |frame_node|
    frame_id = frame_node["id"] || frame_node["ref"]
    frame_names[frame_id] = frame_node["name"] if frame_node["id"]
    frame_id
  end
  backtraces[backtrace_node["id"]] = frame_ids.freeze
end

rows.each do |row|
  thread_node = row.at_xpath("./thread")
  next unless thread_node

  thread_id = thread_node["id"] || thread_node["ref"]
  thread_name = thread_names[thread_id]
  next unless thread_name&.start_with?("Main Thread")

  weight_node = row.at_xpath("./weight")
  next unless weight_node

  weight_id = weight_node["id"] || weight_node["ref"]
  weight_ns = weights_ns.fetch(weight_id, 0)

  backtrace_node = row.at_xpath("./backtrace")
  next unless backtrace_node

  backtrace_id = backtrace_node["id"] || backtrace_node["ref"]
  frames = backtraces.fetch(backtrace_id, []).map { |frame_id| frame_names[frame_id] }.compact

  main_thread_samples += 1
  main_thread_weight_ns += weight_ns
  leaf_weights[frames.first] += weight_ns if frames.first

  unique_frames = frames.uniq
  unique_frames.grep(/com\.mapchina\./).each do |frame_name|
    application_weights[frame_name] += weight_ns
  end
  categories.each do |category, pattern|
    category_weights[category] += weight_ns if unique_frames.any? { |name| name.match?(pattern) }
  end
end

def ranked(weights, total_ns, limit: 25)
  weights.sort_by { |_, weight_ns| -weight_ns }.first(limit).map do |name, weight_ns|
    {
      "name" => name,
      "weight_ms" => (weight_ns / 1_000_000.0).round(3),
      "main_thread_percent" => total_ns.zero? ? 0.0 : (weight_ns.fdiv(total_ns) * 100).round(3)
    }
  end
end

hang_count = 0
hang_duration_ms = 0.0
if hangs_path
  unless File.file?(hangs_path)
    warn "Missing potential-hangs export: #{hangs_path}"
    exit 2
  end

  hangs_document = Nokogiri::XML(File.read(hangs_path)) { |config| config.strict.nonet }
  hang_rows = hangs_document.xpath("//row")
  hang_count = hang_rows.length
  hang_duration_ms = hang_rows.sum do |row|
    duration = row.at_xpath("./duration")
    duration ? duration.text.to_f / 1_000_000.0 : 0.0
  end
end

category_summary = categories.keys.to_h do |category|
  weight_ns = category_weights[category]
  [
    category,
    {
      "weight_ms" => (weight_ns / 1_000_000.0).round(3),
      "main_thread_percent" => main_thread_weight_ns.zero? ? 0.0 : (weight_ns.fdiv(main_thread_weight_ns) * 100).round(3)
    }
  ]
end

result = {
  "time_profile" => time_profile_path,
  "potential_hangs" => hangs_path,
  "main_thread" => {
    "samples" => main_thread_samples,
    "weight_ms" => (main_thread_weight_ns / 1_000_000.0).round(3)
  },
  "hangs" => {
    "count" => hang_count,
    "duration_ms" => hang_duration_ms.round(3)
  },
  "categories" => category_summary,
  "top_application_frames" => ranked(application_weights, main_thread_weight_ns),
  "top_leaf_frames" => ranked(leaf_weights, main_thread_weight_ns)
}

puts JSON.pretty_generate(result)
