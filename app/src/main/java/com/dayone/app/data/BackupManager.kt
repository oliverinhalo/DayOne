package com.dayone.app.data

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.dayone.app.data.db.PhotoEntry
import com.dayone.app.data.db.Project
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Whole-app backup and restore as a single .zip, plus bulk import of loose photos.
 *
 * Photos and the database live in app-private storage, which Android deletes when the
 * app is uninstalled - and an APK signed with a different key can only be installed by
 * uninstalling first. A backup file that lives in Documents/DayOne (shared storage,
 * outside the app sandbox) is what makes a project survive that.
 */
object BackupManager {

    private const val MANIFEST = "manifest.json"
    private const val PHOTO_DIR = "photos"
    private const val FORMAT_VERSION = 2

    data class BackupResult(val file: File, val projects: Int, val photos: Int, val bytes: Long)
    data class RestoreResult(
        val projectsCreated: Int,
        val projectsMerged: Int,
        val photosAdded: Int,
        val photosSkipped: Int,
        val error: String? = null
    )

    // ---------------------------------------------------------------- export

    /** Writes the backup into [outFile] (usually the cache dir); publish it afterwards. */
    suspend fun export(
        repo: PhotoRepository,
        outFile: File,
        onProgress: (Float) -> Unit = {}
    ): BackupResult {
        val projects = repo.getAllProjects()
        val allEntries = repo.getAllEntries()
        val entriesByProject = allEntries.groupBy { it.projectId }

        val root = JSONObject()
        root.put("formatVersion", FORMAT_VERSION)
        root.put("exportedAtMillis", System.currentTimeMillis())

        val projectsJson = JSONArray()
        var photoCount = 0
        val total = allEntries.size.coerceAtLeast(1)

        ZipOutputStream(outFile.outputStream().buffered()).use { zip ->
            projects.forEach { project ->
                val entries = entriesByProject[project.id].orEmpty()
                val pj = projectToJson(project)
                val entriesJson = JSONArray()

                entries.forEach { entry ->
                    val source = File(entry.filePath)
                    val zipPath = "$PHOTO_DIR/${project.folderName}/${source.name}"
                    if (source.exists()) {
                        zip.putNextEntry(ZipEntry(zipPath))
                        source.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                    entriesJson.put(entryToJson(entry, zipPath))
                    photoCount++
                    onProgress(photoCount.toFloat() / total)
                }
                pj.put("entries", entriesJson)
                projectsJson.put(pj)
            }
            root.put("projects", projectsJson)

            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(root.toString(2).toByteArray())
            zip.closeEntry()
        }

        return BackupResult(outFile, projects.size, photoCount, outFile.length())
    }

    // ---------------------------------------------------------------- import

    /**
     * Restores a backup zip. Projects are matched by folder name so restoring twice
     * merges instead of duplicating, and days that already have a photo are left alone
     * unless [overwriteExisting] is set.
     */
    suspend fun restore(
        context: Context,
        repo: PhotoRepository,
        zipUri: Uri,
        overwriteExisting: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): RestoreResult {
        // Stage the zip locally first: a ZipInputStream over a content:// stream can only
        // be read once, and we need two passes (manifest, then photos).
        val staged = File(context.cacheDir, "restore_${System.currentTimeMillis()}.zip")
        try {
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                staged.outputStream().use { out -> input.copyTo(out) }
            } ?: return RestoreResult(0, 0, 0, 0, "Could not open that file")

            val manifest = readManifest(staged)
                ?: return RestoreResult(0, 0, 0, 0, "No DayOne manifest inside that zip")

            var created = 0
            var merged = 0
            var added = 0
            var skipped = 0

            val existing = repo.getAllProjects()
            val projectsJson = manifest.optJSONArray("projects") ?: JSONArray()

            // projectFolder -> target project
            val targets = HashMap<String, Project>()
            for (i in 0 until projectsJson.length()) {
                val pj = projectsJson.getJSONObject(i)
                val folder = pj.optString("folderName")
                val name = pj.optString("name", folder)
                val match = existing.firstOrNull { it.folderName == folder || it.name == name }
                if (match != null) {
                    targets[folder] = match
                    merged++
                } else {
                    val project = repo.createProject(
                        name = name.ifBlank { "Restored" },
                        birthDateMillis = pj.optLongOrNull("birthDateMillis"),
                        reminderMinuteOfDay = pj.optInt("reminderMinuteOfDay", 480),
                        colorArgb = pj.optInt("colorArgb", 0xFF00E5A0.toInt()),
                        activeDaysMask = pj.optInt("activeDaysMask", 0b1111111),
                        nagEnabled = pj.optBoolean("nagEnabled", true),
                        nagIntervalMinutes = pj.optInt("nagIntervalMinutes", 45),
                        nagUntilMinuteOfDay = pj.optInt("nagUntilMinuteOfDay", 1320),
                        reminderEnabled = pj.optBoolean("reminderEnabled", true)
                    )
                    targets[folder] = project
                    created++
                }
            }

            // Second pass: stream photos out of the zip into the right project folders.
            val wanted = HashMap<String, Pair<Project, JSONObject>>()
            for (i in 0 until projectsJson.length()) {
                val pj = projectsJson.getJSONObject(i)
                val folder = pj.optString("folderName")
                val project = targets[folder] ?: continue
                val entries = pj.optJSONArray("entries") ?: continue
                for (e in 0 until entries.length()) {
                    val ej = entries.getJSONObject(e)
                    wanted[ej.optString("zipPath")] = project to ej
                }
            }

            val totalWanted = wanted.size.coerceAtLeast(1)
            var processed = 0

            ZipInputStream(staged.inputStream().buffered()).use { zin ->
                var zipEntry: ZipEntry? = zin.nextEntry
                while (zipEntry != null) {
                    val target = wanted[zipEntry.name]
                    if (target != null && !zipEntry.isDirectory) {
                        val (project, ej) = target
                        val epochDay = ej.optLong("dateEpochDay", Long.MIN_VALUE)
                        if (epochDay != Long.MIN_VALUE) {
                            val date = LocalDate.ofEpochDay(epochDay)
                            val already = repo.getEntryForDate(project.id, date)
                            if (already != null && !overwriteExisting) {
                                skipped++
                            } else {
                                val dest = repo.fileForDay(project, date)
                                dest.parentFile?.mkdirs()
                                dest.outputStream().use { out -> zin.copyTo(out) }
                                val saved = repo.saveEntry(project, date, dest, null, ej.optStringOrNull("note"))
                                if (ej.optBoolean("favorite", false)) repo.setFavorite(saved, true)
                                added++
                            }
                        }
                        processed++
                        onProgress(processed.toFloat() / totalWanted)
                    }
                    zin.closeEntry()
                    zipEntry = zin.nextEntry
                }
            }

            return RestoreResult(created, merged, added, skipped)
        } catch (t: Throwable) {
            return RestoreResult(0, 0, 0, 0, t.message ?: "Restore failed")
        } finally {
            staged.delete()
        }
    }

