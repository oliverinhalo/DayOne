package com.dayone.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoEntryDao {

    @Query("SELECT * FROM photo_entries WHERE projectId = :projectId ORDER BY dateEpochDay ASC")
    fun observeForProject(projectId: Long): Flow<List<PhotoEntry>>

    @Query("SELECT * FROM photo_entries ORDER BY projectId ASC, dateEpochDay ASC")
    fun observeAll(): Flow<List<PhotoEntry>>

    @Query("SELECT * FROM photo_entries WHERE projectId = :projectId ORDER BY dateEpochDay ASC")
    suspend fun getForProject(projectId: Long): List<PhotoEntry>

    @Query("SELECT * FROM photo_entries ORDER BY projectId ASC, dateEpochDay ASC")
    suspend fun getAll(): List<PhotoEntry>

    @Query("SELECT * FROM photo_entries WHERE projectId = :projectId ORDER BY dateEpochDay DESC LIMIT 1")
    suspend fun getLatest(projectId: Long): PhotoEntry?

    @Query("SELECT * FROM photo_entries WHERE projectId = :projectId ORDER BY dateEpochDay ASC LIMIT 1")
    suspend fun getFirst(projectId: Long): PhotoEntry?

    @Query("SELECT * FROM photo_entries WHERE projectId = :projectId AND dateEpochDay = :epochDay LIMIT 1")
    suspend fun getForDay(projectId: Long, epochDay: Long): PhotoEntry?

    @Query("SELECT COUNT(*) FROM photo_entries WHERE projectId = :projectId")
    suspend fun countForProject(projectId: Long): Int

    @Query("SELECT COUNT(*) FROM photo_entries WHERE projectId = :projectId")
    fun observeCountForProject(projectId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PhotoEntry): Long

    @Update
    suspend fun update(entry: PhotoEntry)

    @Delete
    suspend fun delete(entry: PhotoEntry)
}
