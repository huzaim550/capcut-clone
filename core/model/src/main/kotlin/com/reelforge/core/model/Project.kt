package com.reelforge.core.model

import kotlinx.serialization.Serializable

/**
 * The root aggregate: everything the app persists about one edit.
 *
 * [schemaVersion] is stored even though it is always [CURRENT_SCHEMA_VERSION]
 * today. It costs one integer and buys a migration hook: when the shape changes
 * the decoder can branch on the version it reads instead of guessing.
 */
@Serializable
data class Project(
    val id: ProjectId,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    /**
     * Every source file this project uses. Clips reference assets by id, so the
     * list must travel with the project or a saved edit could not be reopened:
     * the clip would name an asset nothing remembers.
     */
    val assets: List<MediaAsset> = emptyList(),
    val timeline: Timeline = Timeline(),
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
) {
    fun asset(assetId: AssetId): MediaAsset? = assets.firstOrNull { it.id == assetId }

    companion object {
        /** Bump when the persisted shape changes incompatibly. */
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}
