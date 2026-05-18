#!/usr/bin/env python3
"""
爬取中国所有 4A/5A 景点数据（名称、经纬度、所属省县市）

策略：按城市搜索所有风景名胜 POI，通过 biz_ext.level 和 keytag 字段过滤 A 级景区。

输出文件:
  - attractions.json: 完整 JSON 数据
  - attractions.csv:  CSV 格式，便于查看和导入
  - seed_data.kt:     可直接粘贴到 DataSeeder.kt 的 Kotlin 代码

用法:
  python3 scrape_attractions.py --key YOUR_AMAP_API_KEY [--output-dir ./output]
"""

import argparse
import csv
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request
from typing import Optional


# 全国地级市（高德 adcode）—— 按省份分组，避免省级搜索结果过多被截断
CITIES = [
    # 北京
    ("110000", "北京"),
    # 天津
    ("120000", "天津"),
    # 河北
    ("130100", "石家庄"), ("130200", "唐山"), ("130300", "秦皇岛"),
    ("130400", "邯郸"), ("130500", "邢台"), ("130600", "保定"),
    ("130700", "张家口"), ("130800", "承德"), ("130900", "沧州"),
    ("131000", "廊坊"), ("131100", "衡水"),
    # 山西
    ("140100", "太原"), ("140200", "大同"), ("140300", "阳泉"),
    ("140400", "长治"), ("140500", "晋城"), ("140600", "朔州"),
    ("140700", "晋中"), ("140800", "运城"), ("140900", "忻州"),
    ("141000", "临汾"), ("141100", "吕梁"),
    # 内蒙古
    ("150100", "呼和浩特"), ("150200", "包头"), ("150300", "乌海"),
    ("150400", "赤峰"), ("150500", "通辽"), ("150600", "鄂尔多斯"),
    ("150700", "呼伦贝尔"), ("150800", "巴彦淖尔"), ("150900", "乌兰察布"),
    ("152200", "兴安盟"), ("152500", "锡林郭勒盟"), ("152900", "阿拉善盟"),
    # 辽宁
    ("210100", "沈阳"), ("210200", "大连"), ("210300", "鞍山"),
    ("210400", "抚顺"), ("210500", "本溪"), ("210600", "丹东"),
    ("210700", "锦州"), ("210800", "营口"), ("210900", "阜新"),
    ("211000", "辽阳"), ("211100", "盘锦"), ("211200", "铁岭"),
    ("211300", "朝阳"), ("211400", "葫芦岛"),
    # 吉林
    ("220100", "长春"), ("220200", "吉林"), ("220300", "四平"),
    ("220400", "辽源"), ("220500", "通化"), ("220600", "白山"),
    ("220700", "松原"), ("220800", "白城"), ("222400", "延边"),
    # 黑龙江
    ("230100", "哈尔滨"), ("230200", "齐齐哈尔"), ("230300", "鸡西"),
    ("230400", "鹤岗"), ("230500", "双鸭山"), ("230600", "大庆"),
    ("230700", "伊春"), ("230800", "佳木斯"), ("230900", "七台河"),
    ("231000", "牡丹江"), ("231100", "黑河"), ("231200", "绥化"),
    ("232700", "大兴安岭"),
    # 上海
    ("310000", "上海"),
    # 江苏
    ("320100", "南京"), ("320200", "无锡"), ("320300", "徐州"),
    ("320400", "常州"), ("320500", "苏州"), ("320600", "南通"),
    ("320700", "连云港"), ("320800", "淮安"), ("320900", "盐城"),
    ("321000", "扬州"), ("321100", "镇江"), ("321200", "泰州"),
    ("321300", "宿迁"),
    # 浙江
    ("330100", "杭州"), ("330200", "宁波"), ("330300", "温州"),
    ("330400", "嘉兴"), ("330500", "湖州"), ("330600", "绍兴"),
    ("330700", "金华"), ("330800", "衢州"), ("330900", "舟山"),
    ("331000", "台州"), ("331100", "丽水"),
    # 安徽
    ("340100", "合肥"), ("340200", "芜湖"), ("340300", "蚌埠"),
    ("340400", "淮南"), ("340500", "马鞍山"), ("340600", "淮北"),
    ("340700", "铜陵"), ("340800", "安庆"), ("341000", "黄山"),
    ("341100", "滁州"), ("341200", "阜阳"), ("341300", "宿州"),
    ("341500", "六安"), ("341600", "亳州"), ("341700", "池州"),
    ("341800", "宣城"),
    # 福建
    ("350100", "福州"), ("350200", "厦门"), ("350300", "莆田"),
    ("350400", "三明"), ("350500", "泉州"), ("350600", "漳州"),
    ("350700", "南平"), ("350800", "龙岩"), ("350900", "宁德"),
    # 江西
    ("360100", "南昌"), ("360200", "景德镇"), ("360300", "萍乡"),
    ("360400", "九江"), ("360500", "新余"), ("360600", "鹰潭"),
    ("360700", "赣州"), ("360800", "吉安"), ("360900", "宜春"),
    ("361000", "抚州"), ("361100", "上饶"),
    # 山东
    ("370100", "济南"), ("370200", "青岛"), ("370300", "淄博"),
    ("370400", "枣庄"), ("370500", "东营"), ("370600", "烟台"),
    ("370700", "潍坊"), ("370800", "济宁"), ("370900", "泰安"),
    ("371000", "威海"), ("371100", "日照"), ("371300", "临沂"),
    ("371400", "德州"), ("371500", "聊城"), ("371600", "滨州"),
    ("371700", "菏泽"),
    # 河南
    ("410100", "郑州"), ("410200", "开封"), ("410300", "洛阳"),
    ("410400", "平顶山"), ("410500", "安阳"), ("410600", "鹤壁"),
    ("410700", "新乡"), ("410800", "焦作"), ("410900", "濮阳"),
    ("411000", "许昌"), ("411100", "漯河"), ("411200", "三门峡"),
    ("411300", "南阳"), ("411400", "商丘"), ("411500", "信阳"),
    ("411600", "周口"), ("411700", "驻马店"),
    # 湖北
    ("420100", "武汉"), ("420200", "黄石"), ("420300", "十堰"),
    ("420500", "宜昌"), ("420600", "襄阳"), ("420700", "鄂州"),
    ("420800", "荆门"), ("420900", "孝感"), ("421000", "荆州"),
    ("421100", "黄冈"), ("421200", "咸宁"), ("421300", "随州"),
    ("422800", "恩施"),
    # 湖南
    ("430100", "长沙"), ("430200", "株洲"), ("430300", "湘潭"),
    ("430400", "衡阳"), ("430500", "邵阳"), ("430600", "岳阳"),
    ("430700", "常德"), ("430800", "张家界"), ("430900", "益阳"),
    ("431000", "郴州"), ("431100", "永州"), ("431200", "怀化"),
    ("431300", "娄底"), ("433100", "湘西"),
    # 广东
    ("440100", "广州"), ("440200", "韶关"), ("440300", "深圳"),
    ("440400", "珠海"), ("440500", "汕头"), ("440600", "佛山"),
    ("440700", "江门"), ("440800", "湛江"), ("440900", "茂名"),
    ("441200", "肇庆"), ("441300", "惠州"), ("441400", "梅州"),
    ("441500", "汕尾"), ("441600", "河源"), ("441700", "阳江"),
    ("441800", "清远"), ("441900", "东莞"), ("442000", "中山"),
    ("445100", "潮州"), ("445200", "揭阳"), ("445300", "云浮"),
    # 广西
    ("450100", "南宁"), ("450200", "柳州"), ("450300", "桂林"),
    ("450400", "梧州"), ("450500", "北海"), ("450600", "防城港"),
    ("450700", "钦州"), ("450800", "贵港"), ("450900", "玉林"),
    ("451000", "百色"), ("451100", "贺州"), ("451200", "河池"),
    ("451300", "来宾"), ("451400", "崇左"),
    # 海南
    ("460100", "海口"), ("460200", "三亚"), ("460400", "儋州"),
    # 重庆
    ("500000", "重庆"),
    # 四川
    ("510100", "成都"), ("510300", "自贡"), ("510400", "攀枝花"),
    ("510500", "泸州"), ("510600", "德阳"), ("510700", "绵阳"),
    ("510800", "广元"), ("510900", "遂宁"), ("511000", "内江"),
    ("511100", "乐山"), ("511300", "南充"), ("511400", "眉山"),
    ("511500", "宜宾"), ("511600", "广安"), ("511700", "达州"),
    ("511800", "雅安"), ("511900", "巴中"), ("512000", "资阳"),
    ("513200", "阿坝"), ("513300", "甘孜"), ("513400", "凉山"),
    # 贵州
    ("520100", "贵阳"), ("520200", "六盘水"), ("520300", "遵义"),
    ("520400", "安顺"), ("520500", "毕节"), ("520600", "铜仁"),
    ("522300", "黔西南"), ("522600", "黔东南"), ("522700", "黔南"),
    # 云南
    ("530100", "昆明"), ("530300", "曲靖"), ("530400", "玉溪"),
    ("530500", "保山"), ("530600", "昭通"), ("530700", "丽江"),
    ("530800", "普洱"), ("530900", "临沧"),
    ("532300", "楚雄"), ("532500", "红河"), ("532600", "文山"),
    ("532800", "西双版纳"), ("532900", "大理"), ("533100", "德宏"),
    ("533300", "怒江"), ("533400", "迪庆"),
    # 西藏
    ("540100", "拉萨"), ("540200", "日喀则"), ("540300", "昌都"),
    ("540400", "林芝"), ("540500", "山南"), ("540600", "那曲"),
    ("542500", "阿里"),
    # 陕西
    ("610100", "西安"), ("610200", "铜川"), ("610300", "宝鸡"),
    ("610400", "咸阳"), ("610500", "渭南"), ("610600", "延安"),
    ("610700", "汉中"), ("610800", "榆林"), ("610900", "安康"),
    ("611000", "商洛"),
    # 甘肃
    ("620100", "兰州"), ("620200", "嘉峪关"), ("620300", "金昌"),
    ("620400", "白银"), ("620500", "天水"), ("620600", "武威"),
    ("620700", "张掖"), ("620800", "平凉"), ("620900", "酒泉"),
    ("621000", "庆阳"), ("621100", "定西"), ("621200", "陇南"),
    ("622900", "临夏"), ("623000", "甘南"),
    # 青海
    ("630100", "西宁"), ("630200", "海东"),
    ("632200", "海北"), ("632300", "黄南"), ("632500", "海南州"),
    ("632600", "果洛"), ("632700", "玉树"), ("632800", "海西"),
    # 宁夏
    ("640100", "银川"), ("640200", "石嘴山"), ("640300", "吴忠"),
    ("640400", "固原"), ("640500", "中卫"),
    # 新疆
    ("650100", "乌鲁木齐"), ("650200", "克拉玛依"), ("650400", "吐鲁番"),
    ("650500", "哈密"), ("652300", "昌吉"), ("652700", "博尔塔拉"),
    ("652800", "巴音郭楞"), ("652900", "阿克苏"), ("653000", "克孜勒苏"),
    ("653100", "喀什"), ("653200", "和田"), ("654000", "伊犁"),
    ("654200", "塔城"), ("654300", "阿勒泰"),
]

