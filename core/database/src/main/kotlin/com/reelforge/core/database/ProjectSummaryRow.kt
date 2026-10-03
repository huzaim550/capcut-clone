package com.reelforge.core.database

import com.reelforge.core.model.ProjectId
import com.reelforge.core.model.ProjectSummary

/**
 * A row of the project list, shaped to match the summary query's columns.
 *
 * Room cannot construct [ProjectSummary] directly because its `id` is a
 * [ProjectId] value class, and teaching Room to convert it would couple the
 * domain type to the persistence layer. A plain string row that is mapped at the
 * boundary keeps both clean.
 */
data class ProjectSummaryRow(
    val id: String,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

fun ProjectSummaryRow.toSummary(): ProjectSummary = ProjectSummary(
    id = ProjectId(id),
    name = name,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
)
