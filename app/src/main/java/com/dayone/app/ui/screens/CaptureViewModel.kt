package com.dayone.app.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dayone.app.DayOneApp
import com.dayone.app.camera.EdgeMap
import com.dayone.app.camera.FaceCropper
import com.dayone.app.data.AppSettings
import com.dayone.app.data.GhostReference
import com.dayone.app.data.MediaStoreSaver
import com.dayone.app.data.StreakCalculator
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

/** A shot that has been processed but not yet committed, while the user reviews it. */
data class PendingShot(
    val processedFile: File,
    val preview: ImageBitmap,
    val face: android.graphics.RectF?
)

data class CaptureUiState(
    val project: Project? = null,
    val reference: ImageBitmap? = null,
    val referenceOutline: ImageBitmap? = null,
    val referenceLabel: String = "",
    val alreadyCapturedToday: Boolean = false,
    val dayNumber: Int = 1,
    val streak: Int = 0,
    val saving: Boolean = false,
    val pending: PendingShot? = null,
    val lastSavedEntry: PhotoEntry? = null,
    val error: String? = null
)

class CaptureViewModel(app: Application) : AndroidViewModel(app) {

    private val repo get() = getApplication<DayOneApp>().repository
    private val settingsRepo get() = getApplication<DayOneApp>().settingsRepository

    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    val settings: StateFlow<AppSettings> get() = settingsRepo.settings

    private var loadedProjectId: Long = -1

    fun load(projectId: Long) {
        if (loadedProjectId == projectId && _state.value.project != null) return
        loadedProjectId = projectId
        viewModelScope.launch {
            val project = repo.getProject(projectId) ?: return@launch
            val entries = repo.getEntries(projectId)
            val alreadyToday = entries.any { it.dateEpochDay == LocalDate.now().toEpochDay() }

            val stats = StreakCalculator.compute(
                capturedDays = entries.map { it.dateEpochDay }.toSet(),
                activeDaysMask = project.activeDaysMask,
                skippedDays = settingsRepo.skippedDays(projectId)
            )

            _state.value = _state.value.copy(
                project = project,
                alreadyCapturedToday = alreadyToday,
                dayNumber = entries.size + 1,
                streak = stats.current
            )
            loadReference(project, entries)
        }
    }

    /** Re-reads the overlay reference photo, e.g. after the reference setting changes. */
    fun reloadReference() {
        val project = _state.value.project ?: return
        viewModelScope.launch { loadReference(project, repo.getEntries(project.id)) }
    }

    private suspend fun loadReference(project: Project, entries: List<PhotoEntry>) {
        val settings = settingsRepo.current
        val pinnedDay = settingsRepo.pinnedReferenceDay(project.id)
        val today = LocalDate.now().toEpochDay()

        val entry = when (settings.ghostReference) {
            GhostReference.FIRST -> entries.minByOrNull { it.dateEpochDay }
            GhostReference.PINNED -> entries.firstOrNull { it.dateEpochDay == pinnedDay }
                ?: entries.filter { it.dateEpochDay != today }.maxByOrNull { it.dateEpochDay }
            GhostReference.PREVIOUS -> entries.filter { it.dateEpochDay != today }.maxByOrNull { it.dateEpochDay }
                ?: entries.maxByOrNull { it.dateEpochDay }
        }

        if (entry == null) {
            _state.value = _state.value.copy(reference = null, referenceOutline = null, referenceLabel = "")
            return
        }

        val (bitmap, outline) = withContext(Dispatchers.Default) {
            val decoded = runCatching {
                BitmapFactory.decodeFile(
                    entry.filePath,
                    BitmapFactory.Options().apply { inSampleSize = 2; inPreferredConfig = Bitmap.Config.ARGB_8888 }
                )
            }.getOrNull()
            // The outline is precomputed once here rather than per frame - a Sobel pass on
            // every recomposition would make the viewfinder stutter.
            val edges = decoded?.let { runCatching { EdgeMap.build(it) }.getOrNull() }
            decoded to edges
        }

        _state.value = _state.value.copy(
            reference = bitmap?.asImageBitmap(),
            referenceOutline = outline?.asImageBitmap(),
            referenceLabel = LocalDate.ofEpochDay(entry.dateEpochDay).toString()
        )
    }

    // ---------------------------------------------------------------- capture

