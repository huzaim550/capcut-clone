package com.reelforge.editor.engine

import com.reelforge.core.model.AssetId
import com.reelforge.core.model.Clip
import com.reelforge.core.model.ClipId
import com.reelforge.core.model.MediaAsset
import com.reelforge.core.model.Project
import com.reelforge.core.model.ProjectId
import com.reelforge.core.model.Timeline
import com.reelforge.core.model.Track
import com.reelforge.core.model.TrackId
import com.reelforge.core.model.TrackKind

/**
 * Shared builders for engine tests. Ids are explicit and readable so a failing
 * assertion names the entity it is about instead of printing a random UUID.
 */
internal object Fixtures {

    val VIDEO = TrackId("video")
    val OVERLAY = TrackId("overlay")
    val AUDIO = TrackId("audio")

    /** A 10-second source, long enough not to constrain most trims. */
    val ASSET = MediaAsset(
        id = AssetId("asset"),
        uri = "content://media/1",
        displayName = "source.mp4",
        durationMs = 10_000,
    )

    fun clip(
        id: String,
        startMs: Long,
        sourceInMs: Long,
        sourceOutMs: Long,
        speed: Float = 1f,
    ) = Clip(
        id = ClipId(id),
        assetId = ASSET.id,
        timelineStartMs = startMs,
        sourceInMs = sourceInMs,
        sourceOutMs = sourceOutMs,
        speed = speed,
    )

    fun emptyProject(assets: List<MediaAsset> = listOf(ASSET)) = Project(
        id = ProjectId("project"),
        name = "Test",
        createdAtMs = 0,
        updatedAtMs = 0,
        assets = assets,
        timeline = Timeline(),
    )

    fun projectWith(vararg tracks: Track, assets: List<MediaAsset> = listOf(ASSET)) =
        emptyProject(assets).copy(timeline = Timeline(tracks = tracks.toList()))

    fun videoTrack(vararg clips: Clip) = Track(VIDEO, TrackKind.VIDEO, clips = clips.toList())

    fun audioTrack(vararg clips: Clip) = Track(AUDIO, TrackKind.AUDIO, clips = clips.toList())
}
