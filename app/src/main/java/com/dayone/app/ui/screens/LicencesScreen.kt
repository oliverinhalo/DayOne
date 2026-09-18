package com.dayone.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayone.app.ui.components.SectionHeader
import com.dayone.app.ui.components.SettingRow
import com.dayone.app.ui.components.SettingsCard

private data class Licence(val name: String, val holder: String, val licence: String)

private val licences = listOf(
    Licence("AndroidX (Core, Lifecycle, Activity, Navigation, Room, CameraX, ExifInterface, DocumentFile, SplashScreen)", "The Android Open Source Project", "Apache License 2.0"),
    Licence("Jetpack Compose & Material 3", "The Android Open Source Project", "Apache License 2.0"),
    Licence("Kotlin standard library & Coroutines", "JetBrains s.r.o.", "Apache License 2.0"),
    Licence("Coil", "Coil Contributors", "Apache License 2.0"),
    Licence("ML Kit face detection (bundled, on-device)", "Google LLC", "Google APIs Terms of Service")
)

/** Third-party attribution, kept in the app because it has no way to open a web page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicencesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open source licences") },
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
            SectionHeader("Libraries")
            SettingsCard {
                licences.forEach { entry ->
                    SettingRow(title = entry.name, subtitle = "${entry.holder}  •  ${entry.licence}")
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Apache License 2.0 - the full text is available at apache.org/licenses/LICENSE-2.0. " +
                    "DayOne itself is built from the source in its GitHub repository.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}