    /**
     * Raw capture landed at [rawFile]; face-detect, crop, and either hold it for review
     * or commit it straight away depending on the user's preference.
     */
    fun processCapture(rawFile: File, reviewFirst: Boolean) {
        val project = _state.value.project ?: return
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                // Processed into the cache first: a discarded retake must not clobber a
                // photo that is already saved for today.
                val staged = File(getApplication<Application>().cacheDir, "staged_${System.currentTimeMillis()}.jpg")
                val result = withContext(Dispatchers.Default) {
                    FaceCropper.processAndSave(
                        srcFile = rawFile,
                        outFile = staged,
                        headFraction = project.headFraction,
                        autoCrop = project.autoCropFace
                    )
                }
                rawFile.delete()

                if (reviewFirst) {
                    val preview = withContext(Dispatchers.Default) {
                        BitmapFactory.decodeFile(
                            staged.absolutePath,
                            BitmapFactory.Options().apply { inSampleSize = 2 }
                        )
                    }
                    _state.value = _state.value.copy(
                        saving = false,
                        pending = PendingShot(staged, preview.asImageBitmap(), result.face)
                    )
                } else {
                    commit(staged, result.face)
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, error = t.message ?: "Failed to save photo")
            }
        }
    }

    fun confirmPending() {
        val pending = _state.value.pending ?: return
        _state.value = _state.value.copy(pending = null, saving = true)
        viewModelScope.launch { commit(pending.processedFile, pending.face) }
    }

    fun discardPending() {
        val pending = _state.value.pending ?: return
        runCatching { pending.processedFile.delete() }
        _state.value = _state.value.copy(pending = null, saving = false)
    }

    private suspend fun commit(staged: File, face: android.graphics.RectF?) {
        val project = _state.value.project ?: return
        try {
            val today = LocalDate.now()
            val destination = repo.fileForDay(project, today)
            withContext(Dispatchers.IO) {
                destination.parentFile?.mkdirs()
                staged.copyTo(destination, overwrite = true)
                staged.delete()
            }

            val entry = repo.saveEntry(project, today, destination, face)

            if (settingsRepo.current.saveCopyToGallery) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        MediaStoreSaver.saveImage(
                            context = getApplication(),
                            source = destination,
                            displayName = "${project.folderName}_$today.jpg",
                            subFolder = "DayOne/${project.folderName}"
                        )
                    }
                }
            }

            // Today's photo is done: cancel the repeat reminder until tomorrow.
            // markDoneToday reads the project back to re-arm tomorrow's alarm, so it
            // runs on the IO dispatcher rather than blocking the UI thread.
            withContext(Dispatchers.IO) { ReminderScheduler.markDoneToday(getApplication(), project.id) }
            settingsRepo.setSkipped(project.id, today.toEpochDay(), false)

            _state.value = _state.value.copy(
                saving = false,
                alreadyCapturedToday = true,
                lastSavedEntry = entry
            )
        } catch (t: Throwable) {
            _state.value = _state.value.copy(saving = false, error = t.message ?: "Failed to save photo")
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    // ---------------------------------------------------------------- overlay controls

    fun setGhostAlpha(value: Float) = settingsRepo.update { it.copy(ghostAlpha = value) }

    fun setGhostMode(mode: com.dayone.app.data.GhostMode) {
        settingsRepo.update { it.copy(ghostMode = mode) }
    }

    fun setGhostReference(reference: GhostReference) {
        settingsRepo.update { it.copy(ghostReference = reference) }
        reloadReference()
    }

    fun setGridMode(mode: com.dayone.app.data.GridMode) = settingsRepo.update { it.copy(gridMode = mode) }

    fun setFaceGuide(show: Boolean) = settingsRepo.update { it.copy(showFaceGuide = show) }

    fun setGhostFlip(flip: Boolean) = settingsRepo.update { it.copy(ghostFlip = flip) }

    fun nudgeGhost(dx: Float, dy: Float) = settingsRepo.update {
        it.copy(
            ghostOffsetX = (it.ghostOffsetX + dx).coerceIn(-0.5f, 0.5f),
            ghostOffsetY = (it.ghostOffsetY + dy).coerceIn(-0.5f, 0.5f)
        )
    }

    fun scaleGhost(factor: Float) = settingsRepo.update {
        it.copy(ghostScale = (it.ghostScale * factor).coerceIn(0.4f, 3f))
    }

    fun resetGhostTransform() = settingsRepo.update {
        it.copy(ghostScale = 1f, ghostOffsetX = 0f, ghostOffsetY = 0f)
    }

    fun setCountdown(seconds: Int) = settingsRepo.update { it.copy(countdownSeconds = seconds) }

    fun rememberLens(front: Boolean) = settingsRepo.update { it.copy(useFrontCamera = front) }
}
