package com.dayone.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY createdAtMillis ASC")
    fun observeAll(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): Project?

    @Query("SELECT * FROM projects WHERE reminderEnabled = 1")
    suspend fun getAllReminderEnabled(): List<Project>

    @Insert
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)
}
