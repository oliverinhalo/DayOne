package com.dayone.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One captured photo for a given project/day. dateEpochDay lets us guarantee at most
 * one entry per calendar day per project, and makes "did I miss a day" trivial to check.
 */
@Entity(
    tableName = "photo_entries",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId", "dateEpochDay"], unique = true)]
)
data class PhotoEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val projectId: Long,

    // LocalDate.toEpochDay() - one row per calendar day
    val dateEpochDay: Long,

    // Absolute file path of the final, auto-cropped & centered photo
    val filePath: String,

    // Face detection bounding box at capture time, stored so we can re-crop later if desired
    val faceLeft: Float? = null,
    val faceTop: Float? = null,
    val faceRight: Float? = null,
    val faceBottom: Float? = null,

    val capturedAtMillis: Long = System.currentTimeMillis()
)
