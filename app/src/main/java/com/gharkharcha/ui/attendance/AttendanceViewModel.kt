package com.gharkharcha.ui.attendance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.R
import com.gharkharcha.data.entity.AttendanceEntity
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.entity.WorkStatus
import com.gharkharcha.data.prefs.UserPreferencesRepository
import com.gharkharcha.data.repo.AttendanceRepository
import com.gharkharcha.data.repo.LabourerRepository
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.repo.AttachmentRepository
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.domain.calc.AttendanceDraft
import com.gharkharcha.domain.calc.AttendanceDrafts
import com.gharkharcha.domain.usecase.CurrentProjectUseCase
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.SavedForm
import com.gharkharcha.ui.common.UiText
import com.gharkharcha.ui.common.retryableLoad
import com.gharkharcha.ui.navigation.AttendanceRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import java.time.LocalDate
import javax.inject.Inject

data class AttendanceRowUi(val labourer: LabourerEntity, val entry: AttendanceEntity?, val photoPath: String? = null)

data class AttendanceUi(
    val date: LocalDate,
    val isToday: Boolean,
    val canGoForward: Boolean,
    val works: List<WorkEntity>,
    /** Null when the house has no works. */
    val work: WorkEntity?,
    val rows: List<AttendanceRowUi>,
    val dayTotalPaise: Long,
    val presentCount: Int,
    val hoursPerDay: Int,
)