# 高德 POI 搜索分类码：风景名胜 + 公园广场
POI_TYPES = "110100|110101|110102|110103|110104|110105|110200|110201|110202|110203|110204|110205|110206|110207|110208|110209|110210|110211|110212|110300|110400"

API_BASE = "https://restapi.amap.com/v3/place/text"


def get_level_from_poi(poi: dict) -> Optional[str]:
    """从 POI 数据中提取 A 级信息，返回 'A5' 或 'A4' 或 None"""
    # 优先检查 biz_ext.level
    biz_ext = poi.get("biz_ext", {})
    if isinstance(biz_ext, dict):
        level_str = biz_ext.get("level", "")
        if isinstance(level_str, str):
            if "AAAAA" in level_str:
                return "A5"
            if "AAAA" in level_str:
                return "A4"

    # 其次检查 keytag
    keytag = poi.get("keytag", "")
    if isinstance(keytag, str):
        if "5A" in keytag:
            return "A5"
        if "4A" in keytag:
            return "A4"

    return None


def fetch_pois(
    api_key: str,
    city_code: str,
    page: int = 1,
    page_size: int = 25,
) -> Optional[dict]:
    """调用高德 POI 搜索接口——搜索所有风景名胜"""
    params = {
        "key": api_key,
        "keywords": "",
        "types": POI_TYPES,
        "city": city_code,
        "citylimit": "true",
        "offset": page_size,
        "page": page,
        "output": "json",
    }
    url = f"{API_BASE}?{urllib.parse.urlencode(params)}"

    req = urllib.request.Request(url)
    req.add_header("User-Agent", "Mozilla/5.0 MapChinaScraper/2.0")

    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"  [ERROR] 请求失败: {e}", file=sys.stderr)
        return None


