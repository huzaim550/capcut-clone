package com.reelforge.core.model

/**
 * The small amount of a [Project] the browser needs: enough to draw a row and
 * sort it, and nothing more.
 *
 * This exists so the project list does not have to deserialize every project's
 * full timeline just to show names. It is a projection, not an entity -- the
 * persisted truth is always the [Project] it summarizes.
 */
data class ProjectSummary(
    val id: ProjectId,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
