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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.dayone.app.data.GalleryMigrator
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

/**
 * The settings sections, in the order they appear on the index.
 *
 * Settings used to be one long scroll where appearance, camera, reminders, video and
 * backup ran together. Splitting it into named sections means a setting can be found by
 * guessing a category rather than by reading the whole page, and each index row carries a
 * summary of its own state so most questions are answered without opening anything.
 */
enum class SettingsSection(val route: String, val title: String) {
    APPEARANCE("appearance", "Appearance"),
    CAMERA("camera", "Camera"),
    OVERLAY("overlay", "Overlay & guides"),
    REMINDERS("reminders", "Reminders"),
    VIDEO("video", "Video"),
    PHOTOS("photos", "Photos & storage"),
    BACKUP("backup", "Backup & restore"),
    ABOUT("about", "About");

    companion object {
        fun fromRoute(route: String?): SettingsSection =
            entries.firstOrNull { it.route == route } ?: APPEARANCE
    }
}

// ---------------------------------------------------------------------------- index

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSection: (SettingsSection) -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val storageLabel by viewModel.storageLabel.collectAsStateWithLifecycle()

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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionHeader("The app")
            SettingsCard {
                IndexRow(
                    SettingsSection.APPEARANCE, Icons.Default.Palette,
                    summary = appearanceSummary(settings), onOpenSection
                )
                IndexRow(
                    SettingsSection.CAMERA, Icons.Default.CameraAlt,
                    summary = cameraSummary(settings), onOpenSection
                )
                IndexRow(
                    SettingsSection.OVERLAY, Icons.Default.Layers,
                    summary = overlaySummary(settings), onOpenSection
                )
                IndexRow(
                    SettingsSection.REMINDERS, Icons.Default.Alarm,
                    summary = remindersSummary(settings), onOpenSection
                )
                IndexRow(
                    SettingsSection.VIDEO, Icons.Default.Movie,
                    summary = videoSummary(settings), onOpenSection
                )
            }

            SectionHeader("Your photos")
            SettingsCard {
                IndexRow(
                    SettingsSection.PHOTOS, Icons.Default.PhotoLibrary,
                    summary = storageLabel, onOpenSection
                )
                IndexRow(
                    SettingsSection.BACKUP, Icons.Default.Backup,
                    summary = "Export everything to one file, or restore it", onOpenSection
                )
            }

            SectionHeader("About")
            SettingsCard {
                IndexRow(
                    SettingsSection.ABOUT, Icons.Default.Info,
                    summary = "Version ${BuildConfig.VERSION_NAME}  •  privacy, terms, licences",
                    onOpenSection
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun IndexRow(
    section: SettingsSection,
    icon: ImageVector,
    summary: String,
    onOpenSection: (SettingsSection) -> Unit
) {
    SettingRow(
        title = section.title,
        subtitle = summary,
        icon = icon,
        onClick = { onOpenSection(section) }
    )
}

private fun appearanceSummary(s: AppSettings): String {
    val theme = when (s.themeMode) {
        ThemeMode.SYSTEM -> "Follows system"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
    val colour = if (s.dynamicColor) "wallpaper colours" else "custom accent"
    return "$theme  •  $colour" + if (s.amoledDark) "  •  pure black" else ""
}

private fun cameraSummary(s: AppSettings): String = buildList {
    add(if (s.useFrontCamera) "Front camera" else "Back camera")
    if (s.countdownSeconds > 0) add("${s.countdownSeconds}s timer")
    if (s.reviewBeforeSave) add("review before saving")
}.joinToString("  •  ")

private fun overlaySummary(s: AppSettings): String = buildList {
    add(if (s.ghostMode == GhostMode.OFF) "Overlay off" else "${s.ghostMode.label} at ${(s.ghostAlpha * 100).toInt()}%")
    if (s.gridMode != GridMode.NONE) add(s.gridMode.label.lowercase())
    if (s.faceLight) add("face light on")
}.joinToString("  •  ")

private fun remindersSummary(s: AppSettings): String =
    "New projects: ${formatMinuteOfDay(s.defaultReminderMinuteOfDay)}  •  ${Weekdays.label(s.defaultActiveDaysMask)}"

private fun videoSummary(s: AppSettings): String =
    "${s.videoResolution}p  •  ${s.videoFps}fps  •  ${s.videoMillisPerPhoto}ms per photo"

// ---------------------------------------------------------------------------- sections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSectionScreen(
    section: SettingsSection,
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
    val migration by viewModel.migration.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDefaultTimePicker by remember { mutableStateOf(false) }
    var showRestoreWarning by remember { mutableStateOf<Uri?>(null) }
    var showMigrateConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) showRestoreWarning = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(section.title) },
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
            Spacer(Modifier.height(8.dp))

            when (section) {
                SettingsSection.APPEARANCE -> AppearanceSection(settings, viewModel)
                SettingsSection.CAMERA -> CameraSection(settings, viewModel)
                SettingsSection.OVERLAY -> OverlaySection(settings, viewModel)
                SettingsSection.REMINDERS -> RemindersSection(
                    settings = settings,
                    viewModel = viewModel,
                    context = context,
                    onPickTime = { showDefaultTimePicker = true }
                )
                SettingsSection.VIDEO -> VideoSection(settings, viewModel)
                SettingsSection.PHOTOS -> PhotosSection(
                    settings = settings,
                    viewModel = viewModel,
                    context = context,
                    storageLabel = storageLabel,
                    migration = migration,
                    onMigrate = { showMigrateConfirm = true }
                )
                SettingsSection.BACKUP -> BackupSection(
                    busy = busy,
                    onExport = { viewModel.exportBackup(context) },
                    onRestore = { restoreLauncher.launch("*/*") }
                )
                SettingsSection.ABOUT -> AboutSection(
                    context = context,
                    onReplayIntro = onReplayIntro,
                    onOpenPrivacy = onOpenPrivacy,
                    onOpenTerms = onOpenTerms,
                    onOpenLicences = onOpenLicences
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
            message = "Projects are matched by name. Any day that already has a photo keeps the " +
                "photo it has, and nothing already in the app is deleted.",
            confirmLabel = "Restore",
            onConfirm = { viewModel.restoreBackup(context, uri) },
            onDismiss = { showRestoreWarning = null }
        )
    }

    if (showMigrateConfirm) {
        ConfirmDialog(
            title = "Copy photos to your gallery?",
            message = "Every photo in every project is copied to Pictures/DayOne, one folder per " +
                "project. Photos already there are skipped, so this is safe to run again. The " +
                "originals stay exactly where they are.",
            confirmLabel = "Copy",
            onConfirm = { viewModel.migrateToGallery(context) },
            onDismiss = { showMigrateConfirm = false }
        )
    }
}

