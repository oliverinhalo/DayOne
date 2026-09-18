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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.dayone.app.data.AppSettings
import com.dayone.app.data.db.Project
import com.dayone.app.data.db.Weekdays
import com.dayone.app.notify.ReminderScheduler
import com.dayone.app.ui.components.SectionHeader
import com.dayone.app.ui.components.SettingRow
import com.dayone.app.ui.components.SettingsCard
import com.dayone.app.ui.components.TimePickerDialog
import com.dayone.app.ui.components.WeekdayPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 18 years ago today - a sane starting point for a birthday picker. */
internal fun defaultBirthdayMillis(): Long =
    LocalDate.now().minusYears(18).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private val palette = listOf(
    0xFF00E5A0.toInt(), 0xFF4FC3F7.toInt(), 0xFFFFB74D.toInt(),
    0xFFF06292.toInt(), 0xFFBA68C8.toInt(), 0xFFFFD54F.toInt(),
    0xFF81C784.toInt(), 0xFFFF8A65.toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: CreateProjectViewModel = viewModel()
) {
    val context = LocalContext.current
    val defaults by viewModel.settings.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var color by remember { mutableIntStateOf(palette.first()) }
    var reminderMinute by remember(defaults.defaultReminderMinuteOfDay) {
        mutableIntStateOf(defaults.defaultReminderMinuteOfDay)
    }
    var daysMask by remember(defaults.defaultActiveDaysMask) {
        mutableIntStateOf(defaults.defaultActiveDaysMask)
    }
    var birthDateMillis by remember { mutableStateOf<Long?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showBirthdayPicker by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New instance") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
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
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (e.g. Me, Group photo, Mum)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SectionHeader("Accent colour")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.forEach { argb ->
                    val selected = argb == color
                    Box(
                        modifier = Modifier
                            .size(if (selected) 38.dp else 32.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { color = argb }
                    )
                }
            }

            SectionHeader("Schedule")
            SettingsCard {
                SettingRow(
                    title = "Reminder time",
                    subtitle = "%02d:%02d".format(reminderMinute / 60, reminderMinute % 60),
                    icon = Icons.Default.Alarm,
                    onClick = { showTimePicker = true }
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Which days?", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        Weekdays.label(daysMask),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    WeekdayPicker(
                        mask = daysMask,
                        onMaskChange = { if (Weekdays.count(it) > 0) daysMask = it }
                    )
                }
                SettingRow(
                    title = "Birthday (optional)",
                    subtitle = birthDateMillis
                        ?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                .format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                        }
                        ?: "Lets videos show an age counter",
                    icon = Icons.Default.Cake,
                    onClick = { showBirthdayPicker = true }
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    if (creating) return@Button
                    creating = true
                    viewModel.create(
                        context = context,
                        name = name.trim().ifBlank { "Untitled" },
                        colorArgb = color,
                        reminderMinuteOfDay = reminderMinute,
                        activeDaysMask = daysMask,
                        birthDateMillis = birthDateMillis,
                        onCreated = onDone
                    )
                },
                enabled = name.isNotBlank() && !creating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Create") }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            title = "Reminder time",
            initialMinuteOfDay = reminderMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { minute ->
                reminderMinute = minute
                showTimePicker = false
            }
        )
    }

    if (showBirthdayPicker) {
        val state = rememberDatePickerState(
            // Most people setting up an age counter are adults, so start the picker
            // somewhere useful instead of on today's date.
            initialSelectedDateMillis = birthDateMillis ?: defaultBirthdayMillis(),
            yearRange = 1900..LocalDate.now().year
        )
        DatePickerDialog(
            onDismissRequest = { showBirthdayPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    birthDateMillis = state.selectedDateMillis
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
}

class CreateProjectViewModel(app: Application) : AndroidViewModel(app) {

    private val repo get() = getApplication<DayOneApp>().repository
    private val settingsRepo get() = getApplication<DayOneApp>().settingsRepository

    val settings: StateFlow<AppSettings> get() = settingsRepo.settings

    fun create(
        context: Context,
        name: String,
        colorArgb: Int,
        reminderMinuteOfDay: Int,
        activeDaysMask: Int,
        birthDateMillis: Long?,
        onCreated: () -> Unit
    ) {
        viewModelScope.launch {
            val defaults = settingsRepo.current
            val project: Project = repo.createProject(
                name = name,
                birthDateMillis = birthDateMillis,
                reminderMinuteOfDay = reminderMinuteOfDay,
                colorArgb = colorArgb,
                activeDaysMask = activeDaysMask,
                nagIntervalMinutes = defaults.defaultNagIntervalMinutes,
                nagUntilMinuteOfDay = defaults.defaultNagUntilMinuteOfDay
            )
            withContext(Dispatchers.IO) { ReminderScheduler.schedule(context, project) }
            onCreated()
        }
    }
}
