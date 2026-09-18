package com.dayone.app.ui.screens

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.dayone.app.DayOneApp
import com.dayone.app.data.db.PhotoEntry
import com.dayone.app.data.db.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectHomeScreen(
    projectId: Long,
    onCapture: () -> Unit,
    onTimeline: () -> Unit,
    onVideoExport: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProjectHomeViewModel = viewModel_()
) {
    LaunchedEffect(projectId) { viewModel.load(projectId) }
    val project by viewModel.project.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadPhoto(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatChip(label = "Total photos", value = entries.size.toString())
                StatChip(label = "Current streak", value = "$streak")
            }

            Spacer(Modifier.height(24.dp))

            val latest = entries.lastOrNull()
            if (latest != null) {
                AsyncImage(
                    model = latest.filePath,
                    contentDescription = "Most recent photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No photos yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(onClick = onCapture, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Take today's photo")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Upload for previous day")
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onTimeline, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Timeline, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Timeline")
                }
                OutlinedButton(onClick = onVideoExport, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Movie, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Make video")
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

class ProjectHomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo get() = (getApplication<DayOneApp>()).repository

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _entries = MutableStateFlow<List<PhotoEntry>>(emptyList())
    val entries: StateFlow<List<PhotoEntry>> = _entries.asStateFlow()

    val streak: StateFlow<Int> = entries.map { list ->
        computeStreak(list.map { java.time.LocalDate.ofEpochDay(it.dateEpochDay) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun load(projectId: Long) {
        viewModelScope.launch {
            _project.value = repo.getProject(projectId)
            repo.observeEntries(projectId).collect { _entries.value = it }
        }
    }

    fun uploadPhoto(context: android.content.Context, uri: Uri) {
        val proj = _project.value ?: return
        viewModelScope.launch {
            val date = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val exif = ExifInterface(input)
                        val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                        if (dateStr != null) {
                            // Exif format is "yyyy:MM:dd HH:mm:ss"
                            val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
                            java.time.LocalDateTime.parse(dateStr, formatter).toLocalDate()
                        } else {
                            LocalDate.now()
                        }
                    } ?: LocalDate.now()
                } catch (e: Exception) {
                    LocalDate.now()
                }
            }

            // Check if entry already exists for this date
            if (repo.getEntryForDate(proj.id, date) != null) {
                // For simplicity, we just don't overwrite if it exists. 
                // In a real app we might want to ask the user.
                return@launch
            }

            withContext(Dispatchers.IO) {
                val file = repo.copyFileToProject(proj, uri, context, date)
                if (file != null) {
                    repo.saveEntry(proj, date, file, null)
                }
            }
        }
    }

    private fun computeStreak(days: List<LocalDate>): Int {
        if (days.isEmpty()) return 0
        val set = days.toHashSet()
        var cursor = LocalDate.now()
        // If today isn't done yet, streak still counts up to yesterday
        if (!set.contains(cursor)) cursor = cursor.minusDays(1)
        var streak = 0
        while (set.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}

@Composable
private fun viewModel_(): ProjectHomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
