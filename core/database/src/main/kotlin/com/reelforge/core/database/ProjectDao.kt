package com.reelforge.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Query surface over the project table.
 *
 * Deliberately narrow: everything the app needs is expressed here, so no caller
 * has to open a transaction or write SQL. [observeSummaries] selects only the
 * columns a list row needs; the heavy [ProjectEntity.documentJson] is fetched
 * only when a specific project is opened.
 */
@Dao
interface ProjectDao {

    @Query("SELECT id, name, createdAtMs, updatedAtMs FROM projects ORDER BY updatedAtMs DESC")
    fun observeSummaries(): Flow<List<ProjectSummaryRow>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProjectEntity?

    /** Insert or replace. Saving is idempotent, so there is no separate update. */
    @Upsert
    suspend fun upsert(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}
