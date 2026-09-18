package com.dayone.app.data

import android.content.Context
import android.net.Uri
import com.dayone.app.data.db.AppDatabase
import com.dayone.app.data.db.PhotoEntry
import com.dayone.app.data.db.Project
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Single source of truth for all project + photo data. Everything here is local-only:
 * Room database for metadata, plain files under app-specific external storage for images.
 * Nothing in this class ever touches the network.
 */
class PhotoRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val projectDao = db.projectDao()
    private val entryDao = db.photoEntryDao()

    // Root: /Android/data/com.dayone.app/files/DayOne  (app-specific, no storage permission needed)
    private val rootDir: File = File(context.getExternalFilesDir(null), "DayOne").apply { mkdirs() }

    fun observeProjects(): Flow<List<Project>> = projectDao.observeAll()

    fun observeEntries(projectId: Long): Flow<List<PhotoEntry>> = entryDao.observeForProject(projectId)

    suspend fun createProject(
        name: String,
        birthDateMillis: Long? = null,
        reminderMinuteOfDay: Int = 480,
        colorArgb: Int = 0xFF00E5A0.toInt()
    ): Project {
        val folderName = name.trim().replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "Project" }
        var candidate = folderName
        var suffix = 1
        while (folderDir(candidate).exists()) {
            candidate = "${folderName}_$suffix"
            suffix++
        }
        folderDir(candidate).mkdirs()

        val project = Project(
            name = name,
            folderName = candidate,
            startDateMillis = System.currentTimeMillis(),
            birthDateMillis = birthDateMillis,
            reminderMinuteOfDay = reminderMinuteOfDay,
            colorArgb = colorArgb
        )
        val id = projectDao.insert(project)
        return project.copy(id = id)
    }

    suspend fun updateProject(project: Project) = projectDao.update(project)

    suspend fun updateProjectName(projectId: Long, newName: String) {
        val project = projectDao.getById(projectId) ?: return
        projectDao.update(project.copy(name = newName))
    }

    suspend fun deleteProject(project: Project) {
        folderDir(project.folderName).deleteRecursively()
        projectDao.delete(project)
    }

    suspend fun getProject(id: Long): Project? = projectDao.getById(id)

    suspend fun getReminderEnabledProjects(): List<Project> = projectDao.getAllReminderEnabled()

    fun folderDir(folderName: String): File = File(rootDir, folderName)

    /** Where today's (or any given day's) photo file should live for a project, before it exists. */
    fun fileForDay(project: Project, date: LocalDate): File {
        val name = "%05d_%s.jpg".format(date.toEpochDay(), date)
        return File(folderDir(project.folderName), name)
    }

    suspend fun getLatestEntry(projectId: Long): PhotoEntry? = entryDao.getLatest(projectId)

    suspend fun getEntryForDate(projectId: Long, date: LocalDate): PhotoEntry? =
        entryDao.getForDay(projectId, date.toEpochDay())

    suspend fun hasCapturedToday(project: Project): Boolean {
        val today = LocalDate.now(ZoneId.systemDefault())
        return getEntryForDate(project.id, today) != null
    }

    suspend fun saveEntry(
        project: Project,
        date: LocalDate,
        file: File,
        face: android.graphics.RectF?
    ): PhotoEntry {
        val entry = PhotoEntry(
            projectId = project.id,
            dateEpochDay = date.toEpochDay(),
            filePath = file.absolutePath,
            faceLeft = face?.left,
            faceTop = face?.top,
            faceRight = face?.right,
            faceBottom = face?.bottom
        )
        val id = entryDao.insert(entry)
        return entry.copy(id = id)
    }

    suspend fun deleteEntry(entry: PhotoEntry) {
        val file = File(entry.filePath)
        if (file.exists()) file.delete()
        entryDao.delete(entry)
    }

    fun copyFileToProject(project: Project, sourceUri: Uri, context: Context, date: LocalDate): File? {
        val destFile = fileForDay(project, date)
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            null
        }
    }

    suspend fun countForProject(projectId: Long): Int = entryDao.countForProject(projectId)
}
