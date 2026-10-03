package com.reelforge.core.database

import com.reelforge.core.common.AppResult
import com.reelforge.core.common.map
import com.reelforge.core.model.Project
import com.reelforge.core.model.ProjectJson

/**
 * Translation between the domain [Project] and its stored [ProjectEntity].
 *
 * The whole model is serialized through [ProjectJson], which is what makes these
 * functions total: any project the model can represent round-trips through the
 * database. The columns are a queryable index over that document; a project is
 * always reconstituted by decoding the document, never by reading the columns.
 */
internal fun Project.toEntity(): AppResult<ProjectEntity> =
    ProjectJson.encode(this).map { json ->
        ProjectEntity(
            id = id.value,
            name = name,
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs,
            schemaVersion = schemaVersion,
            documentJson = json,
        )
    }

internal fun ProjectEntity.toProject(): AppResult<Project> =
    ProjectJson.decode(documentJson)
