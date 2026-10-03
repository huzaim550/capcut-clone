package com.reelforge.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * How a [com.reelforge.core.model.Project] is stored.
 *
 * The columns duplicate fields that also live inside [documentJson]. That is
 * intentional: the columns exist so the browser can list, sort, and search
 * projects *without* decoding every timeline, while [documentJson] is the
 * lossless copy of the whole model (assets, tracks, clips) produced by
 * `ProjectJson`. Reading a project always goes through the JSON, so there is one
 * source of truth for the edit and the columns are a queryable index over it.
 *
 * Keeping the model as one document also means the schema does not have to grow
 * a table every time the timeline gains a concept -- a very common source of
 * migration pain in editors.
 */
@Entity(
    tableName = "projects",
    indices = [Index("updatedAtMs")],
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val schemaVersion: Int,
    val documentJson: String,
)
