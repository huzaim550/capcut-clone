package com.reelforge.core.database

import com.reelforge.core.common.AppError
import com.reelforge.core.common.AppResult
import com.reelforge.core.common.DefaultDispatcherProvider
import com.reelforge.core.common.DispatcherProvider
import com.reelforge.core.model.Project
import com.reelforge.core.model.ProjectId
import com.reelforge.core.model.ProjectSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * The app's one door to project persistence.
 *
 * It speaks only domain types and [AppResult], and it does every database call on
 * the IO dispatcher so no caller accidentally touches disk on the main thread.
 *
 * "Not found" is modelled as `Success(null)`, not a failure. A deleted project is
 * a normal outcome the browser handles by showing an empty list; turning it into
 * an error would force every caller to special-case a message the user does not
 * need to see. Genuine failures -- a corrupt document, a disk error -- are
 * [AppError.Storage].
 */
class ProjectStore(
    private val dao: ProjectDao,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : ProjectRepository {

    /** Project list rows, newest first, refreshed whenever the table changes. */
    override fun observeSummaries(): Flow<List<ProjectSummary>> =
        dao.observeSummaries()
            .map { rows -> rows.map(ProjectSummaryRow::toSummary) }
            .flowOn(dispatchers.io)

    /** Loads a project, or `Success(null)` when it does not exist. */
    override suspend fun load(id: ProjectId): AppResult<Project?> = withContext(dispatchers.io) {
        val entity = storageResult("ProjectStore.load") { dao.getById(id.value) }
        when (entity) {
            is AppResult.Failure -> entity
            is AppResult.Success -> entity.value?.toProject() ?: AppResult.Success(null)
        }
    }

    /** Inserts or replaces [project]. Saving is idempotent. */
    override suspend fun save(project: Project): AppResult<Unit> = withContext(dispatchers.io) {
        when (val entity = project.toEntity()) {
            is AppResult.Failure -> entity
            is AppResult.Success -> storageResult("ProjectStore.save") { dao.upsert(entity.value) }
        }
    }

    /** Deletes by id. Deleting a missing project is not an error. */
    override suspend fun delete(id: ProjectId): AppResult<Unit> = withContext(dispatchers.io) {
        storageResult("ProjectStore.delete") { dao.deleteById(id.value) }
    }
}

/**
 * Runs [block], turning any database throwable into [AppError.Storage].
 *
 * Cancellation is rethrown: it is control flow, and swallowing it into a storage
 * error would strand a cancelled load. This mirrors the boundary helpers in
 * `ProjectJson` and `runCatchingApp`.
 */
private inline fun <T> storageResult(context: String, block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (t: Throwable) {
    AppResult.Failure(AppError.Storage(detail = context, cause = t))
}
