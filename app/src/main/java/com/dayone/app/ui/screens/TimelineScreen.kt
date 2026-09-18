package com.dayone.app.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.dayone.app.DayOneApp
import com.dayone.app.data.db.PhotoEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(projectId: Long, onBack: () -> Unit, viewModel: TimelineViewModel = viewModel_()) {
    LaunchedEffect(projectId) { viewModel.load(projectId) }
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    var index by remember(entries.size) { mutableStateOf((entries.size - 1).coerceAtLeast(0)) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timeline") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No photos yet")
            }
            return@Scaffold
        }

        val current = entries[index]

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AsyncImage(
                model = current.filePath,
                contentDescription = "Day photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
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
                    IconButton(onClick = { viewModel.deleteEntry(current) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete photo",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Slider(
                    value = index.toFloat(),
                    onValueChange = {
                        index = it.toInt()
                        scope.launch { listState.animateScrollToItem(index) }
                    },
                    valueRange = 0f..(entries.size - 1).coerceAtLeast(0).toFloat(),
                    steps = (entries.size - 2).coerceAtLeast(0)
                )
            }

            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(entries.size) { i ->
                    val entry = entries[i]
                    val selected = i == index
                    AsyncImage(
                        model = entry.filePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .then(Modifier)
                            .clickableIndex { index = i }
                    )
                }
            }
        }
    }
}

// small helper to avoid importing clickable separately at call site with lambdas capturing i
private fun Modifier.clickableIndex(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

class TimelineViewModel(app: Application) : AndroidViewModel(app) {
    private val repo get() = (getApplication<DayOneApp>()).repository

    private val _entries = MutableStateFlow<List<PhotoEntry>>(emptyList())
    val entries: StateFlow<List<PhotoEntry>> = _entries.asStateFlow()

    fun load(projectId: Long) {
        viewModelScope.launch {
            repo.observeEntries(projectId).collect { _entries.value = it }
        }
    }

    fun deleteEntry(entry: PhotoEntry) {
        viewModelScope.launch {
            repo.deleteEntry(entry)
        }
    }
}

@Composable
private fun viewModel_(): TimelineViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
