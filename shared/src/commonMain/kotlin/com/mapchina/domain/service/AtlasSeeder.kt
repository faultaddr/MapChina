package com.mapchina.domain.service

import com.mapchina.data.repository.AtlasRepository
import com.mapchina.domain.model.AtlasItem

object AtlasSeeder {

    fun seedAtlas(atlasRepo: AtlasRepository) {
        if (atlasRepo.isSeeded()) return

        atlasRepo.insertDefinition("world_heritage", "世界遗产", "联合国教科文组织认定的世界文化与自然遗产", "atlas_heritage")
        atlasRepo.insertDefinition("museum", "博物馆", "国家级及省级重点博物馆", "atlas_museum")
        atlasRepo.insertDefinition("mountain", "名山", "中国名山大川与山岳型景区", "atlas_mountain")

        val items = buildList {
            // 世界遗产
            add(AtlasItem("world_heritage", "attr_B000A8UIN8", "故宫博物院", "北京", "东城区"))
            add(AtlasItem("world_heritage", "attr_B000A81CB2", "天坛公园", "北京", "东城区"))
            add(AtlasItem("world_heritage", "attr_B000A45467", "八达岭长城", "北京", "延庆区"))
            add(AtlasItem("world_heritage", "attr_B02E80N9BJ", "武陵源风景名胜区", "湖南", "张家界"))
            add(AtlasItem("world_heritage", "attr_B022F0ML6Z", "黄山风景区", "安徽", "黄山"))

            // 博物馆
            add(AtlasItem("museum", "attr_B000A8UIN8", "故宫博物院", "北京", "东城区"))

            // 名山
            add(AtlasItem("mountain", "attr_B022F0ML6Z", "黄山风景区", "安徽", "黄山"))
            add(AtlasItem("mountain", "attr_B02E80N9BJ", "武陵源风景名胜区", "湖南", "张家界"))
        }

        atlasRepo.insertItemsInTransaction(items)
    }
}
