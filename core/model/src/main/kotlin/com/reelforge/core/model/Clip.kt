package com.reelforge.core.model

import kotlin.math.roundToLong
import kotlinx.serialization.Serializable

/**
 * One placed slice of a [MediaAsset] on a [Track].
 *
 * A clip has **two independent time axes**, and conflating them is the classic
 * timeline bug:
 *
 *  * [timelineStartMs] is where the clip sits on the project timeline.
 *  * [sourceInMs] / [sourceOutMs] are the trim within the source asset.
 *
 * A clip trimmed to the middle of a long recording therefore has a small
 * [timelineDurationMs] even though it sits late on the timeline. Both are
 * stored explicitly rather than as a single offset so a move does not disturb
 * the trim, and a trim does not disturb the position.
 *
 * [speed] changes how long the clip occupies the timeline, but never the source
 * range it reads: a 4s range at 2x speed occupies 2s on the timeline. This is
 * why duration is derived here rather than stored -- a stored duration can
 * disagree with the trim and the speed, and then nothing knows which is right.
 */
@Serializable
data class Clip(
    val id: ClipId,
    val assetId: AssetId,
    /** Position of the clip's first frame on the project timeline. */
    val timelineStartMs: Long,
    /** First source frame read, relative to the start of the asset. */
    val sourceInMs: Long,
    /** One past the last source frame read, relative to the start of the asset. */
    val sourceOutMs: Long,
    /** Playback rate; must be strictly positive. 1.0 is normal speed. */
    val speed: Float = 1f,
    /** Linear gain in `0.0..1.0` for now; >1.0 boost is a later feature. */
    val volume: Float = 1f,
    val transform: Transform = Transform(),
) {
    /** How much of the source this clip reads. */
    val sourceDurationMs: Long get() = sourceOutMs - sourceInMs

    /** How much timeline the clip occupies once [speed] is applied. */
    val timelineDurationMs: Long get() = (sourceDurationMs / speed).roundToLong()

    /** One past the clip's last frame on the project timeline. */
    val timelineEndMs: Long get() = timelineStartMs + timelineDurationMs
}
