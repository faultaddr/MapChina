package com.mapchina.data.remote

import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel

object DataSeeder {

    fun seedRegions(regionRepo: RegionRepository) {
        if (regionRepo.getRegionsByLevel(RegionLevel.PROVINCE).isNotEmpty()) return

        val provinces = listOf(
            Region("110000", "北京市", RegionLevel.PROVINCE, null),
            Region("120000", "天津市", RegionLevel.PROVINCE, null),
            Region("130000", "河北省", RegionLevel.PROVINCE, null),
            Region("140000", "山西省", RegionLevel.PROVINCE, null),
            Region("150000", "内蒙古自治区", RegionLevel.PROVINCE, null),
            Region("210000", "辽宁省", RegionLevel.PROVINCE, null),
            Region("220000", "吉林省", RegionLevel.PROVINCE, null),
            Region("230000", "黑龙江省", RegionLevel.PROVINCE, null),
            Region("310000", "上海市", RegionLevel.PROVINCE, null),
            Region("320000", "江苏省", RegionLevel.PROVINCE, null),
            Region("330000", "浙江省", RegionLevel.PROVINCE, null),
            Region("340000", "安徽省", RegionLevel.PROVINCE, null),
            Region("350000", "福建省", RegionLevel.PROVINCE, null),
            Region("360000", "江西省", RegionLevel.PROVINCE, null),
            Region("370000", "山东省", RegionLevel.PROVINCE, null),
            Region("410000", "河南省", RegionLevel.PROVINCE, null),
            Region("420000", "湖北省", RegionLevel.PROVINCE, null),
            Region("430000", "湖南省", RegionLevel.PROVINCE, null),
            Region("440000", "广东省", RegionLevel.PROVINCE, null),
            Region("450000", "广西壮族自治区", RegionLevel.PROVINCE, null),
            Region("460000", "海南省", RegionLevel.PROVINCE, null),
            Region("500000", "重庆市", RegionLevel.PROVINCE, null),
            Region("510000", "四川省", RegionLevel.PROVINCE, null),
            Region("520000", "贵州省", RegionLevel.PROVINCE, null),
            Region("530000", "云南省", RegionLevel.PROVINCE, null),
            Region("540000", "西藏自治区", RegionLevel.PROVINCE, null),
            Region("610000", "陕西省", RegionLevel.PROVINCE, null),
            Region("620000", "甘肃省", RegionLevel.PROVINCE, null),
            Region("630000", "青海省", RegionLevel.PROVINCE, null),
            Region("640000", "宁夏回族自治区", RegionLevel.PROVINCE, null),
            Region("650000", "新疆维吾尔自治区", RegionLevel.PROVINCE, null),
            Region("710000", "台湾省", RegionLevel.PROVINCE, null),
            Region("810000", "香港特别行政区", RegionLevel.PROVINCE, null),
            Region("820000", "澳门特别行政区", RegionLevel.PROVINCE, null)
        )

        provinces.forEach { regionRepo.insertRegion(it) }

        // Seed key cities under some provinces
        val cities = listOf(
            Region("110100", "北京市", RegionLevel.CITY, "110000"),
            Region("310100", "上海市", RegionLevel.CITY, "310000"),
            Region("510100", "成都市", RegionLevel.CITY, "510000"),
            Region("510300", "自贡市", RegionLevel.CITY, "510000"),
            Region("510700", "绵阳市", RegionLevel.CITY, "510000"),
            Region("330100", "杭州市", RegionLevel.CITY, "330000"),
            Region("330200", "宁波市", RegionLevel.CITY, "330000"),
            Region("440100", "广州市", RegionLevel.CITY, "440000"),
            Region("440300", "深圳市", RegionLevel.CITY, "440000"),
            Region("320100", "南京市", RegionLevel.CITY, "320000"),
            Region("320500", "苏州市", RegionLevel.CITY, "320000"),
            Region("420100", "武汉市", RegionLevel.CITY, "420000"),
            Region("430100", "长沙市", RegionLevel.CITY, "430000"),
            Region("500100", "重庆市", RegionLevel.CITY, "500000"),
            Region("110101", "东城区", RegionLevel.DISTRICT, "110100"),
            Region("110102", "西城区", RegionLevel.DISTRICT, "110100"),
            Region("510107", "武侯区", RegionLevel.DISTRICT, "510100")
        )

        cities.forEach { regionRepo.insertRegion(it) }
    }

    fun seedAttractions(attractionRepo: AttractionRepository) {
        val attractions = listOf(
            Attraction("attr001", "故宫博物院", "110101", AttractionLevel.A5, 39.9163, 116.3972, "中国明清两代的皇家宫殿"),
            Attraction("attr002", "天坛", "110101", AttractionLevel.A5, 39.8822, 116.4066, "明清两朝帝王祭天之地"),
            Attraction("attr003", "西湖", "330102", AttractionLevel.A5, 30.2590, 120.1388, "中国最著名的湖泊之一"),
            Attraction("attr004", "武侯祠", "510107", AttractionLevel.A4, 30.6460, 104.0478, "纪念诸葛亮的祠堂"),
            Attraction("attr005", "锦里", "510107", AttractionLevel.A4, 30.6454, 104.0489, "成都历史文化街区"),
            Attraction("attr006", "长城(八达岭)", "110100", AttractionLevel.A5, 40.3588, 116.0204, "世界文化遗产"),
            Attraction("attr007", "外滩", "310100", AttractionLevel.A4, 31.2400, 121.4900, "上海标志性景点"),
            Attraction("attr008", "兵马俑", "610100", AttractionLevel.A5, 34.3842, 109.2785, "秦始皇陵兵马俑"),
            Attraction("attr009", "张家界", "430800", AttractionLevel.A5, 29.3249, 110.4343, "世界自然遗产"),
            Attraction("attr010", "黄山", "341000", AttractionLevel.A5, 30.1375, 118.1694, "中国十大名山之一")
        )

        attractions.forEach { attractionRepo.insertAttraction(it) }
    }
}
