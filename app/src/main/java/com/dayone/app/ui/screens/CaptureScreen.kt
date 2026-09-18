package com.dayone.app.ui.screens

import android.Manifest
import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dayone.app.camera.CameraController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    projectId: Long,
    onDone: () -> Unit,
    viewModel: CaptureViewModel = viewModel_()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(projectId) { viewModel.load(projectId) }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    if (!cameraPermission.status.isGranted) {
        PermissionRequestUi(onRequest = { cameraPermission.launchPermissionRequest() })
        return
    }

    val cameraController = remember { CameraController(context) }
    val previewView = remember { PreviewView(context) }
    var cameraReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        cameraController.bind(lifecycleOwner, previewView) { cameraReady = true }
        onDispose { cameraController.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        CaptureOverlay(
            previousPhoto = state.previousPhotoBitmap,
            ghostAlpha = state.ghostAlpha,
            showGuideOval = state.showGuide,
            zoomScalar = state.zoomScalar,
            modifier = Modifier.fillMaxSize()
        )

        // Top bar: project name + day counter
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 32.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
                Text(
                    text = state.project?.name ?: "",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            IconButton(onClick = { cameraController.toggleCamera(lifecycleOwner, previewView) }) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Flip camera", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Bottom controls: ghost alpha slider + guide toggle + shutter
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.alreadyCapturedToday) {
                Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 6.dp) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Already captured today - retake to overwrite")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.GridOn, contentDescription = "Ghost overlay strength")
                Slider(
                    value = state.ghostAlpha,
                    onValueChange = { viewModel.setGhostAlpha(it) },
                    valueRange = 0f..0.7f,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            Box(contentAlignment = Alignment.Center) {
                FilledIconButton(
                    onClick = {
                        val raw = File(context.cacheDir, "raw_${System.currentTimeMillis()}.jpg")
                        cameraController.capture(raw) { success, _ ->
                            if (success) {
                                scope.launch { viewModel.processCapture(raw) }
                            }
                        }
                    },
                    enabled = cameraReady && !state.saving,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    } else {
                        Text("●", style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }
        }

        state.lastSavedEntry?.let {
            LaunchedEffect(it.id) { onDone() }
        }

        state.error?.let { err ->
            Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                Text(err)
            }
        }
    }
}

@Composable
private fun PermissionRequestUi(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("DayOne needs camera access to take your daily photo.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Grant camera permission") }
    }
}

// Small helper so this file doesn't need an extra import block change elsewhere
@Composable
private fun viewModel_(): CaptureViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
