package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.Journal
import com.mapchina.domain.model.JournalPhoto
import com.mapchina.domain.model.JournalTrackPoint

class JournalRepository(private val database: MapChinaDatabase) {

    fun getJournal(id: String): Journal? {
        val row = database.journalQueries.selectById(id).executeAsOneOrNull() ?: return null
        return Journal(row.id, row.user_id, row.title, row.description, row.region_id, row.attraction_id, row.start_time, row.end_time, row.created_at, row.updated_at)
    }

    fun getJournalsByUser(userId: String): List<Journal> {
        return database.journalQueries.selectByUserId(userId).executeAsList().map {
            Journal(it.id, it.user_id, it.title, it.description, it.region_id, it.attraction_id, it.start_time, it.end_time, it.created_at, it.updated_at)
        }
    }

    fun insertJournal(journal: Journal) {
        database.journalQueries.insertJournal(
            journal.id, journal.userId, journal.title, journal.description,
            journal.regionId, journal.attractionId, journal.startTime, journal.endTime,
            journal.createdAt, journal.updatedAt
        )
    }

    fun updateJournal(title: String, description: String, regionId: String?, attractionId: String?, endTime: Long?, updatedAt: Long, id: String) {
        database.journalQueries.updateJournal(title, description, regionId, attractionId, endTime, updatedAt, id)
    }

    fun deleteJournal(id: String) {
        database.journalPhotoQueries.deleteByJournalId(id)
        database.journalTrackPointQueries.deleteByJournalId(id)
        database.journalQueries.deleteById(id)
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
    }

    fun insertPhotosInTransaction(photos: List<JournalPhoto>) {
        database.journalPhotoQueries.transaction {
            for (photo in photos) {
                database.journalPhotoQueries.insertPhoto(
                    photo.id, photo.journalId, photo.localPath,
                    photo.latitude, photo.longitude, photo.takenAt, photo.sortOrder
                )
            }
        }
    }

    fun deletePhoto(id: String) {
        database.journalPhotoQueries.deleteById(id)
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
            }
        }
    }
}
