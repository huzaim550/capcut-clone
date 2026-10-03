package com.reelforge.core.model

import kotlinx.serialization.Serializable

/**
 * An ordered lane of [Clip]s of a single [kind].
 *
 * Tracks are complete: a track's clips are listed in whatever order the editor
 * wrote them, and nothing may overlap within one track. Overlap policy when
 * placing a clip is an *edit* concern and lives in :editor:engine; the model
 * only states what a valid track looks like.
 */
@Serializable
data class Track(
    val id: TrackId,
    val kind: TrackKind,
    val isMuted: Boolean = false,
    val clips: List<Clip> = emptyList(),
) {
    /** The clip covering [positionMs] on this track, if any. */
    fun clipAt(positionMs: Long): Clip? =
        clips.firstOrNull { positionMs >= it.timelineStartMs && positionMs < it.timelineEndMs }
}
