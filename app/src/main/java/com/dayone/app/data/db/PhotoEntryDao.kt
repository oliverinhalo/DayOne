package com.dayone.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoEntryDao {

    @Query("SELECT * FROM photo_entries WHERE projectId = :projectId ORDER BY dateEpochDay ASC")
    fun observeForProject(projectId: Long): Flow<List<PhotoEntry>>

    @Query("SELECT * FROM photo_entries WHERE projectId = :projectId ORDER BY dateEpochDay DESC LIMIT 1")
    suspend fun getLatest(projectId: Long): PhotoEntry?

    @Query("SELECT * FROM photo_entries WHERE projectId = :projectId AND dateEpochDay = :epochDay LIMIT 1")
    suspend fun getForDay(projectId: Long, epochDay: Long): PhotoEntry?

    @Query("SELECT COUNT(*) FROM photo_entries WHERE projectId = :projectId")
    suspend fun countForProject(projectId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PhotoEntry): Long

    @Delete
    suspend fun delete(entry: PhotoEntry)
}