/** The labourer whose details sheet is open, with the values being edited. */
@Serializable
data class EditorState(
    val labourerId: Long,
    val draft: AttendanceDraft,
    val isExisting: Boolean,
    val showErrors: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    currentProject: CurrentProjectUseCase,
    private val works: WorkRepository,
    private val labourers: LabourerRepository,
    private val attendance: AttendanceRepository,
    prefs: UserPreferencesRepository,
    attachments: AttachmentRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AttendanceRoute>()
    private val today = LocalDate.now()
    private val retry = MutableStateFlow(0)

    private val day = savedStateHandle.getStateFlow(KEY_DAY, route.epochDay ?: today.toEpochDay())
    private val selectedWorkId = savedStateHandle.getStateFlow(KEY_WORK, route.workId)

    private val editorForm = SavedForm(savedStateHandle, KEY_EDITOR, EditorState.serializer().nullable) { null }
    val editor: StateFlow<EditorState?> = editorForm.state

    private val messageChannel = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = messageChannel.receiveAsFlow()

    private val hoursPerDay = prefs.hoursPerDay.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** House of the work we were opened with, otherwise the current house. */
    private val projectId: Flow<Long?> =
        if (route.workId != 0L) {
            flow { emit(works.get(route.workId)?.projectId) }.flatMapLatest { id ->
                if (id != null) flowOf(id) else currentProject.state.map { it.current?.id }
            }
        } else {
            currentProject.state.map { it.current?.id }
        }

    private val projectWorks: Flow<List<WorkEntity>> = projectId.distinctUntilChanged().flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else works.observeByProject(id)
    }

    private val currentWork: Flow<WorkEntity?> = combine(projectWorks, selectedWorkId) { list, id ->
        list.firstOrNull { it.id == id }
            ?: list.firstOrNull { it.status == WorkStatus.IN_PROGRESS }
            ?: list.firstOrNull()
    }.distinctUntilChanged()

    private val entries: Flow<List<AttendanceEntity>> = combine(currentWork, day) { w, d -> w to d }
        .flatMapLatest { (w, d) ->
            if (w == null) flowOf(emptyList()) else attendance.observeForWorkOnDate(w.id, LocalDate.ofEpochDay(d))
        }

    val state: StateFlow<LoadState<AttendanceUi>> = retryableLoad(retry) {
        combine(
            combine(projectWorks, currentWork, ::Pair),
            entries,
            combine(labourers.observeAll(), attachments.observeFirstPhotos(OwnerType.LABOURER), ::Pair),
            day,
            prefs.hoursPerDay,
        ) { (workList, work), dayEntries, (people, photos), d, hpd ->
            val date = LocalDate.ofEpochDay(d)
            val byLabourer = dayEntries.associateBy { it.labourerId }
            val rows = people
                .filter { it.isActive || it.id in byLabourer }
                .map { AttendanceRowUi(it, byLabourer[it.id], photos[it.id]) }
            AttendanceUi(
                date = date,
                isToday = date == today,
                canGoForward = date < today,
                works = workList,
                work = work,
                rows = rows,
                dayTotalPaise = dayEntries.sumOf { it.totalPayPaise },
                presentCount = dayEntries.count { it.status != AttendanceStatus.ABSENT },
                hoursPerDay = hpd,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun retry() = retry.update { it + 1 }

    fun setDate(date: LocalDate) {
        savedStateHandle[KEY_DAY] = minOf(date, today).toEpochDay()
    }

    fun shiftDay(by: Long) = setDate(LocalDate.ofEpochDay(day.value).plusDays(by))

    fun selectWork(work: WorkEntity) {
        savedStateHandle[KEY_WORK] = work.id
    }

    /** One-tap Full / Half / Absent. "Hours" needs a number, so it opens the details sheet. */
    fun quickMark(row: AttendanceRowUi, status: AttendanceStatus) {
        if (status == AttendanceStatus.HOURS) {
            openEditor(row, AttendanceStatus.HOURS)
            return
        }
        launchSafely {
            val ui = (state.value as? LoadState.Ready)?.data ?: return@launchSafely
            val work = ui.work ?: return@launchSafely
            attendance.save(AttendanceDrafts.quick(row.entry, row.labourer, work.id, ui.date, status, ui.hoursPerDay))
        }
    }

    fun openEditor(row: AttendanceRowUi, presetStatus: AttendanceStatus? = null) {
        val hpd = hoursPerDay.value ?: return
        editorForm.update {
            EditorState(
                labourerId = row.labourer.id,
                draft = AttendanceDrafts.from(row.entry, row.labourer, hpd, presetStatus),
                isExisting = row.entry != null,
            )
        }
    }

    fun updateDraft(transform: (AttendanceDraft) -> AttendanceDraft) =
        editorForm.update { it?.copy(draft = transform(it.draft)) }

    fun closeEditor() = editorForm.update { null }

    fun saveEditor() {
        val ed = editorForm.value ?: return
        if (ed.draft.hoursMissing) {
            editorForm.update { it?.copy(showErrors = true) }
            return
        }
        launchSafely {
            val ui = (state.value as? LoadState.Ready)?.data ?: return@launchSafely
            val work = ui.work ?: return@launchSafely
            val existing = ui.rows.firstOrNull { it.labourer.id == ed.labourerId }?.entry
            attendance.save(ed.draft.toEntity(existing, work.id, ed.labourerId, ui.date, ui.hoursPerDay))
            closeEditor()
        }
    }

    fun clearEditorEntry() {
        val ed = editorForm.value ?: return
        launchSafely {
            val ui = (state.value as? LoadState.Ready)?.data ?: return@launchSafely
            val work = ui.work ?: return@launchSafely
            attendance.clear(work.id, ed.labourerId, ui.date)
            closeEditor()
        }
    }

    fun copyPreviousDay() = launchSafely {
        val ui = (state.value as? LoadState.Ready)?.data ?: return@launchSafely
        val work = ui.work ?: return@launchSafely
        val count = attendance.copyDay(work.id, ui.date.minusDays(1), ui.date)
        messageChannel.send(
            if (count > 0) UiText.Plural(R.plurals.attendance_copied, count) else UiText.of(R.string.attendance_nothing_to_copy),
        )
    }

    /** Repeats the shown day on every date from [start] to [end] (inclusive, never after today). */
    fun repeatDay(start: LocalDate, end: LocalDate) = launchSafely {
        val ui = (state.value as? LoadState.Ready)?.data ?: return@launchSafely
        val work = ui.work ?: return@launchSafely
        if (attendance.listForWorkOnDate(work.id, ui.date).isEmpty()) {
            messageChannel.send(UiText.of(R.string.attendance_range_empty))
            return@launchSafely
        }
        val last = minOf(end, today)
        val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= last }.toList()
        val count = attendance.repeatDay(work.id, ui.date, dates)
        messageChannel.send(
            if (count > 0) UiText.Plural(R.plurals.attendance_range_done, count) else UiText.of(R.string.attendance_range_nothing),
        )
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                messageChannel.send(UiText.of(R.string.save_failed))
            }
        }
    }

    private companion object {
        const val KEY_DAY = "day"
        const val KEY_WORK = "work_id"
        const val KEY_EDITOR = "editor"
    }
}
