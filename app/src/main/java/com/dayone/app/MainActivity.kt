package com.dayone.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dayone.app.ui.screens.*
import com.dayone.app.ui.theme.DayOneTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_CAPTURE_PROJECT_ID = "open_capture_project_id"
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op: banner in UI covers denial */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Required on Android 13+ or the daily/nag reminders can't be shown at all.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val openCaptureProjectId = intent.getLongExtra(EXTRA_OPEN_CAPTURE_PROJECT_ID, -1L)

        setContent {
            DayOneTheme {
                AppNavHost(startCaptureProjectId = openCaptureProjectId.takeIf { it != -1L })
            }
        }
    }
}

@Composable
fun AppNavHost(startCaptureProjectId: Long?) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (startCaptureProjectId != null) "capture/$startCaptureProjectId" else "projects",
        modifier = Modifier
    ) {
        composable("projects") {
            ProjectListScreen(
                onOpenProject = { id -> navController.navigate("project/$id") },
                onCreateProject = { navController.navigate("createProject") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("createProject") {
            CreateProjectScreen(onDone = { navController.popBackStack() })
        }
        composable(
            "project/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ProjectHomeScreen(
                projectId = projectId,
                onCapture = { navController.navigate("capture/$projectId") },
                onTimeline = { navController.navigate("timeline/$projectId") },
                onVideoExport = { navController.navigate("video/$projectId") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "capture/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            CaptureScreen(
                projectId = projectId,
                onDone = { navController.popBackStack("projects", inclusive = false) }
            )
        }
        composable(
            "timeline/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            TimelineScreen(projectId = projectId, onBack = { navController.popBackStack() })
        }
        composable(
            "video/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            VideoExportScreen(projectId = projectId, onBack = { navController.popBackStack() })
        }
    }
}
