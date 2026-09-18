package com.dayone.app.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayone.app.BuildConfig
import com.dayone.app.DayOneApp
import com.dayone.app.data.AppSettings
import com.dayone.app.data.BackupManager
import com.dayone.app.data.GhostMode
import com.dayone.app.data.GhostReference
import com.dayone.app.data.GridMode
import com.dayone.app.data.MediaStoreSaver
import com.dayone.app.data.ThemeMode
import com.dayone.app.data.db.Weekdays
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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val accentPalette = listOf(
    0xFF00E5A0.toInt(), 0xFF4FC3F7.toInt(), 0xFFFFB74D.toInt(),
    0xFFF06292.toInt(), 0xFFBA68C8.toInt(), 0xFFFFD54F.toInt(),
    0xFF81C784.toInt(), 0xFFFF8A65.toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReplayIntro: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenLicences: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val storageLabel by viewModel.storageLabel.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showDefaultTimePicker by remember { mutableStateOf(false) }
    var showRestoreWarning by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) showRestoreWarning = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 16.dp)
        ) {
            // ---------------------------------------------------------- appearance
            SectionHeader("Appearance")
            SettingsCard {
                ChoiceRow(
                    title = "Theme",
                    options = ThemeMode.entries.toList(),
                    selected = settings.themeMode,
                    label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onSelect = { mode -> viewModel.update { it.copy(themeMode = mode) } }
                )
                SwitchRow(
                    title = "Pure black dark mode",
                    subtitle = "Saves battery on OLED screens",
                    checked = settings.amoledDark,
                    icon = Icons.Default.Brightness6
                ) { value -> viewModel.update { it.copy(amoledDark = value) } }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow(
                        title = "Use system colours",
                        subtitle = "Match your wallpaper (Material You)",
                        checked = settings.dynamicColor
                    ) { value -> viewModel.update { it.copy(dynamicColor = value) } }
                }
                if (!settings.dynamicColor) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text("Accent colour", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            accentPalette.forEach { argb ->
                                val isSelected = settings.accentArgb == argb
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 36.dp else 30.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb))
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.update { s -> s.copy(accentArgb = argb) } }
                                )
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------- capture
            SectionHeader("Camera & overlay")
            SettingsCard {
                ChoiceRow(
                    title = "Overlay style",
                    options = GhostMode.entries.toList(),
                    selected = settings.ghostMode,
                    label = { it.label },
                    onSelect = { mode -> viewModel.update { it.copy(ghostMode = mode) } }
                )
                ChoiceRow(
                    title = "Overlay reference",
                    options = GhostReference.entries.toList(),
                    selected = settings.ghostReference,
                    label = { it.label },
                    onSelect = { reference -> viewModel.update { it.copy(ghostReference = reference) } }
                )
                ChoiceRow(
                    title = "Grid",
                    options = GridMode.entries.toList(),
                    selected = settings.gridMode,
                    label = { it.label },
                    onSelect = { grid -> viewModel.update { it.copy(gridMode = grid) } }
                )
                ChoiceRow(
                    title = "Self-timer",
                    options = listOf(0, 3, 5, 10),
                    selected = settings.countdownSeconds,
                    label = { if (it == 0) "Off" else "${it}s" },
                    onSelect = { seconds -> viewModel.update { it.copy(countdownSeconds = seconds) } }
                )
                SwitchRow(
                    title = "Show the crop frame",
                    subtitle = "Dims everything outside the square that gets saved",
                    checked = settings.showFrameBox,
                    icon = Icons.Default.CameraAlt
                ) { value -> viewModel.update { it.copy(showFrameBox = value) } }
                SwitchRow(
                    title = "Head guide oval",
                    checked = settings.showFaceGuide
                ) { value -> viewModel.update { it.copy(showFaceGuide = value) } }
                if (settings.showFaceGuide) {
                    SliderRow(
                        title = "Guide oval size",
                        valueLabel = "${(settings.faceGuideScale * 100).toInt()}%",
                        value = settings.faceGuideScale,
                        range = 0.4f..2f,
                        onValueChange = { value -> viewModel.update { it.copy(faceGuideScale = value) } }
                    )
                }
                SliderRow(
                    title = "Overlay size",
                    valueLabel = "${(settings.ghostScale * 100).toInt()}%",
                    value = settings.ghostScale,
                    range = 0.4f..3f,
                    onValueChange = { value -> viewModel.update { it.copy(ghostScale = value) } }
                )
                SwitchRow(
                    title = "Face light",
                    subtitle = "Lights your face with the screen itself in dim rooms",
                    checked = settings.faceLight,
                    icon = Icons.Default.WbSunny
                ) { value -> viewModel.update { it.copy(faceLight = value) } }
                if (settings.faceLight) {
                    SliderRow(
                        title = "Face light brightness",
                        valueLabel = "${(settings.faceLightIntensity * 100).toInt()}%",
                        value = settings.faceLightIntensity,
                        range = 0.2f..1f,
                        onValueChange = { value -> viewModel.update { it.copy(faceLightIntensity = value) } }
                    )
                }
                SwitchRow(
                    title = "Review before saving",
                    subtitle = "Check the shot and retake it before it counts for the day",
                    checked = settings.reviewBeforeSave
                ) { value -> viewModel.update { it.copy(reviewBeforeSave = value) } }
                SwitchRow(
                    title = "Mirror selfies",
                    subtitle = "Save front-camera photos the way you saw them in the viewfinder",
                    checked = settings.mirrorFrontCamera
                ) { value -> viewModel.update { it.copy(mirrorFrontCamera = value) } }
                SwitchRow(
                    title = "Keep the screen on",
                    checked = settings.keepScreenOn,
                    icon = Icons.Default.Timer
                ) { value -> viewModel.update { it.copy(keepScreenOn = value) } }
                SwitchRow(
                    title = "Haptic feedback",
                    checked = settings.haptics
                ) { value -> viewModel.update { it.copy(haptics = value) } }
                SwitchRow(
                    title = "Also save photos to Gallery",
                    subtitle = "Copies each photo to Pictures/DayOne - they then survive an uninstall",
                    checked = settings.saveCopyToGallery
                ) { value -> viewModel.update { it.copy(saveCopyToGallery = value) } }
            }

            // ---------------------------------------------------------- reminders
            SectionHeader("Reminder defaults for new projects")
            SettingsCard {
                SettingRow(
                    title = "Reminder time",
                    subtitle = formatMinuteOfDay(settings.defaultReminderMinuteOfDay),
                    icon = Icons.Default.Alarm,
                    onClick = { showDefaultTimePicker = true }
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("Days", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        Weekdays.label(settings.defaultActiveDaysMask),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    WeekdayPicker(
                        mask = settings.defaultActiveDaysMask,
                        onMaskChange = { mask -> viewModel.update { it.copy(defaultActiveDaysMask = mask) } }
                    )
                }
                ChoiceRow(
                    title = "Repeat every",
                    options = listOf(15, 30, 45, 60, 120),
                    selected = settings.defaultNagIntervalMinutes,
                    label = { "$it min" },
                    onSelect = { minutes -> viewModel.update { it.copy(defaultNagIntervalMinutes = minutes) } }
                )
                SettingRow(
                    title = "Notification settings",
                    subtitle = "Sound, vibration and importance for DayOne reminders",
                    icon = Icons.Default.Notifications,
                    onClick = { openNotificationSettings(context) }
                )
                SettingRow(
                    title = "Battery optimisation",
                    subtitle = "Set DayOne to \"Unrestricted\" so reminders aren't delayed",
                    icon = Icons.Default.BatterySaver,
                    onClick = { openBatterySettings(context) }
                )
            }

            // ---------------------------------------------------------- backup
            SectionHeader("Backup & data")
            SettingsCard {
                SettingRow(
                    title = "Export a backup",
                    subtitle = "One .zip with every project, photo and note, saved to Documents/DayOne",
                    icon = Icons.Default.Backup,
                    enabled = !busy,
                    onClick = { viewModel.exportBackup(context) },
                    trailing = {
                        if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                )
                SettingRow(
                    title = "Restore from a backup",
                    subtitle = "Merges a .zip back in - existing days are kept, missing ones are filled",
                    icon = Icons.Default.RestorePage,
                    enabled = !busy,
                    onClick = { restoreLauncher.launch("*/*") }
                )
                SettingRow(
                    title = "Storage used",
                    subtitle = storageLabel,
                    icon = Icons.Default.Info
                )
            }

            // ---------------------------------------------------------- about
            SectionHeader("About")
            SettingsCard {
                SettingRow(
                    title = "DayOne ${BuildConfig.VERSION_NAME}",
                    subtitle = "Fully offline. No internet permission, no accounts, no tracking."
                )
                SettingRow(
                    title = "Where photos live",
                    subtitle = "Android/data/${context.packageName}/files/DayOne"
                )
                SettingRow(
                    title = "Show the intro again",
                    subtitle = "Replay the welcome tour and permission prompts",
                    icon = Icons.Default.Replay,
                    onClick = onReplayIntro
                )
                SettingRow(
                    title = "Privacy policy",
                    subtitle = "What is stored, and why nothing leaves the phone",
                    icon = Icons.Default.Policy,
                    onClick = onOpenPrivacy
                )
                SettingRow(
                    title = "Terms of use",
                    icon = Icons.Default.Gavel,
                    onClick = onOpenTerms
                )
                SettingRow(
                    title = "Open source licences",
                    subtitle = "AndroidX, Jetpack Compose, CameraX, Room, Coil, ML Kit",
                    icon = Icons.Default.Code,
                    onClick = onOpenLicences
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showDefaultTimePicker) {
        TimePickerDialog(
            title = "Default reminder time",
            initialMinuteOfDay = settings.defaultReminderMinuteOfDay,
            onDismiss = { showDefaultTimePicker = false },
            onConfirm = { minute ->
                viewModel.update { it.copy(defaultReminderMinuteOfDay = minute) }
                showDefaultTimePicker = false
            }
        )
    }

    showRestoreWarning?.let { uri ->
        ConfirmDialog(
            title = "Restore this backup?",
            message = "Projects are matched by name, and any day that already has a photo is left " +
                "untouched. Nothing currently in the app is deleted.",
            confirmLabel = "Restore",
            onConfirm = { viewModel.restoreBackup(context, uri) },
            onDismiss = { showRestoreWarning = null }
        )
    }
}

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    return "%02d:%02d".format(hour, minute)
}

private fun openNotificationSettings(context: Context) {
    runCatching {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
        }
        context.startActivity(intent)
    }
}

