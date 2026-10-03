package com.reelforge.feature.home

import com.reelforge.core.common.AppError
import com.reelforge.core.common.AppResult
import com.reelforge.core.database.ProjectRepository
import com.reelforge.core.model.Project
import com.reelforge.core.model.ProjectId
import com.reelforge.core.model.ProjectSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An in-memory [ProjectRepository] for tests. It behaves like the real store for
 * the operations the browser uses, including updating the observable list, so the
 * ViewModel's reactive wiring is exercised rather than stubbed out.
 */
class FakeProjectRepository : ProjectRepository {

    val summaries = MutableStateFlow<List<ProjectSummary>>(emptyList())
    val saved = mutableListOf<Project>()
    val deleted = mutableListOf<ProjectId>()

    /** When set, the next save fails with this error. */
    var failSaveWith: AppError? = null

    override fun observeSummaries(): Flow<List<ProjectSummary>> = summaries

    override suspend fun load(id: ProjectId): AppResult<Project?> = AppResult.Success(null)

    override suspend fun save(project: Project): AppResult<Unit> {
        failSaveWith?.let { return AppResult.Failure(it) }
        saved += project
        summaries.value = listOf(
            ProjectSummary(project.id, project.name, project.createdAtMs, project.updatedAtMs),
        ) + summaries.value
        return AppResult.Success(Unit)
    }

    override suspend fun delete(id: ProjectId): AppResult<Unit> {
        deleted += id
        summaries.value = summaries.value.filterNot { it.id == id }
        return AppResult.Success(Unit)
    }
}
