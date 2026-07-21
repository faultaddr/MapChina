package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.Journal
import com.mapchina.domain.model.JournalPhoto
import com.mapchina.domain.model.JournalTrackPoint
import com.mapchina.sync.SyncChangeWriter
import com.mapchina.sync.SyncEntityType
import kotlin.time.Clock

class JournalRepository(
    private val database: MapChinaDatabase,
    private val syncChangeWriter: SyncChangeWriter? = null
) {

    fun getJournal(id: String): Journal? {
        val row = database.journalQueries.selectById(id).executeAsOneOrNull() ?: return null
        return rowToJournal(row)
    }

    fun getJournalsByUser(userId: String): List<Journal> {
        return database.journalQueries.selectByUserId(userId).executeAsList().map {
            rowToJournal(it)
        }
    }

    fun getJournalsByAttraction(attractionId: String): List<Journal> {
        return database.journalQueries.selectByAttractionId(attractionId).executeAsList().map {
            rowToJournal(it)
        }
    }

    fun insertJournal(journal: Journal) {
        database.journalQueries.insertJournal(
            journal.id, journal.userId, journal.title, journal.description,
            journal.regionId, journal.attractionId, journal.startTime, journal.endTime,
            journal.createdAt, journal.updatedAt
        )
        syncChangeWriter?.enqueueJournal(journal)
    }

    fun updateJournal(title: String, description: String, regionId: String?, attractionId: String?, endTime: Long?, updatedAt: Long, id: String) {
        database.journalQueries.updateJournal(title, description, regionId, attractionId, endTime, updatedAt, id)
        database.journalQueries.selectById(id).executeAsOneOrNull()
            ?.let { syncChangeWriter?.enqueueJournal(rowToJournal(it)) }
    }

    fun deleteJournal(id: String) {
        val journal = getJournal(id)
        database.journalPhotoQueries.deleteByJournalId(id)
        database.journalTrackPointQueries.deleteByJournalId(id)
        database.journalQueries.deleteById(id)
        syncChangeWriter?.enqueueJournalDelete(journal, id)
    }

    fun getPhotosByJournal(journalId: String): List<JournalPhoto> {
        return database.journalPhotoQueries.selectByJournalId(journalId).executeAsList().map {
            JournalPhoto(it.id, it.journal_id, it.local_path, it.latitude, it.longitude, it.taken_at, it.sort_order)
        }
    }

    fun insertPhoto(photo: JournalPhoto) {
        database.journalPhotoQueries.insertPhoto(
            photo.id, photo.journalId, photo.localPath,
            photo.latitude, photo.longitude, photo.takenAt, photo.sortOrder
        )
        syncChangeWriter?.enqueueJournalPhoto(photo)
    }

    fun insertPhotosInTransaction(photos: List<JournalPhoto>) {
        database.journalPhotoQueries.transaction {
            for (photo in photos) {
                database.journalPhotoQueries.insertPhoto(
                    photo.id, photo.journalId, photo.localPath,
                    photo.latitude, photo.longitude, photo.takenAt, photo.sortOrder
                )
                syncChangeWriter?.enqueueJournalPhoto(photo)
            }
        }
    }

    fun deletePhoto(id: String) {
        database.journalPhotoQueries.deleteById(id)
        syncChangeWriter?.enqueueDelete(SyncEntityType.JOURNAL_PHOTO, id, Clock.System.now().toEpochMilliseconds())
    }

    fun getAllPhotosWithLocation(): List<JournalPhoto> {
        return database.journalPhotoQueries.selectAllWithLocation().executeAsList().map {
            JournalPhoto(it.id, it.journal_id, it.local_path, it.latitude, it.longitude, it.taken_at, it.sort_order)
        }
    }

    fun getTrackPointsByJournal(journalId: String): List<JournalTrackPoint> {
        return database.journalTrackPointQueries.selectByJournalId(journalId).executeAsList().map {
            JournalTrackPoint(it.id, it.journal_id, it.latitude, it.longitude, it.altitude, it.speed, it.timestamp, it.sort_order)
        }
    }

    fun insertTrackPointsInTransaction(points: List<JournalTrackPoint>) {
        database.journalTrackPointQueries.transaction {
            for (point in points) {
                database.journalTrackPointQueries.insertTrackPoint(
                    point.id, point.journalId, point.latitude, point.longitude,
                    point.altitude, point.speed, point.timestamp, point.sortOrder
                )
                syncChangeWriter?.enqueueJournalTrackPoint(point)
            }
        }
    }

    private fun rowToJournal(row: com.mapchina.data.local.Journal): Journal =
        Journal(
            row.id,
            row.user_id,
            row.title,
            row.description,
            row.region_id,
            row.attraction_id,
            row.start_time,
            row.end_time,
            row.created_at,
            row.updated_at
        )
}
