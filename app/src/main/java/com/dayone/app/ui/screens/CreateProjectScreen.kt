package com.dayone.app.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dayone.app.DayOneApp
import com.dayone.app.notify.ReminderScheduler
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val palette = listOf(
    0xFF00E5A0.toInt(), 0xFF4FC3F7.toInt(), 0xFFFFB74D.toInt(),
    0xFFF06292.toInt(), 0xFFBA68C8.toInt(), 0xFFFFF176.toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(onDone: () -> Unit, viewModel: CreateProjectViewModel = viewModel_()) {
    var name by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }
    var trackAge by remember { mutableStateOf(false) }
    var birthYear by remember { mutableStateOf(2000) }
    var birthMonth by remember { mutableStateOf(1) }
    var birthDay by remember { mutableStateOf(1) }
    var color by remember { mutableStateOf(palette.first()) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text("New instance") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (e.g. Me, Group photo, Mum)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Text("Accent color", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(palette) { c ->
                    val selected = c == color
                    Box(
                        modifier = Modifier
                            .size(if (selected) 44.dp else 36.dp)
                            .clip(CircleShape)
                            .background_(Color(c))
                            .clickable_ { color = c }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Daily reminder time", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberStepper(value = hour, range = 0..23, onChange = { hour = it }, label = "Hour")
                Spacer(Modifier.width(16.dp))
                NumberStepper(value = minute, range = 0..59, step = 5, onChange = { minute = it }, label = "Min")
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = trackAge, onCheckedChange = { trackAge = it })
                Text("Show age in video overlay (enter birthdate)")
            }
            if (trackAge) {
                Spacer(Modifier.height(8.dp))
                Row {
                    NumberStepper(value = birthDay, range = 1..31, onChange = { birthDay = it }, label = "Day")
                    Spacer(Modifier.width(8.dp))
                    NumberStepper(value = birthMonth, range = 1..12, onChange = { birthMonth = it }, label = "Month")
                    Spacer(Modifier.width(8.dp))
                    NumberStepper(value = birthYear, range = 1900..2026, onChange = { birthYear = it }, label = "Year")
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    scope.launch {
                        val birthMillis = if (trackAge) {
                            runCatching {
                                LocalDate.of(birthYear, birthMonth, birthDay)
                                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            }.getOrNull()
                        } else null

                        val project = viewModel.create(
                            name = name.ifBlank { "Untitled" },
                            birthDateMillis = birthMillis,
                            reminderMinuteOfDay = hour * 60 + minute,
                            colorArgb = color
                        )
                        ReminderScheduler.scheduleDaily(
                            viewModel.getApplication(), project.id, project.reminderMinuteOfDay
                        )
                        onDone()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create") }
        }
    }
}

@Composable
private fun NumberStepper(value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange((value - step).coerceIn(range)) }) { Text("-") }
            Text(value.toString().padStart(2, '0'), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { onChange((value + step).coerceIn(range)) }) { Text("+") }
        }
    }
}

class CreateProjectViewModel(app: Application) : AndroidViewModel(app) {
    private val repo get() = (getApplication<DayOneApp>()).repository

    suspend fun create(
        name: String,
        birthDateMillis: Long?,
        reminderMinuteOfDay: Int,
        colorArgb: Int
    ) = repo.createProject(name, birthDateMillis, reminderMinuteOfDay, colorArgb)
}

@Composable
private fun viewModel_(): CreateProjectViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

// Small local aliases to keep Modifier chain readable above without extra top-level imports clutter
private fun Modifier.background_(color: Color) = this.then(Modifier.background(color = color))
private fun Modifier.clickable_(onClick: () -> Unit) =
    this.then(Modifier.clickable(onClick = onClick))