def fetch_all_pois_for_city(
    api_key: str, city_adcode: str, city_name: str
) -> list[dict]:
    """搜索某城市的所有风景名胜 POI"""
    all_pois = []
    page = 1

    while True:
        data = fetch_pois(api_key, city_adcode, page=page)
        if data is None:
            print(f"  [{city_name}] 第{page}页请求失败，跳过", file=sys.stderr)
            break

        if data.get("status") != "1":
            print(f"  [{city_name}] API错误: {data.get('info')}", file=sys.stderr)
            break

        pois = data.get("pois", [])
        if not pois:
            break

        all_pois.extend(pois)

        count = int(data.get("count", "0"))
        # 高德最多返回约1000条（40页*25条）
        if page * 25 >= count or page >= 40:
            break

        page += 1
        time.sleep(0.12)

    return all_pois


def poi_to_attraction(poi: dict, level_key: str) -> Optional[dict]:
    """将高德 POI 数据转换为景点记录"""
    name = poi.get("name", "").strip()
    if not name:
        return None

    location = poi.get("location", "")
    if not location or "," not in location:
        return None

    parts = location.split(",")
    longitude = float(parts[0])
    latitude = float(parts[1])

    pname = poi.get("pname", "")
    cityname = poi.get("cityname", "")
    adname = poi.get("adname", "")
    adcode = poi.get("adcode", "")

    region_id = adcode if adcode else ""

    # 用高德 POI id 生成唯一 ID
    poi_id = poi.get("id", "")
    # 清理 id 中的特殊字符
    safe_id = re.sub(r"[^a-zA-Z0-9]", "", poi_id)
    attraction_id = f"attr_{safe_id}"

    address = poi.get("address", "")
    description = f"{pname}{cityname}{adname}"
    if address:
        description += f" {address}"

    return {
        "id": attraction_id,
        "name": name,
        "region_id": region_id,
        "level": level_key,
        "latitude": round(latitude, 6),
        "longitude": round(longitude, 6),
        "province": pname,
        "city": cityname,
        "district": adname,
        "description": description.strip(),
    }


