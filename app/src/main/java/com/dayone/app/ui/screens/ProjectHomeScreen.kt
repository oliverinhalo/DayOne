package com.dayone.app.ui.screens

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dayone.app.DayOneApp
import com.dayone.app.data.BackupManager
import com.dayone.app.data.StreakCalculator
import com.dayone.app.data.StreakStats
import com.dayone.app.data.db.PhotoEntry
import com.dayone.app.data.db.Project
import com.dayone.app.data.db.Weekdays
import com.dayone.app.ui.components.MonthGrid
import com.dayone.app.ui.components.StatTile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectHomeScreen(
    projectId: Long,
    onCapture: () -> Unit,
    onTimeline: (Long?) -> Unit,
    onVideoExport: () -> Unit,
    onProjectSettings: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProjectHomeViewModel = viewModel()
) {
    LaunchedEffect(projectId) { viewModel.load(projectId) }
    val project by viewModel.project.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var visibleMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPhotos(context, uris) { added, skipped ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (added == 0) "Nothing imported - $skipped file(s) already had a photo for their date"
                        else "Imported $added photo(s)" + if (skipped > 0) ", skipped $skipped" else ""
                    )
                }
            }
        }
    }

    val accent = project?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onProjectSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Project settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    value = stats.current.toString(),
                    label = "Current streak",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = stats.best.toString(),
                    label = "Best streak",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = stats.total.toString(),
                    label = "Photos",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = "${(stats.completion * 100).toInt()}%",
                    label = "Completion",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(18.dp))

            val latest = entries.lastOrNull()
            if (latest != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    AsyncImage(
                        model = latest.filePath,
                        contentDescription = "Most recent photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            LocalDate.ofEpochDay(latest.dateEpochDay)
                                .format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("No photos yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onCapture,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        stats.doneToday -> "Retake today's photo"
                        stats.restDayToday -> "Take a photo anyway"
                        else -> "Take today's photo"
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onTimeline(null) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Timeline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Timeline")
                }
                OutlinedButton(onClick = onVideoExport, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Movie, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Make video")
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { importLauncher.launch("image/*") },
                enabled = !importing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (importing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (importing) "Importing..." else "Import photos for past days")
            }

            Spacer(Modifier.height(22.dp))

            // ---- calendar --------------------------------------------------------
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                        }
                        Text(
                            visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        IconButton(
                            onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                            enabled = visibleMonth.isBefore(LocalDate.now().withDayOfMonth(1))
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MonthGrid(
                        month = visibleMonth,
                        capturedDays = entries.map { it.dateEpochDay }.toSet(),
                        skippedDays = viewModel.skippedDays(),
                        activeDaysMask = project?.activeDaysMask ?: Weekdays.ALL,
                        accent = accent,
                        onDayClick = { date -> onTimeline(date.toEpochDay()) }
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        scheduleSummary(project, stats),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun scheduleSummary(project: Project?, stats: StreakStats): String {
    if (project == null) return ""
    val schedule = Weekdays.label(project.activeDaysMask)
    val missed = if (stats.missed == 0) "no missed days" else "${stats.missed} missed day(s)"
    return "$schedule  •  $missed"
}

class ProjectHomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo get() = getApplication<DayOneApp>().repository
    private val settings get() = getApplication<DayOneApp>().settingsRepository

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _entries = MutableStateFlow<List<PhotoEntry>>(emptyList())
    val entries: StateFlow<List<PhotoEntry>> = _entries.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    val stats: StateFlow<StreakStats> =
        combine(_entries, _project) { entries, project ->
            StreakCalculator.compute(
                capturedDays = entries.map { it.dateEpochDay }.toSet(),
                activeDaysMask = project?.activeDaysMask ?: Weekdays.ALL,
                skippedDays = project?.let { settings.skippedDays(it.id) } ?: emptySet()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreakStats())

    private var loadedProjectId = -1L

    fun load(projectId: Long) {
        if (loadedProjectId == projectId) return
        loadedProjectId = projectId
        viewModelScope.launch {
            repo.observeProject(projectId).collect { _project.value = it }
        }
        viewModelScope.launch {
            repo.observeEntries(projectId).collect { _entries.value = it }
        }
    }

    fun skippedDays(): Set<Long> = _project.value?.let { settings.skippedDays(it.id) } ?: emptySet()

    /**
     * Brings in existing photos - a restore from an older install, or filling in days that
     * were shot on the normal camera. Each file's date comes from its name or EXIF data.
     */
    fun importPhotos(context: Context, uris: List<Uri>, onFinished: (added: Int, skipped: Int) -> Unit) {
        val project = _project.value ?: return
        _importing.value = true
        viewModelScope.launch {
            val result = BackupManager.importImages(
                context = context,
                repo = repo,
                project = project,
                uris = uris,
                copyToGallery = settings.current.saveCopyToGallery
            )
            _importing.value = false
            onFinished(result.photosAdded, result.photosSkipped)
        }
    }
}
