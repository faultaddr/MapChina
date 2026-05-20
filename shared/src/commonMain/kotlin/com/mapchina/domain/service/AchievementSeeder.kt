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

    fun seedAchievements(repo: AchievementRepository) {
        if (repo.getAllDefinitions().isNotEmpty()) return
        for (s in seeds) {
            repo.insertDefinition(
                s.id, s.category, s.subCategory, s.name, s.description, s.icon,
                s.rarity, s.triggerType, s.triggerCondition, s.rewardScore, s.sortOrder
            )
        }
    }
}