def deduplicate_attractions(attractions: list[dict]) -> list[dict]:
    """按名称+坐标去重"""
    seen = set()
    result = []
    for a in attractions:
        key = (a["name"], round(a["latitude"], 2), round(a["longitude"], 2))
        if key not in seen:
            seen.add(key)
            result.append(a)
    return result


def generate_kotlin_seed(attractions: list[dict]) -> str:
    """生成可直接粘贴到 DataSeeder.kt 的 Kotlin 代码"""
    lines = ["val attractions = listOf("]

    for a in attractions:
        desc = a["description"].replace('"', '\\"') if a["description"] else ""
        level = a["level"]
        lines.append(
            f'    Attraction("{a["id"]}", "{a["name"]}", "{a["region_id"]}", '
            f'AttractionLevel.{level}, {a["latitude"]}, {a["longitude"]}, "{desc}"),'
        )

    lines.append(")")
    lines.append("")
    lines.append("attractions.forEach { attractionRepo.insertAttraction(it) }")
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="爬取中国4A/5A景点数据")
    parser.add_argument("--key", required=True, help="高德地图 Web 服务 API Key")
    parser.add_argument("--output-dir", default="./output", help="输出目录")
    parser.add_argument("--resume-from", default="", help="从指定城市 adcode 继续")
    args = parser.parse_args()

    os.makedirs(args.output_dir, exist_ok=True)

    all_attractions = []
    total_a5 = 0
    total_a4 = 0
    skip = bool(args.resume_from)

    for city_adcode, city_name in CITIES:
        if skip:
            if city_adcode == args.resume_from:
                skip = False
            else:
                continue

        print(f"[{city_name}] 搜索中...", end="", flush=True)
        pois = fetch_all_pois_for_city(args.key, city_adcode, city_name)

        a5 = 0
        a4 = 0
        for poi in pois:
            level = get_level_from_poi(poi)
            if level is None:
                continue
            attraction = poi_to_attraction(poi, level)
            if attraction:
                all_attractions.append(attraction)
                if level == "A5":
                    a5 += 1
                else:
                    a4 += 1

        total_a5 += a5
        total_a4 += a4
        print(f" 5A={a5} 4A={a4} (累计 5A={total_a5} 4A={total_a4})")

        time.sleep(0.2)

    # 去重
    before = len(all_attractions)
    all_attractions = deduplicate_attractions(all_attractions)
    after = len(all_attractions)
    removed = before - after

    # 按省份+级别排序
    all_attractions.sort(key=lambda a: (a["province"], a["level"], a["name"]))

    # 统计
    a5_count = sum(1 for a in all_attractions if a["level"] == "A5")
    a4_count = sum(1 for a in all_attractions if a["level"] == "A4")
    print(f"\n===== 完成 =====")
    print(f"5A 景区: {a5_count} 个")
    print(f"4A 景区: {a4_count} 个")
    print(f"总计: {len(all_attractions)} 个 (去重移除 {removed} 条)")

    # 输出 JSON
    json_path = os.path.join(args.output_dir, "attractions.json")
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(all_attractions, f, ensure_ascii=False, indent=2)
    print(f"\n已保存 JSON: {json_path}")

    # 输出 CSV
    csv_path = os.path.join(args.output_dir, "attractions.csv")
    with open(csv_path, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(
            f,
            fieldnames=[
                "id", "name", "level", "latitude", "longitude",
                "province", "city", "district", "region_id", "description",
            ],
        )
        writer.writeheader()
        writer.writerows(all_attractions)
    print(f"已保存 CSV: {csv_path}")

    # 输出 Kotlin seed 代码
    kt_path = os.path.join(args.output_dir, "seed_data.kt")
    with open(kt_path, "w", encoding="utf-8") as f:
        f.write(generate_kotlin_seed(all_attractions))
    print(f"已保存 Kotlin: {kt_path}")


if __name__ == "__main__":
    main()