@Composable
private fun AppearanceSection(settings: AppSettings, viewModel: SettingsViewModel) {
    SettingsCard {
        ChoiceRow(
            title = "Theme",
            options = ThemeMode.entries.toList(),
            selected = settings.themeMode,
            label = {
                when (it) {
                    ThemeMode.SYSTEM -> "Follow system"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                }
            },
            onSelect = { mode -> viewModel.update { it.copy(themeMode = mode) } }
        )
        SwitchRow(
            title = "Pure black in dark mode",
            subtitle = "Black instead of dark grey - saves power on OLED screens",
            checked = settings.amoledDark
        ) { value -> viewModel.update { it.copy(amoledDark = value) } }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SwitchRow(
                title = "Use my wallpaper colours",
                subtitle = "Takes the palette from your wallpaper (Material You)",
                checked = settings.dynamicColor
            ) { value -> viewModel.update { it.copy(dynamicColor = value) } }
        }
    }

    if (!settings.dynamicColor) {
        SectionHeader("Accent colour")
        SettingsCard {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    "Used for buttons, streaks and highlights.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
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
}

@Composable
private fun CameraSection(settings: AppSettings, viewModel: SettingsViewModel) {
    SettingsCard {
        ChoiceRow(
            title = "Self-timer",
            options = listOf(0, 3, 5, 10),
            selected = settings.countdownSeconds,
            label = { if (it == 0) "Off" else "$it seconds" },
            onSelect = { seconds -> viewModel.update { it.copy(countdownSeconds = seconds) } }
        )
        SwitchRow(
            title = "Check the shot before it's saved",
            subtitle = "Shows the photo with Keep and Retake instead of saving straight away",
            checked = settings.reviewBeforeSave
        ) { value -> viewModel.update { it.copy(reviewBeforeSave = value) } }
        SwitchRow(
            title = "Mirror selfies",
            subtitle = "Saves front-camera photos the way you saw them in the viewfinder",
            checked = settings.mirrorFrontCamera
        ) { value -> viewModel.update { it.copy(mirrorFrontCamera = value) } }
        SwitchRow(
            title = "Keep the screen awake",
            subtitle = "Stops the screen dimming while you line up the shot",
            checked = settings.keepScreenOn
        ) { value -> viewModel.update { it.copy(keepScreenOn = value) } }
        SwitchRow(
            title = "Vibrate on shutter",
            checked = settings.haptics
        ) { value -> viewModel.update { it.copy(haptics = value) } }
    }
}

@Composable
private fun OverlaySection(settings: AppSettings, viewModel: SettingsViewModel) {
    SettingsCard {
        ChoiceRow(
            title = "Overlay style",
            options = GhostMode.entries.toList(),
            selected = settings.ghostMode,
            label = { it.label },
            onSelect = { mode -> viewModel.update { it.copy(ghostMode = mode) } }
        )
        ChoiceRow(
            title = "Line up against",
            options = GhostReference.entries.toList(),
            selected = settings.ghostReference,
            label = { it.label },
            onSelect = { reference -> viewModel.update { it.copy(ghostReference = reference) } }
        )
        SliderRow(
            title = "Overlay strength",
            valueLabel = "${(settings.ghostAlpha * 100).toInt()}%",
            value = settings.ghostAlpha,
            range = 0.05f..0.95f,
            onValueChange = { value -> viewModel.update { it.copy(ghostAlpha = value) } }
        )
        SliderRow(
            title = "Overlay size",
            valueLabel = "${(settings.ghostScale * 100).toInt()}%",
            value = settings.ghostScale,
            range = 0.4f..3f,
            onValueChange = { value -> viewModel.update { it.copy(ghostScale = value) } }
        )
    }

    SectionHeader("Guides")
    SettingsCard {
        ChoiceRow(
            title = "Grid",
            options = GridMode.entries.toList(),
            selected = settings.gridMode,
            label = { it.label },
            onSelect = { grid -> viewModel.update { it.copy(gridMode = grid) } }
        )
        SwitchRow(
            title = "Show the crop frame",
            subtitle = "Dims everything outside the square that actually gets saved",
            checked = settings.showFrameBox
        ) { value -> viewModel.update { it.copy(showFrameBox = value) } }
        SwitchRow(
            title = "Head guide oval",
            subtitle = "Dashed oval showing where to put your face",
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
    }

    SectionHeader("Lighting")
    SettingsCard {
        SwitchRow(
            title = "Face light",
            subtitle = "Turns the screen around the frame into a soft light for dim rooms",
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
    }
}

@Composable
private fun RemindersSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    context: Context,
    onPickTime: () -> Unit
) {
    Text(
        "These are the starting values for projects you create from now on. Each project " +
            "keeps its own time and days, changed from that project's settings.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
    SettingsCard {
        SettingRow(
            title = "Reminder time",
            subtitle = formatMinuteOfDay(settings.defaultReminderMinuteOfDay),
            icon = Icons.Default.Alarm,
            onClick = onPickTime
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Days", style = MaterialTheme.typography.bodyLarge)
            Text(
                Weekdays.label(settings.defaultActiveDaysMask),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            WeekdayPicker(
                mask = settings.defaultActiveDaysMask,
                onMaskChange = { mask -> viewModel.update { it.copy(defaultActiveDaysMask = mask) } }
            )
        }
        ChoiceRow(
            title = "Ask again every",
            options = listOf(15, 30, 45, 60, 120),
            selected = settings.defaultNagIntervalMinutes,
            label = { if (it >= 60) "${it / 60}h" else "$it min" },
            onSelect = { minutes -> viewModel.update { it.copy(defaultNagIntervalMinutes = minutes) } }
        )
    }

    SectionHeader("Make reminders reliable")
    SettingsCard {
        SettingRow(
            title = "Notification sound & vibration",
            subtitle = "Opens Android's notification settings for DayOne",
            icon = Icons.Default.Notifications,
            onClick = { openNotificationSettings(context) }
        )
        SettingRow(
            title = "Battery restrictions",
            subtitle = "Set DayOne to Unrestricted so Android doesn't delay reminders",
            icon = Icons.Default.BatterySaver,
            onClick = { openBatterySettings(context) }
        )
    }
}

@Composable
private fun VideoSection(settings: AppSettings, viewModel: SettingsViewModel) {
    Text(
        "Defaults for the timelapse. You can change any of it on the export screen too.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
    SettingsCard {
        ChoiceRow(
            title = "Resolution",
            options = listOf(720, 1080, 1440),
            selected = settings.videoResolution,
            label = { "${it}p" },
            onSelect = { value -> viewModel.update { it.copy(videoResolution = value) } }
        )
        ChoiceRow(
            title = "Frame rate",
            options = listOf(24, 30, 60),
            selected = settings.videoFps,
            label = { "$it fps" },
            onSelect = { value -> viewModel.update { it.copy(videoFps = value) } }
        )
        ChoiceRow(
            title = "Time per photo",
            options = listOf(50, 80, 100, 150, 200, 330, 500, 1000),
            selected = settings.videoMillisPerPhoto,
            label = { if (it >= 1000) "${it / 1000}s" else "${it}ms" },
            onSelect = { value -> viewModel.update { it.copy(videoMillisPerPhoto = value) } }
        )
        SwitchRow(
            title = "Fade between days",
            subtitle = "Blends one photo into the next instead of a hard cut",
            checked = settings.videoCrossfade
        ) { value -> viewModel.update { it.copy(videoCrossfade = value) } }
        SwitchRow(
            title = "Play newest first",
            checked = settings.videoNewestFirst
        ) { value -> viewModel.update { it.copy(videoNewestFirst = value) } }
        SwitchRow(
            title = "Save videos to your gallery",
            subtitle = "Copies the finished video to Movies/DayOne",
            checked = settings.videoSaveToGallery
        ) { value -> viewModel.update { it.copy(videoSaveToGallery = value) } }
    }
}

@Composable
private fun PhotosSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    context: Context,
    storageLabel: String,
    migration: MigrationState,
    onMigrate: () -> Unit
) {
    SettingsCard {
        SwitchRow(
            title = "Save a copy to your gallery",
            subtitle = "New photos are also written to Pictures/DayOne, one folder per project, " +
                "so they show up in Gallery and survive uninstalling",
            checked = settings.saveCopyToGallery,
            icon = Icons.Default.PhotoLibrary
        ) { value -> viewModel.update { it.copy(saveCopyToGallery = value) } }
    }

    SectionHeader("Photos already in the app")
    SettingsCard {
        SettingRow(
            title = "Copy existing photos to your gallery",
            subtitle = when (migration) {
                is MigrationState.Running -> "Copying ${(migration.progress * 100).toInt()}%..."
                is MigrationState.Done -> migration.summary
                else -> "Puts every photo already in the app into Pictures/DayOne. " +
                    "Photos taken before you turned the setting on, and anything you imported, " +
                    "are only inside the app until you do this."
            },
            icon = Icons.Default.Folder,
            enabled = migration !is MigrationState.Running,
            onClick = onMigrate,
            trailing = {
                if (migration is MigrationState.Running) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        )
        if (migration is MigrationState.Running) {
            LinearProgressIndicator(
                progress = { migration.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    SectionHeader("Where things are kept")
    SettingsCard {
        SettingRow(title = "Photos in the app", subtitle = storageLabel, icon = Icons.Default.Info)
        SettingRow(
            title = "App folder",
            subtitle = "Android/data/${context.packageName}/files/DayOne"
        )
        SettingRow(title = "Gallery folder", subtitle = "Pictures/DayOne/<project>")
        SettingRow(title = "Exported videos", subtitle = "Movies/DayOne")
        SettingRow(title = "Backups", subtitle = "Documents/DayOne")
    }
}

@Composable
private fun BackupSection(busy: Boolean, onExport: () -> Unit, onRestore: () -> Unit) {
    Text(
        "A backup is a single .zip holding every project, photo, note and setting. It lives " +
            "in Documents, outside the app, so it survives uninstalling the app or changing phone.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
    SettingsCard {
        SettingRow(
            title = "Back up everything now",
            subtitle = "Saves to Documents/DayOne",
            icon = Icons.Default.Backup,
            enabled = !busy,
            onClick = onExport,
            trailing = {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        )
        SettingRow(
            title = "Restore from a backup",
            subtitle = "Fills in missing days and keeps the photos you already have",
            icon = Icons.Default.RestorePage,
            enabled = !busy,
            onClick = onRestore
        )
    }
}

@Composable
private fun AboutSection(
    context: Context,
    onReplayIntro: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenLicences: () -> Unit
) {
    SettingsCard {
        SettingRow(
            title = "DayOne ${BuildConfig.VERSION_NAME}",
            subtitle = "Works entirely offline. No accounts, no analytics, and no permission " +
                "to use the internet at all."
        )
        SettingRow(
            title = "Show the intro again",
            subtitle = "Replays the welcome tour and permission prompts",
            icon = Icons.Default.Replay,
            onClick = onReplayIntro
        )
    }

    SectionHeader("Legal")
    SettingsCard {
        SettingRow(
            title = "Privacy policy",
            subtitle = "What is stored, and why none of it leaves the phone",
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
}

// ---------------------------------------------------------------------------- helpers

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

// ---------------------------------------------------------------------------- view model

sealed interface MigrationState {
    data object Idle : MigrationState
    data class Running(val progress: Float) : MigrationState
    data class Done(val summary: String) : MigrationState
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

    private val _migration = MutableStateFlow<MigrationState>(MigrationState.Idle)
    val migration: StateFlow<MigrationState> = _migration.asStateFlow()

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

    /**
     * Copies every project's photos into the gallery. Photos already there are skipped, so
     * running it twice costs time but never duplicates anything.
     */
    fun migrateToGallery(context: Context) {
        if (_migration.value is MigrationState.Running) return
        val appContext = context.applicationContext
        _migration.value = MigrationState.Running(0f)

        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val projects = repo.getAllProjects()
                    var copied = 0
                    var skipped = 0
                    var failed = 0

                    projects.forEachIndexed { index, project ->
                        val files = repo.getEntries(project.id).map { File(it.filePath) }
                        val outcome = GalleryMigrator.migrate(appContext, project, files) { inner ->
                            val overall = (index + inner) / projects.size.coerceAtLeast(1)
                            _migration.value = MigrationState.Running(overall.coerceIn(0f, 1f))
                        }
                        copied += outcome.copied
                        skipped += outcome.alreadyThere
                        failed += outcome.failed
                    }
                    Triple(copied, skipped, failed)
                }
            }

            result.onSuccess { (copied, skipped, failed) ->
                val summary = buildString {
                    append(if (copied == 0) "Nothing new to copy" else "Copied $copied photo(s) to Pictures/DayOne")
                    if (skipped > 0) append(" - $skipped already there")
                    if (failed > 0) append(" - $failed could not be copied")
                }
                _migration.value = MigrationState.Done(summary)
                _messages.tryEmit(summary)
            }.onFailure { error ->
                _migration.value = MigrationState.Idle
                _messages.tryEmit("Copy to gallery failed: ${error.message}")
            }
        }
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
                    if (saved == null) "Backup could not be saved to shared storage"
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
