package com.gharkharcha

import android.app.Application
import com.gharkharcha.data.files.AttachmentFileStore
import com.gharkharcha.di.ApplicationScope
import com.gharkharcha.di.IoDispatcher
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GharKharchaApp : Application() {
    @Inject lateinit var files: AttachmentFileStore

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    @Inject @IoDispatcher
    lateinit var io: CoroutineDispatcher

    override fun onCreate() {
        super.onCreate()
        // Photos picked in forms that were never saved.
        appScope.launch(io) { files.cleanCache() }
    }
}
