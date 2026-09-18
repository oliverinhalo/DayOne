package com.dayone.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayone.app.data.GhostMode
import com.dayone.app.data.GhostReference
import com.dayone.app.data.GridMode
import com.dayone.app.ui.components.ChoiceRow
import com.dayone.app.ui.components.SwitchRow
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    projectId: Long,
    onDone: () -> Unit,
    viewModel: CaptureViewModel = viewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    LaunchedEffect(projectId) { viewModel.load(projectId) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    if (!hasCameraPermission) {
        PermissionRequestUi(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        return
    }

    val cameraController = remember { com.dayone.app.camera.CameraController(context) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    var cameraReady by remember { mutableStateOf(false) }
    val torchOn by cameraController.torchOn.collectAsStateWithLifecycle()
    val hasTorch by cameraController.hasTorch.collectAsStateWithLifecycle()
    var usingFront by remember { mutableStateOf(settings.useFrontCamera) }

    DisposableEffect(Unit) {
        cameraController.bind(lifecycleOwner, previewView, front = usingFront) { cameraReady = true }
        onDispose { cameraController.shutdown() }
    }

    // Composing a photo takes a moment - don't let the screen blank out mid-pose.
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    var adjustOverlayMode by remember { mutableStateOf(false) }
    var showTuning by remember { mutableStateOf(false) }
    var countdownRemaining by remember { mutableIntStateOf(0) }
    var shutterRequested by remember { mutableStateOf(false) }

    fun haptic() {
        if (settings.haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun shoot() {
        val raw = File(context.cacheDir, "raw_${System.currentTimeMillis()}.jpg")
        haptic()
        cameraController.capture(raw, mirror = usingFront && settings.mirrorFrontCamera) { success, _ ->
            if (success) viewModel.processCapture(raw, reviewFirst = settings.reviewBeforeSave)
        }
    }

    // Countdown timer, so you can put the phone down and get into frame.
    LaunchedEffect(shutterRequested) {
        if (!shutterRequested) return@LaunchedEffect
        if (settings.countdownSeconds > 0) {
            countdownRemaining = settings.countdownSeconds
            while (countdownRemaining > 0) {
                delay(1000)
                countdownRemaining--
            }
        }
        shoot()
        shutterRequested = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(adjustOverlayMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (adjustOverlayMode) {
                            // Nudge and resize the ghost itself to line it up precisely.
                            if (pan != androidx.compose.ui.geometry.Offset.Zero) {
                                viewModel.nudgeGhost(pan.x / size.width, pan.y / size.height)
                            }
                            if (zoom != 1f) viewModel.scaleGhost(zoom)
                        } else if (zoom != 1f) {
                            cameraController.setZoomRatio(cameraController.zoomRatio.value * zoom)
                        }
                    }
                }
                .pointerInput(adjustOverlayMode) {
                    detectTapGestures { offset ->
                        if (!adjustOverlayMode) cameraController.focusAt(previewView, offset.x, offset.y)
                    }
                }
        )

        CaptureOverlay(
            reference = state.reference,
            outline = state.referenceOutline,
            settings = settings,
            headFraction = state.project?.headFraction ?: 0.42f,
            modifier = Modifier.fillMaxSize()
        )

        // ---- top bar -------------------------------------------------------------
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(Icons.Default.Close, "Close", onClick = onDone)
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(
                        state.project?.name.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        buildString {
                            append("Day ${state.dayNumber}")
                            if (state.streak > 0) append("  •  ${state.streak} day streak")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (hasTorch) {
                GlassIconButton(
                    if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    "Torch",
                    onClick = { cameraController.setTorch(!torchOn) }
                )
                Spacer(Modifier.width(8.dp))
            }
            GlassIconButton(Icons.Default.Cameraswitch, "Flip camera", onClick = {
                cameraController.toggleCamera(lifecycleOwner, previewView) { front ->
                    usingFront = front
                    viewModel.rememberLens(front)
                }
            })
        }

        // ---- countdown -----------------------------------------------------------
        AnimatedVisibility(
            visible = countdownRemaining > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                countdownRemaining.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // ---- bottom controls -----------------------------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            if (state.alreadyCapturedToday) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Already shot today - a new photo replaces it",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            // Overlay strength - the control people reach for most, so it stays on screen.
            if (settings.ghostMode != GhostMode.OFF && state.reference != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BlurOn, contentDescription = "Overlay strength", tint = Color.White)
                    Slider(
                        value = settings.ghostAlpha,
                        onValueChange = { viewModel.setGhostAlpha(it) },
                        valueRange = 0.05f..0.95f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    Text(
                        "${(settings.ghostAlpha * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    GlassIconButton(
                        Icons.Default.Tune,
                        "Overlay settings",
                        onClick = { showTuning = true }
                    )
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(
                        Icons.Default.OpenWith,
                        if (adjustOverlayMode) "Stop adjusting overlay" else "Move overlay",
                        highlighted = adjustOverlayMode,
                        onClick = { adjustOverlayMode = !adjustOverlayMode }
                    )
                }

                ShutterButton(
                    enabled = cameraReady && !state.saving && countdownRemaining == 0,
                    busy = state.saving,
                    onClick = { shutterRequested = true }
                )

                Row {
                    GlassIconButton(
                        Icons.Default.Timer,
                        "Countdown timer",
                        label = if (settings.countdownSeconds > 0) "${settings.countdownSeconds}s" else null,
                        onClick = {
                            val next = when (settings.countdownSeconds) {
                                0 -> 3
                                3 -> 5
                                5 -> 10
                                else -> 0
                            }
                            viewModel.setCountdown(next)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(
                        Icons.Default.Grid3x3,
                        "Grid",
                        highlighted = settings.gridMode != GridMode.NONE,
                        onClick = {
                            val next = when (settings.gridMode) {
                                GridMode.NONE -> GridMode.THIRDS
                                GridMode.THIRDS -> GridMode.GRID
                                GridMode.GRID -> GridMode.CENTER
                                GridMode.CENTER -> GridMode.NONE
                            }
                            viewModel.setGridMode(next)
                        }
                    )
                }
            }

            if (adjustOverlayMode) {
                Text(
                    "Drag to move the overlay, pinch to resize",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 6.dp)
                )
            }
        }

        // ---- review sheet --------------------------------------------------------
        state.pending?.let { pending ->
            ReviewOverlay(
                pending = pending,
                onSave = { viewModel.confirmPending() },
                onRetake = { viewModel.discardPending() }
            )
        }

        state.error?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }
    }

    if (showTuning) {
        ModalBottomSheet(
            onDismissRequest = { showTuning = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "Overlay",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
                ChoiceRow(
                    title = "Style",
                    options = GhostMode.entries.toList(),
                    selected = settings.ghostMode,
                    label = { it.label },
                    onSelect = { viewModel.setGhostMode(it) }
                )
                ChoiceRow(
                    title = "Reference photo",
                    options = GhostReference.entries.toList(),
                    selected = settings.ghostReference,
                    label = { it.label },
                    onSelect = { viewModel.setGhostReference(it) }
                )
                ChoiceRow(
                    title = "Grid",
                    options = GridMode.entries.toList(),
                    selected = settings.gridMode,
                    label = { it.label },
                    onSelect = { viewModel.setGridMode(it) }
                )
                SwitchRow(
                    title = "Head guide oval",
                    subtitle = "Shows where the auto-crop will put your face",
                    checked = settings.showFaceGuide,
                    onCheckedChange = { viewModel.setFaceGuide(it) }
                )
                SwitchRow(
                    title = "Mirror the overlay",
                    subtitle = "Use if the reference photo was taken on the other camera",
                    checked = settings.ghostFlip,
                    onCheckedChange = { viewModel.setGhostFlip(it) }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.resetGhostTransform() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reset position")
                    }
                    Button(onClick = { showTuning = false }, modifier = Modifier.weight(1f)) { Text("Done") }
                }
                if (state.referenceLabel.isNotBlank()) {
                    Text(
                        "Reference: ${state.referenceLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    state.lastSavedEntry?.let { entry ->
        LaunchedEffect(entry.id, entry.dateEpochDay) { onDone() }
    }
}

@Composable
private fun ReviewOverlay(pending: PendingShot, onSave: () -> Unit, onRetake: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.foundation.Image(
                bitmap = pending.preview,
                contentDescription = "Photo just taken",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Keep this one?",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRetake) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Retake")
                }
                Button(onClick = onSave) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (enabled) 1f else 0.9f, label = "shutter-scale")
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.95f else 0.5f))
            .pointerInput(enabled) {
                detectTapGestures { if (enabled) onClick() }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((62 * scale).dp)
                .clip(CircleShape)
                .background(if (busy) Color.Gray else Color.White)
        )
        if (busy) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color.Black
            )
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    highlighted: Boolean = false,
    label: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        else Color.Black.copy(alpha = 0.45f),
        modifier = Modifier.size(44.dp)
    ) {
        Box(
            modifier = Modifier.pointerInput(Unit) { detectTapGestures { onClick() } },
            contentAlignment = Alignment.Center
        ) {
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    icon,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionRequestUi(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "DayOne needs camera access to take your daily photo.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Grant camera permission") }
    }
}
