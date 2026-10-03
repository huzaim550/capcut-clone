package com.reelforge.core.model

import kotlinx.serialization.Serializable

/**
 * The whole arrangement of tracks for a project.
 *
 * [durationMs] is derived, never stored. The timeline ends when the last clip
 * ends; storing a separate duration invites the two to drift apart after a trim
 * or a move.
 */
@Serializable
data class Timeline(
    val tracks: List<Track> = emptyList(),
) {
    /** The end of the last-ending clip across every track. Zero when empty. */
    val durationMs: Long
        get() = tracks.maxOfOrNull { track -> track.clips.maxOfOrNull { it.timelineEndMs } ?: 0L } ?: 0L

    fun track(trackId: TrackId): Track? = tracks.firstOrNull { it.id == trackId }

    fun clip(clipId: ClipId): Clip? =
        tracks.asSequence().flatMap { it.clips.asSequence() }.firstOrNull { it.id == clipId }

    /** The track that owns [clipId], if any. */
    fun trackOf(clipId: ClipId): Track? = tracks.firstOrNull { track -> track.clips.any { it.id == clipId } }
}
