package com.gharkharcha.data.sample

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.gharkharcha.data.dao.DaoTestBase
import com.gharkharcha.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SampleDataSeederTest : DaoTestBase() {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val prefsRepo by lazy {
        val dataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_settings_${System.nanoTime()}")
        }
        UserPreferencesRepository(dataStore)
    }

    @Test
    fun `seeder populates project, works, labourers, materials, and attendance`() = runTest {
        val seeder = SampleDataSeeder(
            db = db,
            prefs = prefsRepo,
            io = Dispatchers.Unconfined,
        )

        seeder.seedSampleData()

        val selectedProjectId = prefsRepo.preferences.first().selectedProjectId
        assertNotNull(selectedProjectId)

        val project = db.projectDao().get(selectedProjectId!!)
        assertNotNull(project)
        assertEquals("Dream Villa (Sector 62)", project!!.name)

        val works = db.workDao().observeByProject(selectedProjectId).first()
        assertEquals(3, works.size)

        val labourers = db.labourerDao().observeAll().first()
        assertEquals(4, labourers.size)

        val materials = db.materialDao().observeByWork(works[0].id).first()
        assertTrue(materials.size >= 3)

        val payments = db.paymentDao().observeForLabourer(labourers[0].id).first()
        assertTrue(payments.isNotEmpty())
    }
}
