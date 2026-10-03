package com.reelforge.editor.engine

import com.reelforge.core.model.Clip
import com.reelforge.core.model.ClipId
import com.reelforge.core.model.Timeline
import com.reelforge.core.model.Track
import kotlin.math.roundToLong

/**
 * Pure queries and range surgery over a [Timeline].
 *
 * Nothing here mutates: every function takes a value and returns a value. That
 * is what makes the edit commands trivial to test -- there is no hidden state to
 * set up, and a returned timeline can be compared for equality against an
 * expected one.
 *
 * All positions are `Long` milliseconds, the model's one unit.
 */
object TimelineMath {

    /** The end of the last-ending clip across every track. Zero when empty. */
    fun timelineDuration(timeline: Timeline): Long = timeline.durationMs

    /** The instant after a clip's last frame. */
    fun clipEnd(clip: Clip): Long = clip.timelineEndMs

    /**
     * The topmost clip covering [positionMs], with the track that owns it.
     *
     * Tracks are ordered bottom-to-top: later tracks in [Timeline.tracks] paint
     * over earlier ones, so we search in reverse and take the first hit. A
     * `[start, end)` test means a clip does not own the instant its successor
     * begins, which is what stops two clips from both answering at a cut.
     */
    fun clipAt(timeline: Timeline, positionMs: Long): Pair<Track, Clip>? {
        for (index in timeline.tracks.indices.reversed()) {
            val track = timeline.tracks[index]
            track.clipAt(positionMs)?.let { return track to it }
        }
        return null
    }

    /**
     * Returns [clips] with the half-open range `[startMs, endMs)` vacated, ready
     * for a new clip to be placed there. [excludeClipId] is left untouched, so a
     * clip being moved can clear its own future range without deleting itself.
     *
     * Overwrite is the v1 policy because that is what a CapCut-style main track
     * does: placing footage does not ripple everything downstream. A clip that a
     * placement only partly covers is trimmed rather than discarded -- losing a
     * whole clip because an edge touched it would be surprising. A clip the
     * placement lands strictly inside is split in two, so the footage before and
     * after the insertion survives.
     *
     * Split remainders get deterministic derived ids (`"<base>~r@<endMs>"`)
     * rather than fresh UUIDs. Because a split only ever happens on a range
     * strictly inside a clip, the new right piece's start is unique within its
     * id's chain, so the scheme cannot collide -- and it keeps this function
     * pure, which a random id generator would not.
     */
    fun clearRange(
        clips: List<Clip>,
        startMs: Long,
        endMs: Long,
        excludeClipId: ClipId? = null,
    ): List<Clip> {
        if (endMs <= startMs) return clips
        return clips
            .flatMap { clip -> clearFrom(clip, startMs, endMs, excludeClipId) }
            .filter { it.sourceDurationMs > 0 }
            .sortedBy { it.timelineStartMs }
    }

    private fun clearFrom(
        clip: Clip,
        startMs: Long,
        endMs: Long,
        excludeClipId: ClipId?,
    ): List<Clip> {
        if (clip.id == excludeClipId) return listOf(clip)

        val clipStart = clip.timelineStartMs
        val clipEnd = clip.timelineEndMs

        return when {
            // Disjoint: nothing to do.
            clipEnd <= startMs || clipStart >= endMs -> listOf(clip)

            // Fully covered: the placement replaces it outright.
            clipStart >= startMs && clipEnd <= endMs -> emptyList()

            // The placement sits strictly inside: keep both shoulders.
            clipStart < startMs && clipEnd > endMs -> listOf(
                clip.copy(sourceOutMs = clip.sourceOutAt(startMs)),
                clip.copy(
                    id = ClipId("${clip.id.value}~r@$endMs"),
                    timelineStartMs = endMs,
                    sourceInMs = clip.sourceInAt(endMs),
                ),
            )

            // Overlaps the left edge: keep the head, trim the tail.
            clipStart < startMs -> listOf(clip.copy(sourceOutMs = clip.sourceOutAt(startMs)))

            // Overlaps the right edge: drop the head, keep the tail.
            else -> listOf(
                clip.copy(
                    timelineStartMs = endMs,
                    sourceInMs = clip.sourceInAt(endMs),
                ),
            )
        }
    }

    /** Source position that makes the clip's timeline end land on [timelineEndMs]. */
    private fun Clip.sourceOutAt(timelineEndMs: Long): Long =
        sourceInMs + ((timelineEndMs - timelineStartMs) * speed).roundToLong()

    /** Source position reached when the clip's timeline reaches [timelineStartMs]. */
    private fun Clip.sourceInAt(timelineStartMs: Long): Long =
        sourceInMs + ((timelineStartMs - this.timelineStartMs) * speed).roundToLong()
}
