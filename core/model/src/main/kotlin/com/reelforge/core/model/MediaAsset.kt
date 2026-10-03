package com.reelforge.core.model

import kotlinx.serialization.Serializable

/**
 * An imported source file the user can place on the timeline.
 *
 * This is metadata *about* the file, not the file itself. The bytes stay where
 * the platform put them and are referenced by [uri]; nothing here opens them.
 * Populating these fields (probing duration, dimensions, rotation) is
 * :core:media's job -- the model only fixes the shape so the rest of the app has
 * a stable contract to code against.
 *
 * [rotationDegrees] is recorded rather than baked into [width]/[height] because
 * phones report a landscape buffer plus a rotation flag for a portrait video.
 * Destructive "fixing" here would silently change the user's file on every read.
 */
@Serializable
data class MediaAsset(
    val id: AssetId,
    val uri: String,
    val displayName: String,
    val durationMs: Long,
    val width: Int? = null,
    val height: Int? = null,
    val rotationDegrees: Int = 0,
    /**
     * Frames per second, or `null` when the probe could not determine it. This
     * is one of the few `Double` values in the model: it is read from a probing
     * boundary, never used to accumulate timeline positions.
     */
    val frameRate: Double? = null,
    val hasAudio: Boolean = true,
    val hasVideo: Boolean = true,
)
