package com.dayone.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** How the previous photo is drawn on top of the live camera preview. */
enum class GhostMode {
    OFF,        // no ghost at all
    GHOST,      // classic translucent overlay
    OUTLINE,    // edge-detected outline only - easiest to line up against
    SPLIT,      // left half is the reference photo, right half is live
    STRIPES;    // interleaved vertical bands of reference / live

    val label: String
        get() = when (this) {
            OFF -> "Off"
            GHOST -> "Ghost"
            OUTLINE -> "Outline"
            SPLIT -> "Split"
            STRIPES -> "Stripes"
        }
}

/** Which past photo the overlay shows. */
enum class GhostReference {
    PREVIOUS,   // most recent photo
    FIRST,      // day one - good for long-run consistency
    PINNED;     // a specific day the user pinned from the timeline

    val label: String
        get() = when (this) {
            PREVIOUS -> "Latest photo"
            FIRST -> "Very first photo"
            PINNED -> "Pinned day"
        }
}

enum class GridMode {
    NONE, THIRDS, GRID, CENTER;

    val label: String
        get() = when (this) {
            NONE -> "None"
            THIRDS -> "Rule of thirds"
            GRID -> "Fine grid"
            CENTER -> "Centre cross"
        }
}

/**
 * Every app-wide preference, in one immutable snapshot. The repository keeps a
 * [StateFlow] of this so Compose screens just collect it and stay in sync - editing a
 * setting anywhere updates every screen (and the capture overlay) immediately.
 */
