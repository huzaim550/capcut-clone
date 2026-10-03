package com.reelforge.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineDerivationTest {

    private fun clip(
        id: String,
        startMs: Long,
        sourceInMs: Long,
        sourceOutMs: Long,
        speed: Float = 1f,
    ) = Clip(
        id = ClipId(id),
        assetId = AssetId("asset"),
        timelineStartMs = startMs,
        sourceInMs = sourceInMs,
        sourceOutMs = sourceOutMs,
        speed = speed,
    )

    @Test
    fun `clip duration derives from the source trim at normal speed`() {
        val c = clip("c", startMs = 1_000, sourceInMs = 2_000, sourceOutMs = 6_000)

        assertEquals(4_000, c.sourceDurationMs)
        assertEquals(4_000, c.timelineDurationMs)
        assertEquals(5_000, c.timelineEndMs)
    }

    @Test
    fun `speed changes occupancy without changing the source trim`() {
        val fast = clip("c", startMs = 0, sourceInMs = 0, sourceOutMs = 4_000, speed = 2f)

        assertEquals(4_000, fast.sourceDurationMs)
        assertEquals(2_000, fast.timelineDurationMs)
        assertEquals(2_000, fast.timelineEndMs)
    }

    @Test
    fun `half speed doubles the timeline occupancy`() {
        val slow = clip("c", startMs = 0, sourceInMs = 0, sourceOutMs = 4_000, speed = 0.5f)

        assertEquals(8_000, slow.timelineDurationMs)
    }

    @Test
    fun `timeline duration is the end of the last-ending clip across tracks`() {
        val timeline = Timeline(
            tracks = listOf(
                Track(TrackId("v"), TrackKind.VIDEO, clips = listOf(clip("v1", 0, 0, 3_000))),
                Track(TrackId("a"), TrackKind.AUDIO, clips = listOf(clip("a1", 5_000, 0, 2_000))),
                Track(TrackId("o"), TrackKind.OVERLAY, clips = listOf(clip("o1", 1_000, 0, 1_000))),
            ),
        )

        // Audio ends at 7_000, later than video's 3_000.
        assertEquals(7_000, timeline.durationMs)
    }

    @Test
    fun `an empty timeline has zero duration`() {
        assertEquals(0, Timeline().durationMs)
        assertEquals(0, Timeline(tracks = listOf(Track(TrackId("v"), TrackKind.VIDEO))).durationMs)
    }

    @Test
    fun `clipAt treats the start as inside and the end as outside`() {
        val track = Track(TrackId("v"), TrackKind.VIDEO, clips = listOf(clip("v1", 1_000, 0, 2_000)))

        assertEquals(ClipId("v1"), track.clipAt(1_000)?.id)
        assertEquals(ClipId("v1"), track.clipAt(2_999)?.id)
        // A clip does not own the instant its successor begins.
        assertNull(track.clipAt(3_000))
        assertNull(track.clipAt(999))
    }

    @Test
    fun `lookups find tracks and clips by id and report absence`() {
        val timeline = Timeline(
            tracks = listOf(Track(TrackId("v"), TrackKind.VIDEO, clips = listOf(clip("v1", 0, 0, 1_000)))),
        )

        assertEquals(TrackKind.VIDEO, timeline.track(TrackId("v"))?.kind)
        assertEquals(ClipId("v1"), timeline.clip(ClipId("v1"))?.id)
        assertEquals(TrackId("v"), timeline.trackOf(ClipId("v1"))?.id)
        assertNull(timeline.track(TrackId("missing")))
        assertNull(timeline.clip(ClipId("missing")))
        assertNull(timeline.trackOf(ClipId("missing")))
    }
}
