package com.mapchina.domain.service

import com.mapchina.data.repository.JournalRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.domain.model.Journal
import com.mapchina.domain.model.JournalDetail
import com.mapchina.domain.model.JournalPhoto
import com.mapchina.domain.model.JournalTrackPoint
import kotlinx.datetime.Clock

class JournalService(
    private val journalRepository: JournalRepository,
    private val regionRepository: RegionRepository,
    private val attractionRepository: AttractionRepository
) {

    fun createJournal(userId: String, title: String, description: String, regionId: String?, attractionId: String?, startTime: Long): Journal {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = "journal_${now}_${title.hashCode().toUInt()}"
        val journal = Journal(
            id = id, userId = userId, title = title, description = description,
            regionId = regionId, attractionId = attractionId, startTime = startTime, endTime = null,
            createdAt = now, updatedAt = now
        )
        journalRepository.insertJournal(journal)
        return journal
    }

    fun updateJournal(journalId: String, title: String, description: String, regionId: String?, attractionId: String?, endTime: Long?) {
        val now = Clock.System.now().toEpochMilliseconds()
        journalRepository.updateJournal(title, description, regionId, attractionId, endTime, now, journalId)
    }

    fun addPhoto(journalId: String, localPath: String, latitude: Double?, longitude: Double?, takenAt: Long?) {
        val id = "photo_${Clock.System.now().toEpochMilliseconds()}_${localPath.hashCode().toUInt()}"
        val existing = journalRepository.getPhotosByJournal(journalId)
        val sortOrder = existing.size.toLong()
        val photo = JournalPhoto(id, journalId, localPath, latitude, longitude, takenAt, sortOrder)
        journalRepository.insertPhoto(photo)
    }

    fun addTrackPoints(journalId: String, points: List<Triple<Double, Double, Long>>) {
        val existing = journalRepository.getTrackPointsByJournal(journalId)
        val startOrder = existing.size.toLong()
        val now = Clock.System.now().toEpochMilliseconds()
        val trackPoints = points.mapIndexed { index, (lat, lng, ts) ->
            JournalTrackPoint(
                id = "tp_${now}_$index",
                journalId = journalId, latitude = lat, longitude = lng,
                altitude = 0.0, speed = 0.0, timestamp = ts, sortOrder = startOrder + index
            )
        }
        journalRepository.insertTrackPointsInTransaction(trackPoints)
    }

    fun getJournals(userId: String): List<Journal> {
        return journalRepository.getJournalsByUser(userId)
    }

    fun getJournalDetail(journalId: String): JournalDetail? {
        val journal = journalRepository.getJournal(journalId) ?: return null
        val photos = journalRepository.getPhotosByJournal(journalId)
        val trackPoints = journalRepository.getTrackPointsByJournal(journalId)
        val regionName = journal.regionId?.let { regionRepository.getRegion(it)?.name }
        val attractionName = journal.attractionId?.let { attractionRepository.getAttraction(it)?.name }
        return JournalDetail(journal, photos, trackPoints, regionName, attractionName)
    }

    fun deleteJournal(journalId: String) {
        journalRepository.deleteJournal(journalId)
    }

    fun deletePhoto(photoId: String) {
        journalRepository.deletePhoto(photoId)
    }

    fun getAllPhotosWithLocation(): List<JournalPhoto> {
        return journalRepository.getAllPhotosWithLocation()
    }
}