data class AppSettings(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val amoledDark: Boolean = false,
    val accentArgb: Int = 0xFF00E5A0.toInt(),

    // Capture overlay
    val ghostMode: GhostMode = GhostMode.GHOST,
    val ghostAlpha: Float = 0.45f,
    val ghostReference: GhostReference = GhostReference.PREVIOUS,
    val ghostFlip: Boolean = false,
    val ghostScale: Float = 1.0f,
    val ghostOffsetX: Float = 0f,
    val ghostOffsetY: Float = 0f,
    val gridMode: GridMode = GridMode.THIRDS,
    val showFaceGuide: Boolean = true,
    /** Size of the head-guide oval relative to the crop frame, independent of the crop. */
    val faceGuideScale: Float = 1.0f,
    val showFrameBox: Boolean = true,

    // Capture behaviour
    val countdownSeconds: Int = 0,
    val reviewBeforeSave: Boolean = true,
    val keepScreenOn: Boolean = true,
    val mirrorFrontCamera: Boolean = true,
    val haptics: Boolean = true,
    val useFrontCamera: Boolean = true,
    val saveCopyToGallery: Boolean = false,
    /** Screen-as-fill-light for front-camera shots in poor light. */
    val faceLight: Boolean = false,
    val faceLightIntensity: Float = 0.75f,

    // Defaults applied to newly created projects
    val defaultReminderMinuteOfDay: Int = 480,
    val defaultActiveDaysMask: Int = 0b1111111,
    val defaultNagIntervalMinutes: Int = 45,
    val defaultNagUntilMinuteOfDay: Int = 1320,

    // Video export defaults
    val videoResolution: Int = 1080,
    val videoFps: Int = 30,
    val videoMillisPerPhoto: Int = 300,
    val videoCrossfade: Boolean = true,
    val videoNewestFirst: Boolean = false,
    val videoSaveToGallery: Boolean = true,

    // First-run state
    /** Version of the terms the user accepted; 0 = never accepted. */
    val acceptedTermsVersion: Int = 0,
    val onboardingComplete: Boolean = false
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("dayone_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val current: AppSettings get() = _settings.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        write(next)
        _settings.value = next
    }

    // ---- per-project scratch state (not worth a database column) -------------------

    /** A specific day pinned as the overlay reference for a project, or null. */
    fun pinnedReferenceDay(projectId: Long): Long? =
        prefs.getLong("pinned_ref_$projectId", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }

    fun setPinnedReferenceDay(projectId: Long, epochDay: Long?) {
        prefs.edit().apply {
            if (epochDay == null) remove("pinned_ref_$projectId") else putLong("pinned_ref_$projectId", epochDay)
        }.apply()
    }

    /** Days the user explicitly chose to skip - no nagging, and the streak survives. */
    fun isSkipped(projectId: Long, epochDay: Long): Boolean =
        prefs.getStringSet(KEY_SKIPPED, emptySet())!!.contains("$projectId:$epochDay")

    fun setSkipped(projectId: Long, epochDay: Long, skipped: Boolean) {
        val set = prefs.getStringSet(KEY_SKIPPED, emptySet())!!.toMutableSet()
        val token = "$projectId:$epochDay"
        if (skipped) set.add(token) else set.remove(token)
        // Keep this from growing forever - only the last ~400 skips matter.
        val trimmed = if (set.size > 400) set.toList().takeLast(400).toSet() else set
        prefs.edit().putStringSet(KEY_SKIPPED, trimmed).apply()
    }

    fun skippedDays(projectId: Long): Set<Long> =
        prefs.getStringSet(KEY_SKIPPED, emptySet())!!
            .mapNotNull { token ->
                val parts = token.split(":")
                if (parts.size == 2 && parts[0] == projectId.toString()) parts[1].toLongOrNull() else null
            }
            .toSet()

    /** Remembered so the same lens is used next time this project is opened. */
    fun setUseFrontCamera(front: Boolean) = update { it.copy(useFrontCamera = front) }

    // ---- persistence ---------------------------------------------------------------

    private fun read(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = enumOf(prefs.getString(K_THEME, null), ThemeMode.entries, d.themeMode),
            dynamicColor = prefs.getBoolean(K_DYNAMIC, d.dynamicColor),
            amoledDark = prefs.getBoolean(K_AMOLED, d.amoledDark),
            accentArgb = prefs.getInt(K_ACCENT, d.accentArgb),
            ghostMode = enumOf(prefs.getString(K_GHOST_MODE, null), GhostMode.entries, d.ghostMode),
            ghostAlpha = prefs.getFloat(K_GHOST_ALPHA, d.ghostAlpha),
            ghostReference = enumOf(prefs.getString(K_GHOST_REF, null), GhostReference.entries, d.ghostReference),
            ghostFlip = prefs.getBoolean(K_GHOST_FLIP, d.ghostFlip),
            ghostScale = prefs.getFloat(K_GHOST_SCALE, prefs.getFloat(LEGACY_ZOOM, d.ghostScale)),
            ghostOffsetX = prefs.getFloat(K_GHOST_DX, d.ghostOffsetX),
            ghostOffsetY = prefs.getFloat(K_GHOST_DY, d.ghostOffsetY),
            gridMode = enumOf(prefs.getString(K_GRID, null), GridMode.entries, d.gridMode),
            showFaceGuide = prefs.getBoolean(K_FACE_GUIDE, d.showFaceGuide),
            faceGuideScale = prefs.getFloat(K_FACE_GUIDE_SCALE, d.faceGuideScale),
            showFrameBox = prefs.getBoolean(K_FRAME_BOX, d.showFrameBox),
            countdownSeconds = prefs.getInt(K_COUNTDOWN, d.countdownSeconds),
            reviewBeforeSave = prefs.getBoolean(K_REVIEW, d.reviewBeforeSave),
            keepScreenOn = prefs.getBoolean(K_KEEP_ON, d.keepScreenOn),
            mirrorFrontCamera = prefs.getBoolean(K_MIRROR, d.mirrorFrontCamera),
            haptics = prefs.getBoolean(K_HAPTICS, d.haptics),
            useFrontCamera = prefs.getBoolean(K_FRONT, d.useFrontCamera),
            saveCopyToGallery = prefs.getBoolean(K_GALLERY, d.saveCopyToGallery),
            faceLight = prefs.getBoolean(K_FACE_LIGHT, d.faceLight),
            faceLightIntensity = prefs.getFloat(K_FACE_LIGHT_LEVEL, d.faceLightIntensity),
            defaultReminderMinuteOfDay = prefs.getInt(K_DEF_REMIND, d.defaultReminderMinuteOfDay),
            defaultActiveDaysMask = prefs.getInt(K_DEF_DAYS, d.defaultActiveDaysMask),
            defaultNagIntervalMinutes = prefs.getInt(K_DEF_NAG, d.defaultNagIntervalMinutes),
            defaultNagUntilMinuteOfDay = prefs.getInt(K_DEF_NAG_UNTIL, d.defaultNagUntilMinuteOfDay),
            videoResolution = prefs.getInt(K_VID_RES, d.videoResolution),
            videoFps = prefs.getInt(K_VID_FPS, d.videoFps),
            videoMillisPerPhoto = prefs.getInt(K_VID_MS, d.videoMillisPerPhoto),
            videoCrossfade = prefs.getBoolean(K_VID_FADE, d.videoCrossfade),
            videoNewestFirst = prefs.getBoolean(K_VID_REVERSE, d.videoNewestFirst),
            videoSaveToGallery = prefs.getBoolean(K_VID_GALLERY, d.videoSaveToGallery),
            acceptedTermsVersion = prefs.getInt(K_TERMS, d.acceptedTermsVersion),
            onboardingComplete = prefs.getBoolean(K_ONBOARDED, d.onboardingComplete)
        )
    }

    private fun write(s: AppSettings) {
        prefs.edit()
            .putString(K_THEME, s.themeMode.name)
            .putBoolean(K_DYNAMIC, s.dynamicColor)
            .putBoolean(K_AMOLED, s.amoledDark)
            .putInt(K_ACCENT, s.accentArgb)
            .putString(K_GHOST_MODE, s.ghostMode.name)
            .putFloat(K_GHOST_ALPHA, s.ghostAlpha)
            .putString(K_GHOST_REF, s.ghostReference.name)
            .putBoolean(K_GHOST_FLIP, s.ghostFlip)
            .putFloat(K_GHOST_SCALE, s.ghostScale)
            .putFloat(K_GHOST_DX, s.ghostOffsetX)
            .putFloat(K_GHOST_DY, s.ghostOffsetY)
            .putString(K_GRID, s.gridMode.name)
            .putBoolean(K_FACE_GUIDE, s.showFaceGuide)
            .putFloat(K_FACE_GUIDE_SCALE, s.faceGuideScale)
            .putBoolean(K_FRAME_BOX, s.showFrameBox)
            .putInt(K_COUNTDOWN, s.countdownSeconds)
            .putBoolean(K_REVIEW, s.reviewBeforeSave)
            .putBoolean(K_KEEP_ON, s.keepScreenOn)
            .putBoolean(K_MIRROR, s.mirrorFrontCamera)
            .putBoolean(K_HAPTICS, s.haptics)
            .putBoolean(K_FRONT, s.useFrontCamera)
            .putBoolean(K_GALLERY, s.saveCopyToGallery)
            .putBoolean(K_FACE_LIGHT, s.faceLight)
            .putFloat(K_FACE_LIGHT_LEVEL, s.faceLightIntensity)
            .putInt(K_DEF_REMIND, s.defaultReminderMinuteOfDay)
            .putInt(K_DEF_DAYS, s.defaultActiveDaysMask)
            .putInt(K_DEF_NAG, s.defaultNagIntervalMinutes)
            .putInt(K_DEF_NAG_UNTIL, s.defaultNagUntilMinuteOfDay)
            .putInt(K_VID_RES, s.videoResolution)
            .putInt(K_VID_FPS, s.videoFps)
            .putInt(K_VID_MS, s.videoMillisPerPhoto)
            .putBoolean(K_VID_FADE, s.videoCrossfade)
            .putBoolean(K_VID_REVERSE, s.videoNewestFirst)
            .putBoolean(K_VID_GALLERY, s.videoSaveToGallery)
            .putInt(K_TERMS, s.acceptedTermsVersion)
            .putBoolean(K_ONBOARDED, s.onboardingComplete)
            .apply()
    }

    private fun <T : Enum<T>> enumOf(name: String?, values: List<T>, fallback: T): T =
        values.firstOrNull { it.name == name } ?: fallback

    companion object {
        // 1.x stored a single "zoom_scalar" float; reuse it as the initial ghost scale
        // so an upgrading install keeps the overlay size it was already tuned to.
        private const val LEGACY_ZOOM = "zoom_scalar"

        private const val KEY_SKIPPED = "skipped_days"
        private const val K_THEME = "theme_mode"
        private const val K_DYNAMIC = "dynamic_color"
        private const val K_AMOLED = "amoled_dark"
        private const val K_ACCENT = "accent_argb"
        private const val K_GHOST_MODE = "ghost_mode"
        private const val K_GHOST_ALPHA = "ghost_alpha"
        private const val K_GHOST_REF = "ghost_reference"
        private const val K_GHOST_FLIP = "ghost_flip"
        private const val K_GHOST_SCALE = "ghost_scale"
        private const val K_GHOST_DX = "ghost_dx"
        private const val K_GHOST_DY = "ghost_dy"
        private const val K_GRID = "grid_mode"
        private const val K_FACE_GUIDE = "face_guide"
        private const val K_FACE_GUIDE_SCALE = "face_guide_scale"
        private const val K_FACE_LIGHT = "face_light"
        private const val K_FACE_LIGHT_LEVEL = "face_light_level"
        private const val K_FRAME_BOX = "frame_box"
        private const val K_COUNTDOWN = "countdown_seconds"
        private const val K_REVIEW = "review_before_save"
        private const val K_KEEP_ON = "keep_screen_on"
        private const val K_MIRROR = "mirror_front"
        private const val K_HAPTICS = "haptics"
        private const val K_FRONT = "use_front_camera"
        private const val K_GALLERY = "save_to_gallery"
        private const val K_DEF_REMIND = "default_reminder_minute"
        private const val K_DEF_DAYS = "default_active_days"
        private const val K_DEF_NAG = "default_nag_interval"
        private const val K_DEF_NAG_UNTIL = "default_nag_until"
        private const val K_VID_RES = "video_resolution"
        private const val K_VID_FPS = "video_fps"
        private const val K_VID_MS = "video_ms_per_photo"
        private const val K_VID_FADE = "video_crossfade"
        private const val K_TERMS = "accepted_terms_version"
        private const val K_ONBOARDED = "onboarding_complete"
        private const val K_VID_REVERSE = "video_newest_first"
        private const val K_VID_GALLERY = "video_save_gallery"
    }
}