private fun openBatterySettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
        )
    }
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo get() = getApplication<DayOneApp>().settingsRepository
    private val repo get() = getApplication<DayOneApp>().repository

    val settings: StateFlow<AppSettings> get() = settingsRepo.settings

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _messages = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    private val _storageLabel = MutableStateFlow("Calculating...")
    val storageLabel: StateFlow<String> = _storageLabel.asStateFlow()

    init {
        refreshStorage()
    }

    private fun refreshStorage() {
        viewModelScope.launch {
            // Walking the photo directory is disk work, so it never runs during composition.
            val bytes = withContext(Dispatchers.IO) {
                runCatching { repo.rootDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } }
                    .getOrDefault(0L)
            }
            _storageLabel.value = "%.1f MB of photos and videos".format(bytes / 1_048_576f)
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) = settingsRepo.update(transform)

    fun completeOnboarding() = settingsRepo.update {
        it.copy(onboardingComplete = true, acceptedTermsVersion = Legal.TERMS_VERSION)
    }

    fun exportBackup(context: Context) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
            val name = "dayone-backup-$stamp.zip"
            val staged = File(context.cacheDir, name)
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val backup = BackupManager.export(repo, staged)
                    val saved = MediaStoreSaver.saveDocument(context, staged, name, "application/zip")
                    staged.delete()
                    backup to saved
                }
            }
            _busy.value = false

            result.onSuccess { (backup, saved) ->
                _messages.tryEmit(
                    if (saved == null) "Backup failed to save to shared storage"
                    else "Backed up ${backup.photos} photo(s) to ${saved.displayPath}"
                )
            }.onFailure { error ->
                _messages.tryEmit("Backup failed: ${error.message}")
            }
        }
    }

    fun restoreBackup(context: Context, uri: Uri) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { BackupManager.restore(context, repo, uri) }
            }
            _busy.value = false

            refreshStorage()
            result.onSuccess { restore ->
                _messages.tryEmit(
                    restore.error
                        ?: "Restored ${restore.photosAdded} photo(s) into ${restore.projectsCreated + restore.projectsMerged} project(s)"
                )
            }.onFailure { error ->
                _messages.tryEmit("Restore failed: ${error.message}")
            }
        }
    }
}
