package com.reelforge.editor.engine

import com.reelforge.core.common.AppError
import com.reelforge.core.common.AppResult
import com.reelforge.core.model.Clip
import com.reelforge.core.model.ClipId
import com.reelforge.core.model.Project
import com.reelforge.core.model.Track
import com.reelforge.core.model.TrackId
import com.reelforge.core.model.Transform

/**
 * A single, validated change to a [Project].
 *
 * Commands are the unit of undo as well as the unit of editing: the engine
 * snapshots before each successful [apply], so "undo" never has to know how a
 * particular command worked.
 *
 * Two rules every command follows:
 *
 *  1. **`apply` is pure.** It returns a new [Project] and never touches the one
 *     it was given. A failed command therefore cannot have half-applied.
 *  2. **Illegal edits are [AppResult.Failure], not exceptions.** The reason is a
 *     sentence fit to show the user, in line with the rest of Reelforge's error
 *     model. The engine uses the failure to leave history untouched.
 *
 * Ids are supplied by the caller, not minted inside `apply`. That keeps the
 * commands deterministic and lets a test name every entity it expects to see.
 */
sealed interface EditCommand {
    fun apply(project: Project): AppResult<Project>
}

/** Adds an already-identified [track] to the project. */
data class AddTrack(val track: Track) : EditCommand {
    override fun apply(project: Project): AppResult<Project> {
        if (project.timeline.track(track.id) != null) {
            return fail("A track with that id already exists.")
        }
        return AppResult.Success(
            project.copy(timeline = project.timeline.copy(tracks = project.timeline.tracks + track)),
        )
    }
}

/** Removes a track and every clip on it. */
data class RemoveTrack(val trackId: TrackId) : EditCommand {
    override fun apply(project: Project): AppResult<Project> {
        if (project.timeline.track(trackId) == null) {
            return fail("That track no longer exists.")
        }
        return AppResult.Success(
            project.copy(
                timeline = project.timeline.copy(tracks = project.timeline.tracks.filterNot { it.id == trackId }),
            ),
        )
    }
}

/**
 * Places [clip] on [trackId], overwriting whatever occupied its range.
 * See [TimelineMath.clearRange] for the overwrite policy.
 */
data class AddClip(val trackId: TrackId, val clip: Clip) : EditCommand {
    override fun apply(project: Project): AppResult<Project> {
        val track = project.timeline.track(trackId) ?: return fail("Choose a track first.")
        if (project.timeline.clip(clip.id) != null) {
            return fail("A clip with that id already exists.")
        }
        validatePlacement(project, clip)?.let { return AppResult.Failure(it) }

        val vacated = TimelineMath.clearRange(track.clips, clip.timelineStartMs, clip.timelineEndMs)
        val updated = (vacated + clip).sortedBy { it.timelineStartMs }
        return AppResult.Success(project.replaceClipsOn(trackId, updated))
    }
}

/** Removes one clip, leaving the rest of its track untouched. */
data class RemoveClip(val clipId: ClipId) : EditCommand {
    override fun apply(project: Project): AppResult<Project> {
        val track = project.timeline.trackOf(clipId) ?: return fail("That clip no longer exists.")
        val updated = track.clips.filterNot { it.id == clipId }
        return AppResult.Success(project.replaceClipsOn(track.id, updated))
    }
}

/** Slides a clip along its track, overwriting whatever it lands on. */
data class MoveClip(val clipId: ClipId, val newStartMs: Long) : EditCommand {
    override fun apply(project: Project): AppResult<Project> {
        val track = project.timeline.trackOf(clipId) ?: return fail("That clip no longer exists.")
        val clip = track.clips.first { it.id == clipId }
        if (newStartMs < 0) return fail("A clip can't start before the beginning.")

        val endMs = newStartMs + clip.timelineDurationMs
        val vacated = TimelineMath.clearRange(track.clips, newStartMs, endMs, excludeClipId = clipId)
            .filterNot { it.id == clipId }
        val updated = (vacated + clip.copy(timelineStartMs = newStartMs)).sortedBy { it.timelineStartMs }
        return AppResult.Success(project.replaceClipsOn(track.id, updated))
    }
}

/**
 * Sets a clip's source trim. The clip's timeline position is fixed; changing the
 * trim changes its duration, so any range the new duration reaches into is
 * vacated under the same overwrite policy as a move.
 */
data class TrimClip(
    val clipId: ClipId,
    val newSourceInMs: Long,
    val newSourceOutMs: Long,
) : EditCommand {
    override fun apply(project: Project): AppResult<Project> {
        val track = project.timeline.trackOf(clipId) ?: return fail("That clip no longer exists.")
        val clip = track.clips.first { it.id == clipId }

        val candidate = clip.copy(sourceInMs = newSourceInMs, sourceOutMs = newSourceOutMs)
        validatePlacement(project, candidate)?.let { return AppResult.Failure(it) }

        val endMs = candidate.timelineStartMs + candidate.timelineDurationMs
        val vacated = TimelineMath.clearRange(
            track.clips,
            candidate.timelineStartMs,
            endMs,
            excludeClipId = clipId,
        ).filterNot { it.id == clipId }
        val updated = (vacated + candidate).sortedBy { it.timelineStartMs }
        return AppResult.Success(project.replaceClipsOn(track.id, updated))
    }
}

