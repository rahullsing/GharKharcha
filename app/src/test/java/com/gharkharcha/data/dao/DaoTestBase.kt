package com.gharkharcha.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gharkharcha.data.db.GharKharchaDatabase
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.entity.WorkEntity
import org.junit.After
import org.junit.Before

/** In-memory Room DB on Robolectric; runs with `./gradlew test`, no device needed. */
abstract class DaoTestBase {
    protected lateinit var db: GharKharchaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GharKharchaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() = db.close()

    protected suspend fun newProject(name: String = "Ghar"): Long =
        db.projectDao().insert(ProjectEntity(name = name, budgetPaise = 10_00_000_00))

    protected suspend fun newWork(projectId: Long, name: String = "Electrical"): Long =
        db.workDao().insert(WorkEntity(projectId = projectId, name = name, budgetPaise = 50_000_00))

    protected suspend fun newLabourer(name: String = "Ramesh"): Long =
        db.labourerDao().insert(LabourerEntity(name = name, dailyWagePaise = 700_00))
}
