package com.gharkharcha.data.repo

import androidx.room.withTransaction
import com.gharkharcha.data.dao.AttendanceDao
import com.gharkharcha.data.dao.AttendanceWithWork
import com.gharkharcha.data.db.GharKharchaDatabase
import com.gharkharcha.data.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val db: GharKharchaDatabase,
    private val dao: AttendanceDao,
) {
    fun observeForWorkOnDate(workId: Long, date: LocalDate): Flow<List<AttendanceEntity>> =
        dao.observeForWorkOnDate(workId, date)

    fun observeForWork(workId: Long): Flow<List<AttendanceEntity>> = dao.observeForWork(workId)

    fun observeForLabourer(labourerId: Long, projectId: Long? = null): Flow<List<AttendanceWithWork>> =
        dao.observeForLabourer(labourerId, projectId)

    fun observeDayCost(projectId: Long, date: LocalDate): Flow<Long> = dao.observeDayCost(projectId, date)

    /** Insert-or-replace by (work, labourer, date), preserving the existing row id. */
    suspend fun save(entry: AttendanceEntity): Long = db.withTransaction { saveInternal(entry) }

    private suspend fun saveInternal(entry: AttendanceEntity): Long {
        val existing = dao.find(entry.workId, entry.labourerId, entry.date)
        return if (existing == null) {
            dao.insert(entry.copy(id = 0))
        } else {
            dao.update(
                entry.copy(id = existing.id, createdAt = existing.createdAt, updatedAt = System.currentTimeMillis()),
            )
            existing.id
        }
    }

    suspend fun saveAll(entries: List<AttendanceEntity>) = db.withTransaction {
        entries.forEach { saveInternal(it) }
    }

    suspend fun clear(workId: Long, labourerId: Long, date: LocalDate) {
        dao.find(workId, labourerId, date)?.let { dao.delete(it) }
    }

    suspend fun delete(entry: AttendanceEntity) = dao.delete(entry)

    /**
     * Copies entries of [from] onto [to] for the same work. Labourers already marked on [to]
     * are left untouched. Returns how many entries were created.
     */
    suspend fun copyDay(workId: Long, from: LocalDate, to: LocalDate): Int = db.withTransaction {
        val source = dao.listForWorkOnDate(workId, from)
        val alreadyMarked = dao.listForWorkOnDate(workId, to).map { it.labourerId }.toSet()
        val now = System.currentTimeMillis()
        val toInsert = source.filter { it.labourerId !in alreadyMarked }
        toInsert.forEach {
            // Advances are a one-off cash event, not repeated by copying.
            dao.insert(it.copy(id = 0, date = to, advancePaise = 0, createdAt = now, updatedAt = now))
        }
        toInsert.size
    }

    /** Bulk add: repeats [from]'s entries on every date in [dates] (skipping [from] itself). */
    suspend fun repeatDay(workId: Long, from: LocalDate, dates: List<LocalDate>): Int = db.withTransaction {
        dates.filter { it != from }.sumOf { copyDay(workId, from, it) }
    }

    suspend fun listForWorkOnDate(workId: Long, date: LocalDate): List<AttendanceEntity> =
        dao.listForWorkOnDate(workId, date)
}
