package com.mapchina.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun regionDto_serializesAndDeserializes() {
        val dto = RegionDto(
            id = "110000",
            name = "北京市",
            level = RegionLevel.PROVINCE,
            parentId = null
        )
        val encoded = Json.encodeToString(RegionDto.serializer(), dto)
        val decoded = Json.decodeFromString(RegionDto.serializer(), encoded)
        assertEquals(dto, decoded)
    }

    @Test
    fun regionDto_withParentId_serializesCorrectly() {
        val dto = RegionDto(id = "510100", name = "成都市", level = RegionLevel.CITY, parentId = "510000")
        val encoded = Json.encodeToString(RegionDto.serializer(), dto)
        val decoded = Json.decodeFromString(RegionDto.serializer(), encoded)
        assertEquals("510000", decoded.parentId)
    }

    @Test
    fun footprintLevel_ordering() {
        assertTrue(FootprintLevel.DEEP > FootprintLevel.SHORT_VISIT)
        assertTrue(FootprintLevel.SHORT_VISIT > FootprintLevel.PASS_BY)
        assertTrue(FootprintLevel.DEEP > FootprintLevel.PASS_BY)
    }

    @Test
    fun footprintDto_serializesAndDeserializes() {
        val dto = FootprintDto(userId = "u1", regionId = "510000", level = FootprintLevel.DEEP, timestamp = 1000L)
        val encoded = Json.encodeToString(FootprintDto.serializer(), dto)
        val decoded = Json.decodeFromString(FootprintDto.serializer(), encoded)
        assertEquals(dto, decoded)
    }

    @Test
    fun attractionDto_serializesAndDeserializes() {
        val dto = AttractionDto(
            id = "attr1", name = "九寨沟", regionId = "513200",
            level = AttractionLevel.A5, latitude = 33.26, longitude = 103.92,
            description = "世界自然遗产"
        )
        val encoded = Json.encodeToString(AttractionDto.serializer(), dto)
        val decoded = Json.decodeFromString(AttractionDto.serializer(), encoded)
        assertEquals(dto, decoded)
    }

    @Test
    fun attractionVisitDto_serializesAndDeserializes() {
        val dto = AttractionVisitDto(userId = "u1", attractionId = "attr1", level = FootprintLevel.DEEP, timestamp = 2000L, note = "很美")
        val encoded = Json.encodeToString(AttractionVisitDto.serializer(), dto)
        val decoded = Json.decodeFromString(AttractionVisitDto.serializer(), encoded)
        assertEquals(dto, decoded)
    }

    @Test
    fun userDto_serializesAndDeserializes() {
        val dto = UserDto(id = "u1", phone = "13800138000", nickname = "旅行者", avatar = null, createdAt = 3000L)
        val encoded = Json.encodeToString(UserDto.serializer(), dto)
        val decoded = Json.decodeFromString(UserDto.serializer(), encoded)
        assertEquals(dto, decoded)
    }

    @Test
    fun apiResponse_success_wrapsData() {
        val response = ApiResponse.Success(data = "test", total = 1)
        assertTrue(response.isSuccess())
        assertEquals("test", response.data)
        assertEquals(1, response.total)
    }

    @Test
    fun apiResponse_error_indicatesFailure() {
        val response = ApiResponse.Error<Unit>(code = "NOT_FOUND", message = "区域不存在")
        assertFalse(response.isSuccess())
        assertEquals("NOT_FOUND", response.code)
    }
}
