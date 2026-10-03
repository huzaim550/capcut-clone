package com.reelforge.core.database

import com.reelforge.core.common.AppResult
import com.reelforge.core.model.Project
import com.reelforge.core.model.ProjectId
import com.reelforge.core.model.ProjectSummary
import kotlinx.coroutines.flow.Flow

/**
 * What the rest of the app is allowed to know about project storage.
 *
 * Features depend on this interface rather than on [ProjectStore] or Room, which
 * keeps two things true: a feature can be unit-tested against a cheap in-memory
 * fake, and swapping the persistence mechanism later does not ripple outward.
 */
interface ProjectRepository {

    /** Project list rows, newest first, refreshed whenever the data changes. */
    fun observeSummaries(): Flow<List<ProjectSummary>>

    /** Loads a project, or `Success(null)` when it does not exist. */
    suspend fun load(id: ProjectId): AppResult<Project?>

    /** Inserts or replaces [project]. Saving is idempotent. */
    suspend fun save(project: Project): AppResult<Unit>

    /** Deletes by id. Deleting a missing project is not an error. */
    suspend fun delete(id: ProjectId): AppResult<Unit>
}
