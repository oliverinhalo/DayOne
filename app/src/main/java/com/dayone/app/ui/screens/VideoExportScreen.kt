package com.dayone.app.ui.screens

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dayone.app.DayOneApp
import com.dayone.app.data.db.PhotoEntry
import com.dayone.app.data.db.Project
import com.dayone.app.video.OverlayOptions
import com.dayone.app.video.VideoExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class ExportState {
    object Idle : ExportState()
    data class InProgress(val progress: Float) : ExportState()
    data class Done(val file: File) : ExportState()
    data class Failed(val message: String) : ExportState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoExportScreen(
    projectId: Long,
    onBack: () -> Unit,
    viewModel: VideoExportViewModel = viewModel_()
) {
    val context = LocalContext.current
    LaunchedEffect(projectId) { viewModel.load(projectId) }

    val project by viewModel.project.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    var showDayNumber by remember { mutableStateOf(true) }
    var showDate by remember { mutableStateOf(true) }
    var showAge by remember { mutableStateOf(project?.birthDateMillis != null) }
    var showYear by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Make video") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {

            Text(
                "${entries.size} photos will be stitched into a timelapse, in order, with your chosen overlays burned into each frame.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            Text("Overlay fields", style = MaterialTheme.typography.titleSmall)

            OverlayCheckbox("Day number", showDayNumber) { showDayNumber = it }
            OverlayCheckbox("Date", showDate) { showDate = it }
            OverlayCheckbox(
                "Age" + if (project?.birthDateMillis == null) " (set a birthdate in project settings first)" else "",
                showAge && project?.birthDateMillis != null,
                enabled = project?.birthDateMillis != null
            ) { showAge = it }
            OverlayCheckbox("Year", showYear) { showYear = it }

            Spacer(Modifier.height(32.dp))

            when (val s = exportState) {
                is ExportState.InProgress -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("${(s.progress * 100).toInt()}%")
                    }
                }
                is ExportState.Done -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Video ready: ${s.file.name}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val uri = FileProvider.getUriForFile(
                                    context, "${context.packageName}.fileprovider", s.file
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "video/mp4")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open video"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Open / share video") }
                    }
                }
                is ExportState.Failed -> {
                    Text("Export failed: ${s.message}", color = MaterialTheme.colorScheme.error)
                }
                ExportState.Idle -> {
                    Button(
                        onClick = {
                            viewModel.export(
                                OverlayOptions(
                                    showDayNumber = showDayNumber,
                                    showDate = showDate,
                                    showAge = showAge && project?.birthDateMillis != null,
                                    showYear = showYear
                                )
                            )
                        },
                        enabled = entries.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export video")
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayCheckbox(label: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

class VideoExportViewModel(app: Application) : AndroidViewModel(app) {
    private val repo get() = (getApplication<DayOneApp>()).repository

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _entries = MutableStateFlow<List<PhotoEntry>>(emptyList())
    val entries: StateFlow<List<PhotoEntry>> = _entries.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private var currentProjectId: Long = -1

    fun load(projectId: Long) {
        currentProjectId = projectId
        viewModelScope.launch {
            _project.value = repo.getProject(projectId)
            repo.observeEntries(projectId).collect { _entries.value = it }
        }
    }

    fun export(options: OverlayOptions) {
        val project = _project.value ?: return
        val entries = _entries.value
        if (entries.isEmpty()) return

        _exportState.value = ExportState.InProgress(0f)
        viewModelScope.launch {
            try {
                val outDir = File(getApplication<Application>().getExternalFilesDir(null), "DayOne/exports").apply { mkdirs() }
                val outFile = File(outDir, "${project.folderName}_${System.currentTimeMillis()}.mp4")

                withContext(Dispatchers.Default) {
                    VideoExporter().export(project, entries, options, outFile) { progress ->
                        _exportState.value = ExportState.InProgress(progress)
                    }
                }
                _exportState.value = ExportState.Done(outFile)
            } catch (t: Throwable) {
                _exportState.value = ExportState.Failed(t.message ?: "Unknown error")
            }
        }
    }
}

@Composable
private fun viewModel_(): VideoExportViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
