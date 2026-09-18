package com.dayone.app.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dayone.app.ui.components.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onOpenProject: (Long) -> Unit,
    onCreateProject: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuickCapture: (Long) -> Unit,
    viewModel: ProjectListViewModel = viewModel()
) {
    val context = LocalContext.current
    val active by viewModel.activeCards.collectAsStateWithLifecycle()
    val archived by viewModel.archivedCards.collectAsStateWithLifecycle()

    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    var needsExactAlarmPermission by remember {
        mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms())
    }

    // Re-check whenever the screen resumes (e.g. after returning from system Settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    needsExactAlarmPermission = !alarmManager.canScheduleExactAlarms()
                }
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val dueToday = active.count { !it.stats.doneToday && !it.stats.restDayToday && !it.skippedToday }
    val doneToday = active.count { it.stats.doneToday }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DayOne") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateProject,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New instance") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (needsExactAlarmPermission) {
                item {
                    WarningBanner(
                        text = "Tap to allow exact alarms - without this, daily reminders may be delayed or skipped.",
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            }
                        }
                    )
                }
            }

            if (active.isNotEmpty()) {
                item {
                    TodaySummary(done = doneToday, due = dueToday, total = active.size)
                }
            }

            if (active.isEmpty() && archived.isEmpty()) {
                item { EmptyState() }
            }

            items(active, key = { it.project.id }) { card ->
                ProjectCard(
                    card = card,
                    onClick = { onOpenProject(card.project.id) },
                    onCapture = { onQuickCapture(card.project.id) },
                    onRename = { viewModel.renameProject(card.project, it) },
                    onSkipToday = { viewModel.skipToday(card.project) },
                    onArchive = { viewModel.setArchived(card.project, true) },
                    onDelete = { viewModel.deleteProject(card.project) }
                )
            }

            if (archived.isNotEmpty()) {
                item {
                    Text(
                        "Archived",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, start = 4.dp)
                    )
                }
                items(archived, key = { it.project.id }) { card ->
                    ProjectCard(
                        card = card,
                        archivedStyle = true,
                        onClick = { onOpenProject(card.project.id) },
                        onCapture = { onQuickCapture(card.project.id) },
                        onRename = { viewModel.renameProject(card.project, it) },
                        onSkipToday = { },
                        onArchive = { viewModel.setArchived(card.project, false) },
                        onDelete = { viewModel.deleteProject(card.project) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TodaySummary(done: Int, due: Int, total: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                when {
                    due == 0 && done > 0 -> "All caught up for today"
                    due == 0 -> "Nothing scheduled today"
                    done == 0 -> "$due to shoot today"
                    else -> "$done done, $due still to shoot"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else done.toFloat() / total },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun WarningBanner(text: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No projects yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create one for yourself, a group photo, or anyone else you want to track - each gets its own folder, streak and reminder schedule.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectCard(
    card: ProjectCardState,
    archivedStyle: Boolean = false,
    onClick: () -> Unit,
    onCapture: () -> Unit,
    onRename: (String) -> Unit,
    onSkipToday: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val project = card.project
    val stats = card.stats
    val accent = Color(project.colorArgb)
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Latest photo doubles as the project's avatar - far more recognisable than a dot.
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = if (archivedStyle) 0.15f else 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                if (card.latestPhotoPath != null) {
                    AsyncImage(
                        model = card.latestPhotoPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        project.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = accent
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (stats.current > 0) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "${stats.current}",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        statusLine(card),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            when {
                stats.doneToday -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Done today",
                    tint = accent
                )
                stats.restDayToday || card.skippedToday -> Icon(
                    if (card.skippedToday) Icons.Default.SkipNext else Icons.Default.Weekend,
                    contentDescription = "Not scheduled today",
                    tint = MaterialTheme.colorScheme.outline
                )
                else -> FilledTonalIconButton(onClick = onCapture) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Take today's photo")
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { showMenu = false; showRenameDialog = true },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    if (!archivedStyle && !stats.doneToday && !stats.restDayToday && !card.skippedToday) {
                        DropdownMenuItem(
                            text = { Text("Skip today") },
                            onClick = { showMenu = false; onSkipToday() },
                            leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (archivedStyle) "Unarchive" else "Archive") },
                        onClick = { showMenu = false; onArchive() },
                        leadingIcon = {
                            Icon(
                                if (archivedStyle) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showMenu = false; showDeleteDialog = true },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        var name by remember { mutableStateOf(project.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename project") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = { onRename(name); showRenameDialog = false }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete project?",
            message = "This permanently deletes \"${project.name}\" and all ${stats.total} of its photos. " +
                "Export a backup first if you might want them later.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = onDelete,
            onDismiss = { showDeleteDialog = false }
        )
    }
}

private fun statusLine(card: ProjectCardState): String {
    val stats = card.stats
    return when {
        stats.doneToday -> "Today's photo done"
        card.skippedToday -> "Skipped today"
        stats.restDayToday -> "Rest day - not scheduled"
        stats.total == 0 -> "No photos yet - start today"
        else -> "Today's photo not taken yet"
    }
}
