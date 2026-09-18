package com.dayone.app

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.dayone.app.data.db.Project

/**
 * Long-press the launcher icon to jump straight into a project's camera.
 *
 * The shortcuts track the projects that still need a photo today, so the first entry is
 * almost always the one you meant. Any failure here is cosmetic, so everything is
 * wrapped - a locked or rate-limited ShortcutManager must never take the app down.
 */
object ShortcutPublisher {

    private const val MAX_SHORTCUTS = 4

    fun publish(context: Context, projects: List<Project>, doneToday: Set<Long>) {
        runCatching {
            val ordered = projects
                .filterNot { it.archived }
                .sortedBy { doneToday.contains(it.id) }      // outstanding ones first
                .take(MAX_SHORTCUTS)

            val shortcuts = ordered.map { project ->
                ShortcutInfoCompat.Builder(context, "capture_${project.id}")
                    .setShortLabel(project.name.take(10))
                    .setLongLabel("Photo for ${project.name}")
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_camera))
                    .setIntent(
                        Intent(context, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra(MainActivity.EXTRA_OPEN_CAPTURE_PROJECT_ID, project.id)
                        }
                    )
                    .build()
            }
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            if (shortcuts.isNotEmpty()) ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
        }
    }
}
