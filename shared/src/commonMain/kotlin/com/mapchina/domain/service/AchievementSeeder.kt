package com.mapchina.domain.service

import com.mapchina.data.repository.AchievementRepository

object AchievementSeeder {

    private data class SeedDef(
        val id: String, val category: String, val subCategory: String,
        val name: String, val description: String, val icon: String,
        val rarity: String, val triggerType: String, val triggerCondition: String,
        val rewardScore: Long, val sortOrder: Long
    )

    private val seeds = listOf(
        // 县级
        SeedDef("region_district_1", "REGION", "district", "初探山河", "点亮1个县", "badge_district", "COMMON", "COUNT", "district:1", 50, 1),
        SeedDef("region_district_10", "REGION", "district", "县域漫游者", "点亮10个县", "badge_district", "COMMON", "COUNT", "district:10", 100, 2),
        SeedDef("region_district_30", "REGION", "district", "四方踏访", "点亮30个县", "badge_district", "RARE", "COUNT", "district:30", 200, 3),
        SeedDef("region_district_100", "REGION", "district", "百县行者", "点亮100个县", "badge_district", "EPIC", "COUNT", "district:100", 500, 4),
        // 市级
        SeedDef("region_city_1", "REGION", "city", "城市初见", "点亮1个市", "badge_city", "COMMON", "COUNT", "city:1", 50, 5),
        SeedDef("region_city_10", "REGION", "city", "十城旅人", "点亮10个市", "badge_city", "COMMON", "COUNT", "city:10", 100, 6),
        SeedDef("region_city_30", "REGION", "city", "山河识途者", "点亮30个市", "badge_city", "RARE", "COUNT", "city:30", 200, 7),
        SeedDef("region_city_100", "REGION", "city", "百城达人", "点亮100个市", "badge_city", "EPIC", "COUNT", "city:100", 500, 8),
        // 省级
        SeedDef("region_province_1", "REGION", "province", "跨省出发", "点亮1个省", "badge_province", "COMMON", "COUNT", "province:1", 50, 9),
        SeedDef("region_province_5", "REGION", "province", "五省行记", "点亮5个省", "badge_province", "COMMON", "COUNT", "province:5", 100, 10),
        SeedDef("region_province_10", "REGION", "province", "九州漫游者", "点亮10个省", "badge_province", "RARE", "COUNT", "province:10", 200, 11),
        SeedDef("region_province_20", "REGION", "province", "大地巡游家", "点亮20个省", "badge_province", "EPIC", "COUNT", "province:20", 500, 12),
        SeedDef("region_province_31", "REGION", "province", "丈量中国", "点亮31个省级区域", "badge_province", "LEGENDARY", "COUNT", "province:31", 1000, 13),
        // 5A 景点
        SeedDef("scenic_5a_1", "SCENIC", "5a", "初见华景", "点亮1个5A景点", "badge_5a", "COMMON", "COUNT", "5a:1", 50, 14),
        SeedDef("scenic_5a_10", "SCENIC", "5a", "胜景识途者", "点亮10个5A景点", "badge_5a", "COMMON", "COUNT", "5a:10", 100, 15),
        SeedDef("scenic_5a_30", "SCENIC", "5a", "名胜巡礼者", "点亮30个5A景点", "badge_5a", "RARE", "COUNT", "5a:30", 200, 16),
        SeedDef("scenic_5a_50", "SCENIC", "5a", "山河收藏家", "点亮50个5A景点", "badge_5a", "EPIC", "COUNT", "5a:50", 500, 17),
        SeedDef("scenic_5a_100", "SCENIC", "5a", "国家胜景大师", "点亮100个5A景点", "badge_5a", "LEGENDARY", "COUNT", "5a:100", 1000, 18),
        // 4A/5A 总数
        SeedDef("scenic_total_10", "SCENIC", "total", "风景在路上", "点亮10个景点", "badge_total", "COMMON", "COUNT", "total:10", 50, 19),
        SeedDef("scenic_total_50", "SCENIC", "total", "景区猎人", "点亮50个景点", "badge_total", "RARE", "COUNT", "total:50", 200, 20),
        SeedDef("scenic_total_100", "SCENIC", "total", "名景博览者", "点亮100个景点", "badge_total", "EPIC", "COUNT", "total:100", 500, 21),
        SeedDef("scenic_total_300", "SCENIC", "total", "中华胜景图鉴家", "点亮300个景点", "badge_total", "LEGENDARY", "COUNT", "total:300", 1000, 22),
    )

