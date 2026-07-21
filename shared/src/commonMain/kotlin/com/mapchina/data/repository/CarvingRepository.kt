package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.Carving
import com.mapchina.sync.SyncChangeWriter
import com.mapchina.sync.SyncEntityType
import kotlin.time.Clock

class CarvingRepository(
    private val database: MapChinaDatabase,
    private val syncChangeWriter: SyncChangeWriter? = null
) {

    fun getCarving(id: String): Carving? {
        val row = database.carvingQueries.selectById(id).executeAsOneOrNull() ?: return null
        return rowToCarving(row)
    }

    fun getCarvingsByRegion(regionId: String): List<Carving> {
        return database.carvingQueries.selectByRegionId(regionId).executeAsList().map { rowToCarving(it) }
    }

    fun getCarvingsByAttraction(attractionId: String): List<Carving> {
        return database.carvingQueries.selectByAttractionId(attractionId).executeAsList().map { rowToCarving(it) }
    }

    fun getCarvingsByUser(userId: String): List<Carving> {
        return database.carvingQueries.selectByUserId(userId).executeAsList().map { rowToCarving(it) }
    }

    fun getAllCarvings(): List<Carving> {
        return database.carvingQueries.selectAll().executeAsList().map { rowToCarving(it) }
    }

    fun insertCarving(carving: Carving) {
        database.carvingQueries.insertCarving(
            carving.id, carving.userId, carving.regionId, carving.regionName,
            carving.imagePath, carving.strokeData, carving.createdAt,
            carving.attractionId, carving.attractionName, carving.previewAspectRatio?.toStableDouble()
        )
        syncChangeWriter?.enqueueCarving(carving)
    }

    fun updateCarving(carving: Carving) {
        database.carvingQueries.updateCarving(
            carving.imagePath, carving.strokeData, carving.previewAspectRatio?.toStableDouble(), carving.id
        )
        syncChangeWriter?.enqueueCarving(carving, Clock.System.now().toEpochMilliseconds())
    }

    fun deleteCarving(id: String) {
        database.carvingQueries.deleteById(id)
        syncChangeWriter?.enqueueDelete(SyncEntityType.CARVING, id, Clock.System.now().toEpochMilliseconds())
    }

    private fun rowToCarving(row: com.mapchina.data.local.Carving): Carving =
        Carving(
            id = row.id,
            userId = row.user_id,
            regionId = row.region_id,
            regionName = row.region_name,
            imagePath = row.image_path,
            strokeData = row.stroke_data,
            createdAt = row.created_at,
            attractionId = row.attraction_id,
            attractionName = row.attraction_name,
            previewAspectRatio = row.preview_aspect_ratio?.toFloat()
        )
}

private fun Float.toStableDouble(): Double = toString().toDouble()
