package com.gharkharcha.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gharkharcha.data.dao.AttachmentDao
import com.gharkharcha.data.dao.AttendanceDao
import com.gharkharcha.data.dao.LabourerDao
import com.gharkharcha.data.dao.MaterialDao
import com.gharkharcha.data.dao.PaymentDao
import com.gharkharcha.data.dao.ProjectDao
import com.gharkharcha.data.dao.VendorDao
import com.gharkharcha.data.dao.WorkDao
import com.gharkharcha.data.entity.AttachmentEntity
import com.gharkharcha.data.entity.AttendanceEntity
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.MaterialEntity
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.entity.VendorEntity
import com.gharkharcha.data.entity.WorkEntity

@Database(
    entities = [
        ProjectEntity::class,
        WorkEntity::class,
        VendorEntity::class,
        MaterialEntity::class,
        LabourerEntity::class,
        AttendanceEntity::class,
        PaymentEntity::class,
        AttachmentEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GharKharchaDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun workDao(): WorkDao
    abstract fun vendorDao(): VendorDao
    abstract fun materialDao(): MaterialDao
    abstract fun labourerDao(): LabourerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        const val NAME = "gharkharcha.db"
    }
}
