package com.dayone.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dayone.app.DayOneApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsRepo = remember { (context.applicationContext as DayOneApp).settingsRepository }
    
    var zoomScalar by remember { mutableStateOf(settingsRepo.zoomScalar) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(text = "Capture Zoom", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Controls how closely the app crops to your face. Higher means more zoom.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "%.1fx".format(zoomScalar), modifier = Modifier.width(48.dp))
                Slider(
                    value = zoomScalar,
                    onValueChange = { 
                        zoomScalar = it
                        settingsRepo.zoomScalar = it
                    },
                    valueRange = 0.5f..4.0f,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Text(
                text = "Default is 1.5x",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
