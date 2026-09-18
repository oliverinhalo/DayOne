package com.dayone.app.ui.screens

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dayone.app.data.db.Project

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onOpenProject: (Long) -> Unit,
    onCreateProject: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ProjectListViewModel = viewModel_()
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val statuses by viewModel.todayStatus.collectAsStateWithLifecycle()

    val alarmManager = remember { context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager }
    var needsExactAlarmPermission by remember {
        mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms())
    }
    // Re-check whenever the screen resumes (e.g. after returning from Settings)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                needsExactAlarmPermission = !alarmManager.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            ExtendedFloatingActionButton(onClick = onCreateProject, icon = {
                Icon(Icons.Default.Add, contentDescription = null)
            }, text = { Text("New instance") })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (needsExactAlarmPermission) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).clickable {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Tap to allow exact alarms - without this, daily reminders may be delayed or skipped.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (projects.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No projects yet.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Create one for yourself, a group photo, or anyone else you want to track daily - each gets its own folder, streak and reminder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            doneToday = statuses[project.id] == true,
                            onClick = { onOpenProject(project.id) },
                            onRename = { newName -> viewModel.renameProject(project, newName) },
                            onDelete = { viewModel.deleteProject(project) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    doneToday: Boolean,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(project.colorArgb))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (doneToday) "Today's photo done" else "Today's photo not taken yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (doneToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (doneToday) Color(0xFF00E5A0) else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(8.dp))
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        },
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
            title = { Text("Rename Project") },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("New name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(name)
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Project?") },
            text = { Text("This will permanently delete '${project.name}' and all its photos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun viewModel_(): ProjectListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
