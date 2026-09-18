package com.dayone.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * First-run flow: what the app is, what it does with your photos, the terms, and the two
 * permissions it needs. Nothing is stored and no camera is opened until the terms are
 * accepted, which is also what Play expects of an app that asks for camera access.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    var accepted by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(0) }

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraGranted = granted }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            when (page) {
                0 -> {
                    Text("One photo a day", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Take the same photo every day and watch a year go past in thirty seconds. " +
                            "Run as many of these as you like - yourself, a group, a room, a plant.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(28.dp))
                    FeatureLine(Icons.Default.CameraAlt, "Line up with yesterday", "A ghost or outline of a previous photo sits over the viewfinder, and faces are auto-centred.")
                    FeatureLine(Icons.Default.Alarm, "Nudges on your days", "Pick the time and the weekdays. Rest days never break a streak.")
                    FeatureLine(Icons.Default.Lock, "Stays on this phone", Legal.SUMMARY)
                }

                1 -> {
                    Text("Before you start", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        Legal.SUMMARY,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text("I agree to the Terms of Use and Privacy Policy.")
                            Row {
                                TextButton(onClick = onOpenTerms) { Text("Terms") }
                                TextButton(onClick = onOpenPrivacy) { Text("Privacy") }
                            }
                        }
                    }
                }

                else -> {
                    Text("Two permissions", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "You can skip either one and grant it later from Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    PermissionCard(
                        title = "Camera",
                        rationale = "Needed to take the photo. Used only while the capture screen is open.",
                        granted = cameraGranted,
                        onGrant = { cameraLauncher.launch(Manifest.permission.CAMERA) }
                    )
                    Spacer(Modifier.height(12.dp))
                    PermissionCard(
                        title = "Notifications",
                        rationale = "Needed for the daily reminder. Without it the app cannot nudge you.",
                        granted = notificationsGranted,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(visible = page > 0) {
                    OutlinedButton(onClick = { page-- }) { Text("Back") }
                }
                Button(
                    onClick = {
                        if (page < 2) {
                            page++
                        } else {
                            viewModel.completeOnboarding()
                            onFinished()
                        }
                    },
                    enabled = page != 1 || accepted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when (page) {
                            0 -> "Get started"
                            1 -> if (accepted) "Agree and continue" else "Agree to continue"
                            else -> "Start using DayOne"
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Step ${page + 1} of 3",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun FeatureLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Row(modifier = Modifier.padding(bottom = 18.dp)) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    rationale: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (title == "Camera") Icons.Default.CameraAlt else Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.fillMaxWidth(0.72f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.fillMaxWidth(0.02f))
            if (granted) {
                Text("Granted", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            } else {
                TextButton(onClick = onGrant) { Text("Allow") }
            }
        }
    }
}