/** Changes playback rate; occupancy on the timeline changes with it. */
data class SetClipSpeed(val clipId: ClipId, val speed: Float) : EditCommand {
    override fun apply(project: Project): AppResult<Project> {
        val track = project.timeline.trackOf(clipId) ?: return fail("That clip no longer exists.")
        val clip = track.clips.first { it.id == clipId }
        if (speed <= 0f) return fail("Speed must be greater than zero.")

        val candidate = clip.copy(speed = speed)
        val endMs = candidate.timelineStartMs + candidate.timelineDurationMs
        val vacated = TimelineMath.clearRange(
            track.clips,
            candidate.timelineStartMs,
            endMs,
            excludeClipId = clipId,
        ).filterNot { it.id == clipId }
        val updated = (vacated + candidate).sortedBy { it.timelineStartMs }
        return AppResult.Success(project.replaceClipsOn(track.id, updated))
    }
}

/** Sets a clip's linear gain. */
data class SetClipVolume(val clipId: ClipId, val volume: Float) : EditCommand {
    override fun apply(project: Project): AppResult<Project> {
        val track = project.timeline.trackOf(clipId) ?: return fail("That clip no longer exists.")
        if (volume !in 0f..1f) return fail("Volume must be between 0% and 100%.")
        val clip = track.clips.first { it.id == clipId }
        return AppResult.Success(
            project.replaceClipsOn(track.id, track.clips.replacing(clip.copy(volume = volume))),
        )
    }
}

/** Replaces a clip's visual transform wholesale. */
data class SetClipTransform(val clipId: ClipId, val transform: Transform) : EditCommand {
    override fun apply(project: Project): AppResult<Project> {
        val track = project.timeline.trackOf(clipId) ?: return fail("That clip no longer exists.")
        val clip = track.clips.first { it.id == clipId }
        return AppResult.Success(
            project.replaceClipsOn(track.id, track.clips.replacing(clip.copy(transform = transform))),
        )
    }
}

/** Mutes or unmutes every clip on a track. */
data class SetTrackMuted(val trackId: TrackId, val isMuted: Boolean) : EditCommand {
    override fun apply(project: Project): AppResult<Project> {
        val track = project.timeline.track(trackId) ?: return fail("That track no longer exists.")
        return AppResult.Success(project.replaceTrack(track.copy(isMuted = isMuted)))
    }
}

// --- shared validation and structural helpers -------------------------------

private fun fail(reason: String): AppResult<Project> = AppResult.Failure(AppError.InvalidEdit(reason))

/**
 * Checks a clip against the project's own facts. Returns `null` when the clip is
 * legal. Asset bounds are enforced here rather than in the UI so no caller can
 * slip a trim past the end of the source and produce a decode failure later.
 */
private fun validatePlacement(project: Project, clip: Clip): AppError.InvalidEdit? {
    val asset = project.asset(clip.assetId)
        ?: return AppError.InvalidEdit("The source for this clip isn't in the project.")
    return when {
        clip.timelineStartMs < 0 -> AppError.InvalidEdit("A clip can't start before the beginning.")
        clip.sourceInMs < 0 -> AppError.InvalidEdit("The trim starts before the source does.")
        clip.sourceOutMs <= clip.sourceInMs -> AppError.InvalidEdit("A clip needs some length.")
        clip.sourceOutMs > asset.durationMs -> AppError.InvalidEdit("The trim runs past the end of the source.")
        clip.speed <= 0f -> AppError.InvalidEdit("Speed must be greater than zero.")
        // A very short source at a very high speed can round to zero timeline
        // milliseconds; placing it would produce an invisible, uneditable clip.
        clip.timelineDurationMs <= 0L -> AppError.InvalidEdit("A clip needs some length.")
        clip.volume !in 0f..1f -> AppError.InvalidEdit("Volume must be between 0% and 100%.")
        else -> null
    }
}

private fun Project.replaceTrack(track: Track): Project =
    copy(timeline = timeline.copy(tracks = timeline.tracks.map { if (it.id == track.id) track else it }))

private fun Project.replaceClipsOn(trackId: TrackId, clips: List<Clip>): Project =
    copy(
        timeline = timeline.copy(
            tracks = timeline.tracks.map { if (it.id == trackId) it.copy(clips = clips) else it },
        ),
    )

private fun List<Clip>.replacing(clip: Clip): List<Clip> =
    map { if (it.id == clip.id) clip else it }
