package com.dayone.app.ui.screens

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayone.app.DayOneApp
import com.dayone.app.data.AppSettings
import com.dayone.app.data.db.PhotoEntry
import com.dayone.app.data.db.Project
import com.dayone.app.ui.components.ChoiceRow
import com.dayone.app.ui.components.SectionHeader
import com.dayone.app.ui.components.SettingsCard
import com.dayone.app.ui.components.SwitchRow
import com.dayone.app.video.ExportManager
import com.dayone.app.video.OverlayOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoExportScreen(
    projectId: Long,
    onBack: () -> Unit,
    viewModel: VideoExportViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(projectId) { viewModel.load(projectId) }

    val project by viewModel.project.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val globalExportState by ExportManager.state.collectAsStateWithLifecycle()
    // An export running for another project shouldn't take over this screen.
    val exportState = when (val s = globalExportState) {
        is ExportManager.State.Running -> if (s.projectId == projectId) s else ExportManager.State.Idle
        is ExportManager.State.Done -> if (s.projectId == projectId) s else ExportManager.State.Idle
        is ExportManager.State.Failed -> if (s.projectId == projectId) s else ExportManager.State.Idle
        is ExportManager.State.Cancelled -> if (s.projectId == projectId) s else ExportManager.State.Idle
        ExportManager.State.Idle -> ExportManager.State.Idle
    }

    var showDayNumber by remember { mutableStateOf(true) }
    var showDate by remember { mutableStateOf(true) }
    var showAge by remember { mutableStateOf(false) }
    var showYear by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var showProjectName by remember { mutableStateOf(false) }
    var showProgressBar by remember { mutableStateOf(true) }
    var favouritesOnly by remember { mutableStateOf(false) }

    LaunchedEffect(project?.birthDateMillis) {
        showAge = project?.birthDateMillis != null
    }

    val selected = remember(entries, favouritesOnly) {
        if (favouritesOnly) entries.filter { it.favorite } else entries
    }

    val estimatedSeconds = remember(selected, settings) {
        val perPhoto = settings.videoMillisPerPhoto / 1000f
        max(1, (selected.size * perPhoto).toInt())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Make video") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "${selected.size} photo(s) will be stitched into a timelapse - about ${estimatedSeconds}s " +
                    "at ${settings.videoResolution}p, ${settings.videoFps}fps.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            SectionHeader("Burned-in overlays")
            SettingsCard {
                SwitchRow("Day number", checked = showDayNumber) { showDayNumber = it }
                SwitchRow("Date", checked = showDate) { showDate = it }
                SwitchRow(
                    title = "Age",
                    subtitle = if (project?.birthDateMillis == null) "Set a birthday in project settings first" else null,
                    checked = showAge && project?.birthDateMillis != null,
                    enabled = project?.birthDateMillis != null
                ) { showAge = it }
                SwitchRow("Year", checked = showYear) { showYear = it }
                SwitchRow("Day notes", checked = showNote) { showNote = it }
                SwitchRow("Project name", checked = showProjectName) { showProjectName = it }
                SwitchRow("Progress bar", checked = showProgressBar) { showProgressBar = it }
            }

            SectionHeader("Video")
            SettingsCard {
                ChoiceRow(
                    title = "Resolution",
                    options = listOf(720, 1080, 1440),
                    selected = settings.videoResolution,
                    label = { "${it}p" },
                    onSelect = { viewModel.updateSettings { s -> s.copy(videoResolution = it) } }
                )
                ChoiceRow(
                    title = "Frame rate",
                    options = listOf(24, 30, 60),
                    selected = settings.videoFps,
                    label = { "$it fps" },
                    onSelect = { viewModel.updateSettings { s -> s.copy(videoFps = it) } }
                )
                ChoiceRow(
                    title = "Time per photo",
                    options = listOf(100, 200, 300, 500, 1000),
                    selected = settings.videoMillisPerPhoto,
                    label = { if (it >= 1000) "${it / 1000}s" else "${it}ms" },
                    onSelect = { viewModel.updateSettings { s -> s.copy(videoMillisPerPhoto = it) } }
                )
                ChoiceRow(
                    title = "Quality",
                    options = listOf(6, 12, 20, 30),
                    selected = settings.videoQualityMbps,
                    label = { "$it Mbps" },
                    onSelect = { viewModel.updateSettings { s -> s.copy(videoQualityMbps = it) } }
                )
                SwitchRow(
                    title = "Crossfade between days",
                    subtitle = "Smooth blend instead of a hard cut",
                    checked = settings.videoCrossfade
                ) { value -> viewModel.updateSettings { it.copy(videoCrossfade = value) } }
                SwitchRow(
                    title = "Newest first",
                    subtitle = "Play the timelapse backwards",
                    checked = settings.videoNewestFirst
                ) { value -> viewModel.updateSettings { it.copy(videoNewestFirst = value) } }
                SwitchRow(
                    title = "Save to Gallery",
                    subtitle = "Copies the finished video to Movies/DayOne so it shows up in your gallery and files",
                    checked = settings.videoSaveToGallery
                ) { value -> viewModel.updateSettings { it.copy(videoSaveToGallery = value) } }
                SwitchRow(
                    title = "Favourites only",
                    subtitle = "Only include days you starred in the timeline",
                    checked = favouritesOnly
                ) { favouritesOnly = it }
            }

            Spacer(Modifier.height(24.dp))

            when (val state = exportState) {
                is ExportManager.State.Running -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Encoding ${(state.progress * 100).toInt()}%")
                        Text(
                            "You can leave this screen - the export keeps running.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { ExportManager.cancel() }) { Text("Cancel") }
                    }
                }

                is ExportManager.State.Done -> {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Video ready", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (state.inGallery) "Saved to ${state.displayPath} - open it from your gallery or Files app any time."
                                else "Saved inside the app at ${state.displayPath}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(state.shareUri, "video/mp4")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Play")
                                }
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent.createChooser(
                                                    Intent(Intent.ACTION_SEND).apply {
                                                        type = "video/mp4"
                                                        putExtra(Intent.EXTRA_STREAM, state.shareUri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    },
                                                    "Share video"
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { ExportManager.reset() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Make another") }
                        }
                    }
                }

                is ExportManager.State.Failed -> {
                    Text(
                        "Export failed: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(12.dp))
                    ExportButton(enabled = selected.isNotEmpty()) {
                        startExport(
                            viewModel, project, selected, settings,
                            OverlayOptions(
                                showDayNumber, showDate, showAge && project?.birthDateMillis != null,
                                showYear, showNote, showProjectName, showProgressBar
                            )
                        )
                    }
                }

                else -> {
                    ExportButton(enabled = selected.isNotEmpty()) {
                        startExport(
                            viewModel, project, selected, settings,
                            OverlayOptions(
                                showDayNumber, showDate, showAge && project?.birthDateMillis != null,
                                showYear, showNote, showProjectName, showProgressBar
                            )
                        )
                    }
                    if (exportState is ExportManager.State.Cancelled) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Export cancelled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ExportButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Icon(Icons.Default.Movie, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Export video")
    }
}

private fun startExport(
    viewModel: VideoExportViewModel,
    project: Project?,
    entries: List<PhotoEntry>,
    settings: AppSettings,
    options: OverlayOptions
) {
    if (project == null || entries.isEmpty()) return
    viewModel.export(project, entries, options, settings)
}

class VideoExportViewModel(app: Application) : AndroidViewModel(app) {

    private val repo get() = getApplication<DayOneApp>().repository
    private val settingsRepo get() = getApplication<DayOneApp>().settingsRepository

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _entries = MutableStateFlow<List<PhotoEntry>>(emptyList())
    val entries: StateFlow<List<PhotoEntry>> = _entries.asStateFlow()

    val settings: StateFlow<AppSettings> get() = settingsRepo.settings

    private var loadedProjectId = -1L

    fun load(projectId: Long) {
        if (loadedProjectId == projectId) return
        loadedProjectId = projectId
        viewModelScope.launch {
            _project.value = repo.getProject(projectId)
            repo.observeEntries(projectId).collect { _entries.value = it }
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsRepo.update(transform)

    fun export(
        project: Project,
        entries: List<PhotoEntry>,
        options: OverlayOptions,
        settings: AppSettings
    ) {
        ExportManager.start(getApplication(), project, entries, options, settings)
    }
}
