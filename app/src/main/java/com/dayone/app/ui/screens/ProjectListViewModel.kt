package com.dayone.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dayone.app.DayOneApp
import com.dayone.app.data.db.Project
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProjectListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo get() = (getApplication<DayOneApp>()).repository

    val projects: StateFlow<List<Project>> = repo.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _todayStatus = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val todayStatus: StateFlow<Map<Long, Boolean>> = _todayStatus.asStateFlow()

    init {
        viewModelScope.launch {
            projects.collect { list ->
                val map = list.associate { it.id to repo.hasCapturedToday(it) }
                _todayStatus.value = map
            }
        }
    }

    fun renameProject(project: Project, newName: String) {
        viewModelScope.launch {
            repo.updateProject(project.copy(name = newName))
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repo.deleteProject(project)
        }
    }
}
