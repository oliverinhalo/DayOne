package com.dayone.app.data

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.dayone.app.data.db.Project
import java.io.File

/**
 * Copies photos already sitting in a project's private folder out into the gallery, at
 * `Pictures/DayOne/<Project>`.
 *
 * The "save a copy to Gallery" setting only ever applied at the moment a photo was taken,
 * so anything imported from elsewhere - or shot before the setting was turned on - stayed
 * invisible to the Gallery and Files apps. This walks a project and fills in whatever is
 * missing, skipping files that are already there so it is safe to run repeatedly.
 */
object GalleryMigrator {

    data class Result(val copied: Int, val alreadyThere: Int, val failed: Int, val folder: String)

    fun folderFor(project: Project): String = "DayOne/${project.folderName}"

    fun galleryPathFor(project: Project): String =
        "${Environment.DIRECTORY_PICTURES}/${folderFor(project)}"

    /**
     * @param onProgress 0f..1f across [entries]
     */
    fun migrate(
        context: Context,
        project: Project,
        files: List<File>,
        onProgress: (Float) -> Unit = {}
    ): Result {
        val subFolder = folderFor(project)
        val existing = existingNames(context, subFolder)

        var copied = 0
        var alreadyThere = 0
        var failed = 0

        files.forEachIndexed { index, file ->
            val displayName = file.name
            when {
                !file.exists() -> failed++
                existing.contains(displayName) -> alreadyThere++
                else -> {
                    val saved = MediaStoreSaver.saveImage(context, file, displayName, subFolder)
                    if (saved != null) copied++ else failed++
                }
            }
            onProgress((index + 1).toFloat() / files.size.coerceAtLeast(1))
        }

        return Result(copied, alreadyThere, failed, galleryPathFor(project))
    }

    /**
     * Names already present in that gallery folder. Without this, re-running the copy
     * would stack up "photo(1).jpg", "photo(2).jpg" duplicates in the user's gallery.
     */
    private fun existingNames(context: Context, subFolder: String): Set<String> {
        val names = mutableSetOf<String>()
        runCatching {
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = "${Environment.DIRECTORY_PICTURES}/$subFolder/"
                context.contentResolver.query(
                    collection,
                    projection,
                    "${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                    arrayOf(relativePath),
                    null
                )?.use { cursor ->
                    val column = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    while (cursor.moveToNext()) names.add(cursor.getString(column))
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), subFolder)
                dir.listFiles()?.forEach { names.add(it.name) }
            }
        }
        return names
    }
}
