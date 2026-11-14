package com.gharkharcha.data.repo

import androidx.room.withTransaction
import com.gharkharcha.data.dao.AttachmentDao
import com.gharkharcha.data.dao.PaymentDao
import com.gharkharcha.data.db.GharKharchaDatabase
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val db: GharKharchaDatabase,
    private val dao: PaymentDao,
    private val attachmentDao: AttachmentDao,
    private val attachments: AttachmentRepository,
) {
    fun observeForLabourer(labourerId: Long, projectId: Long? = null): Flow<List<PaymentEntity>> =
        dao.observeForLabourer(labourerId, projectId)

    fun observeForWork(workId: Long): Flow<List<PaymentEntity>> = dao.observeForWork(workId)

    fun observeForProject(projectId: Long): Flow<List<PaymentEntity>> = dao.observeForProject(projectId)

    suspend fun get(id: Long): PaymentEntity? = dao.get(id)

    suspend fun save(payment: PaymentEntity): Long {
        require(payment.amountPaise > 0) { "Payment amount must be positive" }
        return if (payment.id == 0L) {
            dao.insert(payment)
        } else {
            dao.update(payment.copy(updatedAt = System.currentTimeMillis()))
            payment.id
        }
    }

    suspend fun delete(payment: PaymentEntity) {
        val owned = db.withTransaction {
            val list = attachmentDao.listFor(OwnerType.PAYMENT, payment.id)
            attachmentDao.deleteAll(list)
            dao.delete(payment)
            list
        }
        attachments.deleteFiles(owned)
    }
}