    /**
     * Bulk-imports loose image files into a project, working out each photo's date from
     * the DayOne filename pattern, then any yyyy-MM-dd in the name, then EXIF capture
     * time. This is how photos from an older install (or another phone) come back.
     */
    suspend fun importImages(
        context: Context,
        repo: PhotoRepository,
        project: Project,
        uris: List<Uri>,
        overwriteExisting: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): RestoreResult {
        var added = 0
        var skipped = 0
        uris.forEachIndexed { index, uri ->
            val date = dateForImage(context, uri)
            if (date == null) {
                skipped++
            } else {
                val already = repo.getEntryForDate(project.id, date)
                if (already != null && !overwriteExisting) {
                    skipped++
                } else {
                    val file = repo.copyFileToProject(project, uri, context, date)
                    if (file != null) {
                        repo.saveEntry(project, date, file, null)
                        added++
                    } else {
                        skipped++
                    }
                }
            }
            onProgress((index + 1).toFloat() / uris.size.coerceAtLeast(1))
        }
        return RestoreResult(0, 1, added, skipped)
    }

    /** Best-effort date for an imported image. */
    fun dateForImage(context: Context, uri: Uri): LocalDate? {
        val name = displayName(context, uri)
        if (name != null) {
            // DayOne's own naming: 19876_2024-06-01.jpg
            Regex("""(\d{4}-\d{2}-\d{2})""").find(name)?.let { match ->
                runCatching { return LocalDate.parse(match.groupValues[1]) }
            }
            // Camera naming: IMG_20240601_083000.jpg
            Regex("""(20\d{2})(\d{2})(\d{2})""").find(name)?.let { match ->
                runCatching {
                    return LocalDate.of(
                        match.groupValues[1].toInt(),
                        match.groupValues[2].toInt(),
                        match.groupValues[3].toInt()
                    )
                }
            }
        }
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                raw?.let {
                    LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")).toLocalDate()
                }
            }
        }.getOrNull()
    }

    private fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        } ?: uri.lastPathSegment
    }.getOrNull() ?: uri.lastPathSegment

    private fun readManifest(zipFile: File): JSONObject? {
        ZipInputStream(zipFile.inputStream().buffered()).use { zin ->
            var entry: ZipEntry? = zin.nextEntry
            while (entry != null) {
                if (entry.name == MANIFEST) {
                    val text = zin.readBytes().toString(Charsets.UTF_8)
                    return runCatching { JSONObject(text) }.getOrNull()
                }
                zin.closeEntry()
                entry = zin.nextEntry
            }
        }
        return null
    }

    private fun projectToJson(p: Project) = JSONObject().apply {
        put("name", p.name)
        put("folderName", p.folderName)
        put("startDateMillis", p.startDateMillis)
        p.birthDateMillis?.let { put("birthDateMillis", it) }
        put("reminderMinuteOfDay", p.reminderMinuteOfDay)
        put("reminderEnabled", p.reminderEnabled)
        put("colorArgb", p.colorArgb)
        put("createdAtMillis", p.createdAtMillis)
        put("activeDaysMask", p.activeDaysMask)
        put("nagEnabled", p.nagEnabled)
        put("nagIntervalMinutes", p.nagIntervalMinutes)
        put("nagUntilMinuteOfDay", p.nagUntilMinuteOfDay)
        p.secondReminderMinuteOfDay?.let { put("secondReminderMinuteOfDay", it) }
        put("archived", p.archived)
        put("autoCropFace", p.autoCropFace)
        put("headFraction", p.headFraction.toDouble())
    }

    private fun entryToJson(e: PhotoEntry, zipPath: String) = JSONObject().apply {
        put("dateEpochDay", e.dateEpochDay)
        put("zipPath", zipPath)
        put("capturedAtMillis", e.capturedAtMillis)
        e.note?.let { put("note", it) }
        put("favorite", e.favorite)
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
}
