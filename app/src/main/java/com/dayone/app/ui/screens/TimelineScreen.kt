package com.dayone.app.ui.screens

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dayone.app.DayOneApp
import com.dayone.app.data.db.PhotoEntry
import com.dayone.app.ui.components.ConfirmDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    projectId: Long,
    initialEpochDay: Long? = null,
    onBack: () -> Unit,
    viewModel: TimelineViewModel = viewModel()
) {
    LaunchedEffect(projectId) { viewModel.load(projectId) }
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timeline") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No photos yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        val startIndex = remember(entries.size, initialEpochDay) {
            initialEpochDay
                ?.let { day -> entries.indexOfFirst { it.dateEpochDay == day }.takeIf { it >= 0 } }
                ?: (entries.size - 1).coerceAtLeast(0)
        }
        val pagerState = rememberPagerState(initialPage = startIndex) { entries.size }
        val listState = rememberLazyListState()
        var showDeleteDialog by remember { mutableStateOf(false) }
        var noteDraft by remember { mutableStateOf<String?>(null) }

        // Keep the thumbnail strip in step with whichever day is on screen.
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                listState.animateScrollToItem(page.coerceIn(0, (entries.size - 1).coerceAtLeast(0)))
            }
        }

        val index = pagerState.currentPage.coerceIn(0, entries.lastIndex)
        val current = entries[index]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val entry = entries[page]
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = entry.filePath,
                        contentDescription = "Photo from ${LocalDate.ofEpochDay(entry.dateEpochDay)}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            LocalDate.ofEpochDay(current.dateEpochDay)
                                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Day ${index + 1} of ${entries.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.toggleFavorite(current) }) {
                        Icon(
                            if (current.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favourite",
                            tint = if (current.favorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { noteDraft = current.note.orEmpty() }) {
                        Icon(Icons.Default.EditNote, contentDescription = "Add a note")
                    }
                    IconButton(onClick = {
                        viewModel.pinAsReference(current)
                        scope.launch {
                            snackbarHostState.showSnackbar("Pinned as the capture overlay reference")
                        }
                    }) {
                        Icon(Icons.Default.PushPin, contentDescription = "Pin as overlay reference")
                    }
                    IconButton(onClick = { sharePhoto(context, current) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete photo",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                current.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            note,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (entries.size > 1) {
                    Slider(
                        value = index.toFloat(),
                        onValueChange = { value ->
                            scope.launch { pagerState.scrollToPage(value.toInt().coerceIn(0, entries.lastIndex)) }
                        },
                        valueRange = 0f..entries.lastIndex.toFloat(),
                        steps = (entries.size - 2).coerceAtLeast(0)
                    )
                }
            }

            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(entries.size, key = { entries[it].id }) { i ->
                    val entry = entries[i]
                    val selected = i == index
                    AsyncImage(
                        model = entry.filePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (selected) 2.5.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { scope.launch { pagerState.animateScrollToPage(i) } }
                    )
                }
            }
        }

        if (showDeleteDialog) {
            ConfirmDialog(
                title = "Delete this photo?",
                message = "The photo for ${LocalDate.ofEpochDay(current.dateEpochDay)} will be removed from the project and from disk.",
                confirmLabel = "Delete",
                destructive = true,
                onConfirm = { viewModel.deleteEntry(current) },
                onDismiss = { showDeleteDialog = false }
            )
        }

        noteDraft?.let { draft ->
            var text by remember(current.id) { mutableStateOf(draft) }
            AlertDialog(
                onDismissRequest = { noteDraft = null },
                title = { Text("Note for this day") },
                text = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("What happened today?") },
                        minLines = 2
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setNote(current, text)
                        noteDraft = null
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { noteDraft = null }) { Text("Cancel") } }
            )
        }
    }
}

private fun sharePhoto(context: android.content.Context, entry: PhotoEntry) {
    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(entry.filePath)
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share photo"))
    }
}

class TimelineViewModel(app: Application) : AndroidViewModel(app) {

    private val repo get() = getApplication<DayOneApp>().repository
    private val settings get() = getApplication<DayOneApp>().settingsRepository

    private val _entries = MutableStateFlow<List<PhotoEntry>>(emptyList())
    val entries: StateFlow<List<PhotoEntry>> = _entries.asStateFlow()

    private var loadedProjectId = -1L

    fun load(projectId: Long) {
        if (loadedProjectId == projectId) return
        loadedProjectId = projectId
        viewModelScope.launch {
            repo.observeEntries(projectId).collect { _entries.value = it }
        }
    }

    fun deleteEntry(entry: PhotoEntry) {
        viewModelScope.launch { repo.deleteEntry(entry) }
    }

    fun toggleFavorite(entry: PhotoEntry) {
        viewModelScope.launch { repo.setFavorite(entry, !entry.favorite) }
    }

    fun setNote(entry: PhotoEntry, note: String) {
        viewModelScope.launch { repo.setNote(entry, note) }
    }

    /** Makes this day the photo the capture overlay lines up against. */
    fun pinAsReference(entry: PhotoEntry) {
        settings.setPinnedReferenceDay(entry.projectId, entry.dateEpochDay)
        settings.update { it.copy(ghostReference = com.dayone.app.data.GhostReference.PINNED) }
    }
}
