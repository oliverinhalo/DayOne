package com.dayone.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dayone.app.ui.screens.CaptureScreen
import com.dayone.app.ui.screens.CreateProjectScreen
import com.dayone.app.ui.screens.LegalScreen
import com.dayone.app.ui.screens.LicencesScreen
import com.dayone.app.ui.screens.OnboardingScreen
import com.dayone.app.ui.screens.ProjectHomeScreen
import com.dayone.app.ui.screens.ProjectListScreen
import com.dayone.app.ui.screens.ProjectSettingsScreen
import com.dayone.app.ui.screens.SettingsScreen
import com.dayone.app.ui.screens.SettingsSection
import com.dayone.app.ui.screens.SettingsSectionScreen
import com.dayone.app.ui.screens.TimelineScreen
import com.dayone.app.ui.screens.VideoExportScreen
import com.dayone.app.ui.theme.DayOneTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_CAPTURE_PROJECT_ID = "open_capture_project_id"
    }

    private var pendingCaptureProjectId by mutableStateOf<Long?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* banner in UI covers denial */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Required on Android 13+ or the daily reminders can't be shown at all.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val startCaptureId = intent.getLongExtra(EXTRA_OPEN_CAPTURE_PROJECT_ID, -1L).takeIf { it != -1L }

        setContent {
            val app = application as DayOneApp
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()

            DayOneTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
                amoledDark = settings.amoledDark,
                accentArgb = settings.accentArgb
            ) {
                val navController = rememberNavController()

                // A notification tapped while the app is already running arrives through
                // onNewIntent rather than a fresh onCreate, so route it here.
                LaunchedEffect(pendingCaptureProjectId) {
                    pendingCaptureProjectId?.let { projectId ->
                        navController.navigate("capture/$projectId")
                        pendingCaptureProjectId = null
                    }
                }

                // Terms have to be accepted before anything else opens, and a bumped
                // TERMS_VERSION re-prompts an existing install without touching its data.
                val needsOnboarding = !settings.onboardingComplete ||
                    settings.acceptedTermsVersion < com.dayone.app.ui.screens.Legal.TERMS_VERSION

                AppNavHost(
                    navController = navController,
                    startCaptureProjectId = startCaptureId,
                    needsOnboarding = needsOnboarding
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getLongExtra(EXTRA_OPEN_CAPTURE_PROJECT_ID, -1L)
            .takeIf { it != -1L }
            ?.let { pendingCaptureProjectId = it }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startCaptureProjectId: Long?,
    needsOnboarding: Boolean = false
) {
    val slideIn = AnimatedContentTransitionScope.SlideDirection.Start
    val slideOut = AnimatedContentTransitionScope.SlideDirection.End

    NavHost(
        navController = navController,
        startDestination = when {
            needsOnboarding -> "onboarding"
            startCaptureProjectId != null -> "capture/$startCaptureProjectId"
            else -> "projects"
        },
        modifier = Modifier,
        enterTransition = { slideIntoContainer(slideIn, tween(260)) + fadeIn(tween(200)) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = { slideOutOfContainer(slideOut, tween(260)) + fadeOut(tween(200)) }
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onFinished = {
                    navController.navigate("projects") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onOpenTerms = { navController.navigate("terms") },
                onOpenPrivacy = { navController.navigate("privacy") }
            )
        }
        composable("terms") { LegalScreen(privacy = false, onBack = { navController.popBackStack() }) }
        composable("privacy") { LegalScreen(privacy = true, onBack = { navController.popBackStack() }) }
        composable("licences") { LicencesScreen(onBack = { navController.popBackStack() }) }
        composable("projects") {
            ProjectListScreen(
                onOpenProject = { id -> navController.navigate("project/$id") },
                onCreateProject = { navController.navigate("createProject") },
                onOpenSettings = { navController.navigate("settings") },
                onQuickCapture = { id -> navController.navigate("capture/$id") }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenSection = { section -> navController.navigate("settings/${section.route}") }
            )
        }
        composable(
            "settings/{section}",
            arguments = listOf(navArgument("section") { type = NavType.StringType })
        ) { backStackEntry ->
            SettingsSectionScreen(
                section = SettingsSection.fromRoute(backStackEntry.arguments?.getString("section")),
                onBack = { navController.popBackStack() },
                onReplayIntro = { navController.navigate("onboarding") },
                onOpenTerms = { navController.navigate("terms") },
                onOpenPrivacy = { navController.navigate("privacy") },
                onOpenLicences = { navController.navigate("licences") }
            )
        }
        composable("createProject") {
            CreateProjectScreen(
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            "project/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ProjectHomeScreen(
                projectId = projectId,
                onCapture = { navController.navigate("capture/$projectId") },
                onTimeline = { day ->
                    navController.navigate("timeline/$projectId" + (day?.let { "?day=$it" } ?: ""))
                },
                onVideoExport = { navController.navigate("video/$projectId") },
                onProjectSettings = { navController.navigate("projectSettings/$projectId") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "projectSettings/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ProjectSettingsScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack("projects", inclusive = false) }
            )
        }
        composable(
            "capture/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            CaptureScreen(
                projectId = projectId,
                onDone = {
                    if (!navController.popBackStack()) {
                        navController.navigate("project/$projectId") {
                            popUpTo("capture/$projectId") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            "timeline/{projectId}?day={day}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("day") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            val day = backStackEntry.arguments?.getLong("day")?.takeIf { it != -1L }
            TimelineScreen(
                projectId = projectId,
                initialEpochDay = day,
                onBack = { navController.popBackStack() }
            )
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
