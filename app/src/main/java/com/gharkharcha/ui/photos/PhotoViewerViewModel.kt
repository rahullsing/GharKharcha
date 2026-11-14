package com.gharkharcha.ui.photos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.data.entity.AttachmentEntity
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.repo.AttachmentRepository
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.retryableLoad
import com.gharkharcha.ui.navigation.PhotoViewerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ViewerPhoto(val attachment: AttachmentEntity, val file: File)

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val attachments: AttachmentRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<PhotoViewerRoute>()
    val startId: Long = route.startId
    private val retry = MutableStateFlow(0)

    val state: StateFlow<LoadState<List<ViewerPhoto>>> = retryableLoad(retry) {
        val source = if (route.workId != 0L) {
            attachments.observeForWorkTree(route.workId)
        } else {
            attachments.observeFor(OwnerType.valueOf(route.ownerType), route.ownerId)
        }
        source.map { list -> list.map { ViewerPhoto(it, attachments.fileOf(it)) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun retry() = retry.update { it + 1 }

    fun saveCaption(photo: ViewerPhoto, caption: String) {
        viewModelScope.launch { attachments.updateCaption(photo.attachment, caption) }
    }

    fun delete(photo: ViewerPhoto) {
        viewModelScope.launch { attachments.delete(photo.attachment) }
    }
}
