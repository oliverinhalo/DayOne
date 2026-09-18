package com.dayone.app.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Publishes files into the phone's *shared* storage, so they show up in the Gallery,
 * the Files app, and on a PC over USB - unlike the app's private folder, which only
 * DayOne itself can see.
 *
 * On Android 10+ this goes through MediaStore (no storage permission needed). On older
 * releases it writes to the public Movies/ or Pictures/ directory, which is why
 * WRITE_EXTERNAL_STORAGE is declared with maxSdkVersion="28" in the manifest.
 */
object MediaStoreSaver {

    data class Saved(val uri: Uri, val displayPath: String)

    fun saveVideo(context: Context, source: File, displayName: String, subFolder: String = "DayOne"): Saved? =
        save(
            context = context,
            source = source,
            displayName = displayName,
            mimeType = "video/mp4",
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            relativeDir = "${Environment.DIRECTORY_MOVIES}/$subFolder",
            legacyDir = Environment.DIRECTORY_MOVIES,
            subFolder = subFolder
        )

    fun saveImage(context: Context, source: File, displayName: String, subFolder: String = "DayOne"): Saved? =
        save(
            context = context,
            source = source,
            displayName = displayName,
            mimeType = "image/jpeg",
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            relativeDir = "${Environment.DIRECTORY_PICTURES}/$subFolder",
            legacyDir = Environment.DIRECTORY_PICTURES,
            subFolder = subFolder
        )

    /** Backups land in Documents/ so they survive an uninstall and are easy to find. */
    fun saveDocument(context: Context, source: File, displayName: String, mimeType: String, subFolder: String = "DayOne"): Saved? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            save(
                context = context,
                source = source,
                displayName = displayName,
                mimeType = mimeType,
                collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                relativeDir = "${Environment.DIRECTORY_DOCUMENTS}/$subFolder",
                legacyDir = Environment.DIRECTORY_DOCUMENTS,
                subFolder = subFolder
            )
        } else {
            legacySave(context, source, displayName, mimeType, Environment.DIRECTORY_DOCUMENTS, subFolder)
        }
    }

    private fun save(
        context: Context,
        source: File,
        displayName: String,
        mimeType: String,
        collection: Uri,
        relativeDir: String,
        legacyDir: String,
        subFolder: String
    ): Saved? {
        if (!source.exists()) return null

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return legacySave(context, source, displayName, mimeType, legacyDir, subFolder)
        }

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { input -> input.copyTo(out) }
            } ?: return null

            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
            Saved(uri, "$relativeDir/$displayName")
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }

    private fun legacySave(
        context: Context,
        source: File,
        displayName: String,
        mimeType: String,
        legacyDir: String,
        subFolder: String
    ): Saved? = try {
        val dir = File(Environment.getExternalStoragePublicDirectory(legacyDir), subFolder).apply { mkdirs() }
        val dest = File(dir, displayName)
        source.inputStream().use { input -> dest.outputStream().use { out -> input.copyTo(out) } }
        MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mimeType), null)
        Saved(Uri.fromFile(dest), dest.absolutePath)
    } catch (t: Throwable) {
        null
    }
}
