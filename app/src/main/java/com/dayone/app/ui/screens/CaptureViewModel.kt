package com.dayone.app.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dayone.app.DayOneApp
import com.dayone.app.camera.FaceCropper
import com.dayone.app.data.db.PhotoEntry
import com.dayone.app.data.db.Project
import com.dayone.app.notify.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

data class CaptureUiState(
    val project: Project? = null,
    val previousPhotoBitmap: Bitmap? = null,
    val alreadyCapturedToday: Boolean = false,
    val ghostAlpha: Float = 0.35f,
    val showGuide: Boolean = true,
    val zoomScalar: Float = 1.5f,
    val saving: Boolean = false,
    val lastSavedEntry: PhotoEntry? = null,
    val error: String? = null
)

class CaptureViewModel(app: Application) : AndroidViewModel(app) {

    private val repo get() = (getApplication<DayOneApp>()).repository
    private val settings get() = (getApplication<DayOneApp>()).settingsRepository

    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    fun load(projectId: Long) {
        viewModelScope.launch {
            val project = repo.getProject(projectId) ?: return@launch
            val alreadyToday = repo.hasCapturedToday(project)
            val latest = repo.getLatestEntry(projectId)
            val bmp = latest?.let { entry ->
                withContext(Dispatchers.IO) {
                    runCatching { BitmapFactory.decodeFile(entry.filePath) }.getOrNull()
                }
            }
            _state.value = _state.value.copy(
                project = project,
                previousPhotoBitmap = bmp,
                alreadyCapturedToday = alreadyToday,
                zoomScalar = settings.zoomScalar
            )
        }
    }

    fun setGhostAlpha(v: Float) {
        _state.value = _state.value.copy(ghostAlpha = v)
    }

    fun setShowGuide(v: Boolean) {
        _state.value = _state.value.copy(showGuide = v)
    }

    /** Raw capture landed at [rawFile]; process it (face-detect + crop) and persist an entry. */
    fun processCapture(rawFile: File) {
        val project = _state.value.project ?: return
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val outFile = repo.fileForDay(project, today)
                val face = withContext(Dispatchers.Default) {
                    FaceCropper.processAndSave(rawFile, outFile)
                }
                rawFile.delete()
                val entry = repo.saveEntry(project, today, outFile, face)

                // Today's photo is done: cancel the nagging reminder until tomorrow.
                ReminderScheduler.markDoneToday(getApplication(), project.id)

                _state.value = _state.value.copy(
                    saving = false,
                    alreadyCapturedToday = true,
                    lastSavedEntry = entry
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, error = t.message ?: "Failed to save photo")
            }
        }
    }
}
