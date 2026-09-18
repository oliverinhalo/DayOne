package com.dayone.app.video

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.dayone.app.data.AppSettings
import com.dayone.app.data.MediaStoreSaver
import com.dayone.app.data.db.PhotoEntry
import com.dayone.app.data.db.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Owns video exports at application scope.
 *
 * Encoding a few hundred frames takes a while, so it deliberately does not live in a
 * ViewModel: leaving the export screen (or rotating the phone) would cancel the job
 * half-written. The screen just observes [state].
 */
object ExportManager {

    sealed interface State {
        data object Idle : State
        data class Running(val projectId: Long, val progress: Float) : State
        data class Done(
            val projectId: Long,
            val shareUri: Uri,
            val displayPath: String,
            val inGallery: Boolean
        ) : State
        data class Failed(val projectId: Long, val message: String) : State
        data class Cancelled(val projectId: Long) : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile private var cancelRequested = false
    private var job: Job? = null

    val isRunning: Boolean get() = _state.value is State.Running

    fun start(
        context: Context,
        project: Project,
        entries: List<PhotoEntry>,
        options: OverlayOptions,
        settings: AppSettings
    ) {
        if (isRunning) return
        val appContext = context.applicationContext
        cancelRequested = false
        _state.value = State.Running(project.id, 0f)

        job = scope.launch {
            // Written to app storage first; publishing to the gallery is a copy step, so a
            // failed or cancelled encode never leaves a broken file in the user's Movies.
            val exportsDir = File(appContext.getExternalFilesDir(null), "DayOne/exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
            val displayName = "${project.folderName}_$stamp.mp4"
            val outFile = File(exportsDir, displayName)

            try {
                val ordered = if (settings.videoNewestFirst) entries.reversed() else entries

                val exporter = VideoExporter(
                    size = settings.videoResolution,
                    fps = settings.videoFps,
                    millisPerPhoto = settings.videoMillisPerPhoto,
                    bitRate = settings.videoQualityMbps * 1_000_000,
                    crossfade = settings.videoCrossfade
                )

                val result = exporter.export(
                    project = project,
                    entries = ordered,
                    options = options,
                    outFile = outFile,
                    isCancelled = { cancelRequested },
                    onProgress = { progress ->
                        _state.value = State.Running(project.id, progress.coerceIn(0f, 1f))
                    }
                )

                if (result == null) {
                    _state.value = State.Failed(project.id, "There are no photos to export yet")
                    return@launch
                }

                var displayPath = "DayOne/exports/$displayName"
                var inGallery = false
                if (settings.videoSaveToGallery) {
                    val saved = MediaStoreSaver.saveVideo(appContext, outFile, displayName)
                    if (saved != null) {
                        displayPath = saved.displayPath
                        inGallery = true
                    }
                }

                _state.value = State.Done(
                    projectId = project.id,
                    shareUri = FileProvider.getUriForFile(
                        appContext, "${appContext.packageName}.fileprovider", outFile
                    ),
                    displayPath = displayPath,
                    inGallery = inGallery
                )
            } catch (cancelled: ExportCancelledException) {
                runCatching { outFile.delete() }
                _state.value = State.Cancelled(project.id)
            } catch (t: Throwable) {
                runCatching { outFile.delete() }
                _state.value = State.Failed(project.id, t.message ?: t::class.java.simpleName)
            }
        }
    }

    fun cancel() {
        cancelRequested = true
    }

    /** Clears a terminal state so the screen shows its normal controls again. */
    fun reset() {
        if (!isRunning) _state.value = State.Idle
    }
}
