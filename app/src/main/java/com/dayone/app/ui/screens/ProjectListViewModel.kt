package com.dayone.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dayone.app.DayOneApp
import com.dayone.app.data.StreakCalculator
import com.dayone.app.data.StreakStats
import com.dayone.app.data.db.PhotoEntry
import com.dayone.app.data.db.Project
import com.dayone.app.notify.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class ProjectCardState(
    val project: Project,
    val stats: StreakStats,
    val latestPhotoPath: String?,
    val skippedToday: Boolean
)

class ProjectListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo get() = getApplication<DayOneApp>().repository
    private val settings get() = getApplication<DayOneApp>().settingsRepository

    // Skipped days live in SharedPreferences rather than the database, so a manual nudge
    // is what brings them into the derived state.
    private val refreshTrigger = MutableStateFlow(0)

    fun refresh() {
        refreshTrigger.value++
    }

    /**
     * Cards are derived from the live flows, so taking a photo or editing a schedule
     * updates the list immediately without any manual refresh.
     */
    val cards: StateFlow<List<ProjectCardState>> =
        combine(repo.observeProjects(), repo.observeAllEntries(), refreshTrigger) { projects, entries, _ ->
            val byProject: Map<Long, List<PhotoEntry>> = entries.groupBy { it.projectId }
            val today = LocalDate.now()
            projects.map { project ->
                val projectEntries = byProject[project.id].orEmpty()
                ProjectCardState(
                    project = project,
                    stats = StreakCalculator.compute(
                        capturedDays = projectEntries.map { it.dateEpochDay }.toSet(),
                        activeDaysMask = project.activeDaysMask,
                        skippedDays = settings.skippedDays(project.id),
                        today = today
                    ),
                    latestPhotoPath = projectEntries.maxByOrNull { it.dateEpochDay }?.filePath,
                    skippedToday = settings.isSkipped(project.id, today.toEpochDay())
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeCards: StateFlow<List<ProjectCardState>> = cards
        .map { list -> list.filter { !it.project.archived } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedCards: StateFlow<List<ProjectCardState>> = cards
        .map { list -> list.filter { it.project.archived } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun renameProject(project: Project, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repo.updateProject(project.copy(name = newName.trim())) }
    }

    fun setArchived(project: Project, archived: Boolean) {
        viewModelScope.launch {
            repo.updateProject(project.copy(archived = archived))
            withContext(Dispatchers.IO) {
                if (archived) ReminderScheduler.cancelAll(getApplication(), project.id)
                else ReminderScheduler.scheduleById(getApplication(), project.id)
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { ReminderScheduler.cancelAll(getApplication(), project.id) }
            repo.deleteProject(project)
        }
    }

    fun skipToday(project: Project) {
        viewModelScope.launch {
            settings.setSkipped(project.id, LocalDate.now().toEpochDay(), true)
            withContext(Dispatchers.IO) { ReminderScheduler.markDoneToday(getApplication(), project.id) }
            refresh()
        }
    }
}