    private val atlasSeeds = listOf(
        // 世界遗产
        SeedDef("atlas_heritage_1", "ATLAS", "world_heritage", "初识世界遗产", "点亮1处世界遗产", "badge_atlas_heritage", "COMMON", "COUNT", "atlas:world_heritage:1", 50, 200),
        SeedDef("atlas_heritage_5", "ATLAS", "world_heritage", "遗产访客", "点亮5处世界遗产", "badge_atlas_heritage", "COMMON", "COUNT", "atlas:world_heritage:5", 100, 201),
        SeedDef("atlas_heritage_10", "ATLAS", "world_heritage", "文明巡礼者", "点亮10处世界遗产", "badge_atlas_heritage", "RARE", "COUNT", "atlas:world_heritage:10", 200, 202),
        SeedDef("atlas_heritage_20", "ATLAS", "world_heritage", "世界遗产收藏家", "点亮20处世界遗产", "badge_atlas_heritage", "EPIC", "COUNT", "atlas:world_heritage:20", 500, 203),
        // 博物馆
        SeedDef("atlas_museum_5", "ATLAS", "museum", "展柜漫游者", "点亮5座博物馆", "badge_atlas_museum", "COMMON", "COUNT", "atlas:museum:5", 50, 210),
        SeedDef("atlas_museum_20", "ATLAS", "museum", "博物馆旅人", "点亮20座博物馆", "badge_atlas_museum", "RARE", "COUNT", "atlas:museum:20", 200, 211),
        SeedDef("atlas_museum_50", "ATLAS", "museum", "文明观察者", "点亮50座博物馆", "badge_atlas_museum", "EPIC", "COUNT", "atlas:museum:50", 500, 212),
        // 名山
        SeedDef("atlas_mountain_5", "ATLAS", "mountain", "登山看中国", "点亮5座名山", "badge_atlas_mountain", "COMMON", "COUNT", "atlas:mountain:5", 50, 220),
        SeedDef("atlas_mountain_10", "ATLAS", "mountain", "群峰行者", "点亮10座名山", "badge_atlas_mountain", "RARE", "COUNT", "atlas:mountain:10", 200, 221),
        SeedDef("atlas_mountain_20", "ATLAS", "mountain", "山岳巡礼家", "点亮20座名山", "badge_atlas_mountain", "EPIC", "COUNT", "atlas:mountain:20", 500, 222),
    )

    private val provinceSeeds = buildList {
        var order = 100L
        val provinceNames = mapOf(
            "11" to "京华初见", "12" to "津门初访", "13" to "冀地初识", "14" to "晋地初识",
            "15" to "草原初临", "21" to "辽沈初访", "22" to "白山初识", "23" to "黑土初临",
            "31" to "浦江初见", "32" to "吴地初识", "33" to "越地初访", "34" to "徽州初识",
            "35" to "闽地初访", "36" to "赣地初识", "37" to "齐鲁初访", "41" to "中原初识",
            "42" to "楚地初访", "43" to "湘地初识", "44" to "岭南初临", "45" to "八桂初访",
            "46" to "海岛初临", "50" to "山城初访", "51" to "蜀地初识", "52" to "黔地初访",
            "53" to "彩云初见", "54" to "雪域初访", "61" to "三秦初识", "62" to "陇原初访",
            "63" to "高原初临", "64" to "塞上初访", "65" to "西域初访"
        )
        val provinceCompleteNames = mapOf(
            "11" to "京华通行者", "12" to "津门通行者", "13" to "冀地通行者", "14" to "晋地通行者",
            "15" to "草原通行者", "21" to "辽沈通行者", "22" to "白山通行者", "23" to "黑土通行者",
            "31" to "浦江通行者", "32" to "吴地通行者", "33" to "越地通行者", "34" to "徽州通行者",
            "35" to "闽地通行者", "36" to "赣地通行者", "37" to "齐鲁通行者", "41" to "中原通行者",
            "42" to "楚地通行者", "43" to "湘地通行者", "44" to "岭南通行者", "45" to "八桂通行者",
            "46" to "海岛通行者", "50" to "山城通行者", "51" to "蜀地通行者", "52" to "黔地通行者",
            "53" to "彩云通行者", "54" to "雪域通行者", "61" to "三秦通行者", "62" to "陇原通行者",
            "63" to "高原通行者", "64" to "塞上通行者", "65" to "西域通行者"
        )
        for ((code, visitName) in provinceNames) {
            add(SeedDef("province_visit_$code", "PROVINCE", "visit", visitName, "到访${code}开头省份", "badge_province_visit", "COMMON", "COUNT", "province_visit:$code:1", 50, order++))
        }
        for ((code, completeName) in provinceCompleteNames) {
            add(SeedDef("province_complete_$code", "PROVINCE", "complete", completeName, "完成${code}开头省份全部城市", "badge_province_complete", "RARE", "FULL_COMPLETE", "province_complete:$code", 200, order++))
        }
    }

    private val geoSeeds = listOf(
        // Geographic exploration achievements
        SeedDef("geo_north", "GEOGRAPHY", "north", "极北之地", "到访最北省份黑龙江", "badge_geo", "RARE", "GEO", "geo:north:23", 200, 300),
        SeedDef("geo_south", "GEOGRAPHY", "south", "天涯海角", "到访最南省份海南", "badge_geo", "RARE", "GEO", "geo:south:46", 200, 301),
        SeedDef("geo_silk_road", "GEOGRAPHY", "silk_road", "丝路起点", "到访甘肃和陕西", "badge_geo", "EPIC", "GEO", "geo:silk_road:2", 500, 302),
        SeedDef("geo_coast", "GEOGRAPHY", "coast", "海岸线", "到访3个沿海省份", "badge_geo", "RARE", "GEO", "geo:coast:3", 200, 303),
        SeedDef("geo_river", "GEOGRAPHY", "river", "跨越大江", "到访长江南北各至少1个省", "badge_geo", "EPIC", "GEO", "geo:cross_river:2", 500, 304),
        SeedDef("geo_same_day_3", "GEOGRAPHY", "same_day", "一日三省", "同一天标记3个不同省的足迹", "badge_geo", "LEGENDARY", "GEO", "geo:same_day_3:3", 1000, 305),
    )

    fun seedAchievements(repo: AchievementRepository) {
        if (repo.getAllDefinitions().isNotEmpty()) return
        for (s in seeds + atlasSeeds + provinceSeeds + geoSeeds) {
            repo.insertDefinition(
                s.id, s.category, s.subCategory, s.name, s.description, s.icon,
                s.rarity, s.triggerType, s.triggerCondition, s.rewardScore, s.sortOrder
            )
        }
    }
}
