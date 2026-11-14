package com.gharkharcha.di

import android.content.Context
import androidx.room.Room
import com.gharkharcha.data.dao.AttachmentDao
import com.gharkharcha.data.dao.AttendanceDao
import com.gharkharcha.data.dao.LabourerDao
import com.gharkharcha.data.dao.MaterialDao
import com.gharkharcha.data.dao.PaymentDao
import com.gharkharcha.data.dao.ProjectDao
import com.gharkharcha.data.dao.VendorDao
import com.gharkharcha.data.dao.WorkDao
import com.gharkharcha.data.db.GharKharchaDatabase
import com.gharkharcha.data.db.Migrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GharKharchaDatabase =
        Room.databaseBuilder(context, GharKharchaDatabase::class.java, GharKharchaDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides fun projectDao(db: GharKharchaDatabase): ProjectDao = db.projectDao()
    @Provides fun workDao(db: GharKharchaDatabase): WorkDao = db.workDao()
    @Provides fun vendorDao(db: GharKharchaDatabase): VendorDao = db.vendorDao()
    @Provides fun materialDao(db: GharKharchaDatabase): MaterialDao = db.materialDao()
    @Provides fun labourerDao(db: GharKharchaDatabase): LabourerDao = db.labourerDao()
    @Provides fun attendanceDao(db: GharKharchaDatabase): AttendanceDao = db.attendanceDao()
    @Provides fun paymentDao(db: GharKharchaDatabase): PaymentDao = db.paymentDao()
    @Provides fun attachmentDao(db: GharKharchaDatabase): AttachmentDao = db.attachmentDao()
}
