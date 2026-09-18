package com.dayone.app.ui.screens

import android.app.Application
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayone.app.DayOneApp
import com.dayone.app.data.db.Project
import com.dayone.app.data.db.Weekdays
import com.dayone.app.notify.ReminderScheduler
import com.dayone.app.ui.components.ChoiceRow
import com.dayone.app.ui.components.ConfirmDialog
import com.dayone.app.ui.components.SectionHeader
import com.dayone.app.ui.components.SettingRow
import com.dayone.app.ui.components.SettingsCard
import com.dayone.app.ui.components.SliderRow
import com.dayone.app.ui.components.SwitchRow
import com.dayone.app.ui.components.TimePickerDialog
import com.dayone.app.ui.components.WeekdayPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val projectPalette = listOf(
    0xFF00E5A0.toInt(), 0xFF4FC3F7.toInt(), 0xFFFFB74D.toInt(),
    0xFFF06292.toInt(), 0xFFBA68C8.toInt(), 0xFFFFD54F.toInt(),
    0xFF81C784.toInt(), 0xFFFF8A65.toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSettingsScreen(
    projectId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: ProjectSettingsViewModel = viewModel()
) {
    LaunchedEffect(projectId) { viewModel.load(projectId) }
    val context = LocalContext.current
    val project by viewModel.project.collectAsStateWithLifecycle()
    val storage by viewModel.storageLabel.collectAsStateWithLifecycle()

    var showTimePicker by remember { mutableStateOf(false) }
    var showSecondTimePicker by remember { mutableStateOf(false) }
    var showNagUntilPicker by remember { mutableStateOf(false) }
    var showBirthdayPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val current = project ?: return

    // The name field edits a local draft and saves shortly after typing stops, rather
    // than writing to the database on every keystroke.
    var nameDraft by remember(current.id) { mutableStateOf(current.name) }
    LaunchedEffect(nameDraft) {
        if (nameDraft != current.name && nameDraft.isNotBlank()) {
            kotlinx.coroutines.delay(400)
            viewModel.save(context, current.copy(name = nameDraft.trim()))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project settings") },
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
            SectionHeader("Project")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Accent colour", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        projectPalette.forEach { argb ->
                            val selected = current.colorArgb == argb
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 36.dp else 30.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.save(context, current.copy(colorArgb = argb)) }
                            )
                        }
                    }
                }
                SettingRow(
                    title = "Birthday",
                    subtitle = current.birthDateMillis
                        ?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                .format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                        }
                        ?: "Not set - needed for the age overlay in videos",
                    icon = Icons.Default.Cake,
                    onClick = { showBirthdayPicker = true }
                )
            }

            // ------------------------------------------------------------ schedule
            SectionHeader("Reminders")
            SettingsCard {
                SwitchRow(
                    title = "Daily reminder",
                    subtitle = if (current.reminderEnabled) "On" else "Off",
                    checked = current.reminderEnabled,
                    icon = Icons.Default.Notifications
                ) { value -> viewModel.save(context, current.copy(reminderEnabled = value)) }

                SettingRow(
                    title = "Reminder time",
                    subtitle = formatMinute(current.reminderMinuteOfDay),
                    icon = Icons.Default.Alarm,
                    enabled = current.reminderEnabled,
                    onClick = { showTimePicker = true }
                )

                SettingRow(
                    title = "Second reminder",
                    subtitle = current.secondReminderMinuteOfDay?.let { formatMinute(it) }
                        ?: "Off - add a later nudge if mornings don't work",
                    enabled = current.reminderEnabled,
                    onClick = { showSecondTimePicker = true },
                    trailing = {
                        if (current.secondReminderMinuteOfDay != null) {
                            TextButton(onClick = {
                                viewModel.save(context, current.copy(secondReminderMinuteOfDay = null))
                            }) { Text("Clear") }
                        }
                    }
                )

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Days", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        Weekdays.label(current.activeDaysMask) +
                            " - other days never remind you and never break the streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    WeekdayPicker(
                        mask = current.activeDaysMask,
                        onMaskChange = { mask ->
                            // Never let every day be switched off - that would silently
                            // disable the project without saying so.
                            if (Weekdays.count(mask) > 0) {
                                viewModel.save(context, current.copy(activeDaysMask = mask))
                            }
                        }
                    )
                }
            }

            SectionHeader("If you haven't taken it yet")
            SettingsCard {
                SwitchRow(
                    title = "Keep reminding me",
                    subtitle = "Repeat until the day's photo is taken",
                    checked = current.nagEnabled,
                    enabled = current.reminderEnabled,
                    icon = Icons.Default.Repeat
                ) { value -> viewModel.save(context, current.copy(nagEnabled = value)) }
                ChoiceRow(
                    title = "Repeat every",
                    options = listOf(15, 30, 45, 60, 120, 180),
                    selected = current.nagIntervalMinutes,
                    label = { if (it >= 60) "${it / 60}h" else "${it}m" },
                    onSelect = { minutes -> viewModel.save(context, current.copy(nagIntervalMinutes = minutes)) }
                )
                SettingRow(
                    title = "Stop reminding at",
                    subtitle = formatMinute(current.nagUntilMinuteOfDay),
                    enabled = current.nagEnabled && current.reminderEnabled,
                    onClick = { showNagUntilPicker = true }
                )
            }

            // ------------------------------------------------------------ framing
            SectionHeader("Framing")
            SettingsCard {
                SwitchRow(
                    title = "Auto-centre on your face",
                    subtitle = "Crops each photo so your face lands in the same place every day",
                    checked = current.autoCropFace
                ) { value -> viewModel.save(context, current.copy(autoCropFace = value)) }
                SliderRow(
                    title = "Crop tightness",
                    valueLabel = "${(current.headFraction * 100).toInt()}%",
                    value = current.headFraction,
                    range = 0.25f..0.7f,
                    steps = 8,
                    enabled = current.autoCropFace,
                    onValueChange = { value -> viewModel.saveLocal(current.copy(headFraction = value)) },
                    onValueChangeFinished = { viewModel.commit(context) }
                )
                Text(
                    "Higher means your head fills more of the frame. The guide oval on the capture screen matches this.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // ------------------------------------------------------------ danger
            SectionHeader("Storage")
            SettingsCard {
                SettingRow(title = "Photos on disk", subtitle = storage)
                SettingRow(
                    title = "Folder",
                    subtitle = "DayOne/${current.folderName}"
                )
                SettingRow(
                    title = "Delete this project",
                    subtitle = "Removes every photo in it - this cannot be undone",
                    icon = Icons.Default.Delete,
                    onClick = { showDeleteDialog = true }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            title = "Reminder time",
            initialMinuteOfDay = current.reminderMinuteOfDay,
            onDismiss = { showTimePicker = false },
            onConfirm = { minute ->
                viewModel.save(context, current.copy(reminderMinuteOfDay = minute))
                showTimePicker = false
            }
        )
    }

    if (showSecondTimePicker) {
        TimePickerDialog(
            title = "Second reminder",
            initialMinuteOfDay = current.secondReminderMinuteOfDay ?: 1140,
            onDismiss = { showSecondTimePicker = false },
            onConfirm = { minute ->
                viewModel.save(context, current.copy(secondReminderMinuteOfDay = minute))
                showSecondTimePicker = false
            }
        )
    }

    if (showNagUntilPicker) {
        TimePickerDialog(
            title = "Stop reminding at",
            initialMinuteOfDay = current.nagUntilMinuteOfDay,
            onDismiss = { showNagUntilPicker = false },
            onConfirm = { minute ->
                viewModel.save(context, current.copy(nagUntilMinuteOfDay = minute))
                showNagUntilPicker = false
            }
        )
    }

    if (showBirthdayPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = current.birthDateMillis ?: defaultBirthdayMillis(),
            yearRange = 1900..LocalDate.now().year
        )
        DatePickerDialog(
            onDismissRequest = { showBirthdayPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.save(context, current.copy(birthDateMillis = state.selectedDateMillis))
                    showBirthdayPicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showBirthdayPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete \"${current.name}\"?",
            message = "Every photo in this project is deleted from the phone. Export a backup first if you might want them back.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.delete(context, current)
                onDeleted()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

private fun formatMinute(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

class ProjectSettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo get() = getApplication<DayOneApp>().repository

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _storageLabel = MutableStateFlow("...")
    val storageLabel: StateFlow<String> = _storageLabel.asStateFlow()

    private var loadedProjectId = -1L

    fun load(projectId: Long) {
        if (loadedProjectId == projectId) return
        loadedProjectId = projectId
        viewModelScope.launch {
            _project.value = repo.getProject(projectId)
            val project = _project.value
            if (project != null) {
                val bytes = withContext(Dispatchers.IO) { repo.folderSizeBytes(project) }
                _storageLabel.value = "%.1f MB".format(bytes / 1_048_576f)
            }
        }
    }

    /** Updates the on-screen copy without hitting the database (used while dragging a slider). */
    fun saveLocal(project: Project) {
        _project.value = project
    }

    /** Persists whatever is currently on screen and re-arms alarms to match. */
    fun commit(context: Context) {
        val project = _project.value ?: return
        viewModelScope.launch {
            repo.updateProject(project)
            withContext(Dispatchers.IO) { ReminderScheduler.schedule(context, project) }
        }
    }

    fun save(context: Context, project: Project) {
        _project.value = project
        viewModelScope.launch {
            repo.updateProject(project)
            withContext(Dispatchers.IO) {
                if (project.reminderEnabled) ReminderScheduler.schedule(context, project)
                else ReminderScheduler.cancelAll(context, project.id)
            }
        }
    }

    fun delete(context: Context, project: Project) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { ReminderScheduler.cancelAll(context, project.id) }
            repo.deleteProject(project)
        }
    }
}
