package com.reelforge.editor.engine

import com.reelforge.core.model.ClipId
import com.reelforge.core.model.Timeline
import com.reelforge.core.model.Track
import com.reelforge.core.model.TrackId
import com.reelforge.core.model.TrackKind
import com.reelforge.editor.engine.Fixtures.clip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineMathTest {

    @Test
    fun `clearRange leaves disjoint clips untouched`() {
        val clips = listOf(clip("a", 0, 0, 1_000), clip("b", 5_000, 0, 1_000))

        val result = TimelineMath.clearRange(clips, 2_000, 3_000)

        assertEquals(clips, result)
    }

    @Test
    fun `clearRange removes a fully covered clip`() {
        val clips = listOf(clip("a", 0, 0, 3_000), clip("b", 5_000, 0, 1_000))

        val result = TimelineMath.clearRange(clips, 0, 3_000)

        assertEquals(listOf(clip("b", 5_000, 0, 1_000)), result)
    }

    @Test
    fun `clearRange trims a clip hit on its right side`() {
        val clips = listOf(clip("a", 0, 0, 4_000))

        // The range covers [3000, 5000); the clip keeps [0, 3000).
        val result = TimelineMath.clearRange(clips, 3_000, 5_000)

        assertEquals(listOf(clip("a", 0, 0, 3_000)), result)
    }

    @Test
    fun `clearRange trims a clip hit on its left side`() {
        val clips = listOf(clip("a", 2_000, 0, 4_000))

        // Range [0, 3000) removes the first second of the clip's timeline span.
        val result = TimelineMath.clearRange(clips, 0, 3_000)

        // Clip now starts at 3000; its source has advanced by one second.
        assertEquals(listOf(clip("a", 3_000, 1_000, 4_000)), result)
    }

    @Test
    fun `clearRange splits a clip the range lands strictly inside`() {
        val clips = listOf(clip("a", 0, 0, 4_000))

        val result = TimelineMath.clearRange(clips, 1_000, 2_000)

        // The head keeps the original id; the tail gets a deterministic derived id.
        assertEquals(
            listOf(
                clip("a", 0, 0, 1_000),
                clip("a~r@2000", 2_000, 2_000, 4_000),
            ),
            result,
        )
    }

    @Test
    fun `clearRange excludes the clip being moved so it does not delete itself`() {
        val clips = listOf(clip("a", 0, 0, 1_000), clip("b", 3_000, 0, 1_000))

        // b is moving onto a's range; only a should be cleared.
        val result = TimelineMath.clearRange(clips, 0, 1_000, excludeClipId = ClipId("b"))

        assertEquals(listOf(clip("b", 3_000, 0, 1_000)), result)
    }

    @Test
    fun `clearRange respects speed when trimming`() {
        // At 2x, one second of source occupies half a second of timeline.
        val clips = listOf(clip("a", 0, 0, 4_000, speed = 2f))

        // Clip occupies [0, 2000). Clearing [1000, 2000) keeps [0,1000),
        // which is 1000ms of timeline = 2000ms of source.
        val result = TimelineMath.clearRange(clips, 1_000, 2_000)

        assertEquals(listOf(clip("a", 0, 0, 2_000, speed = 2f)), result)
    }

    @Test
    fun `clipAt returns the topmost clip at a position`() {
        val bottom = Track(TrackId("bottom"), TrackKind.VIDEO, clips = listOf(clip("b", 0, 0, 1_000)))
        val top = Track(TrackId("top"), TrackKind.OVERLAY, clips = listOf(clip("t", 0, 0, 1_000)))
        val timeline = Timeline(tracks = listOf(bottom, top))

        val hit = TimelineMath.clipAt(timeline, 500)

        assertEquals(ClipId("t"), hit?.second?.id)
        assertEquals(TrackId("top"), hit?.first?.id)
    }

    @Test
    fun `clipAt returns null in a gap`() {
        val timeline = Timeline(tracks = listOf(Fixtures.videoTrack(clip("a", 0, 0, 1_000))))

        assertNull(TimelineMath.clipAt(timeline, 2_000))
    }

    @Test
    fun `timelineDuration reports the last clip end`() {
        val timeline = Timeline(
            tracks = listOf(
                Fixtures.videoTrack(clip("a", 0, 0, 1_000)),
                Fixtures.audioTrack(clip("c", 4_000, 0, 2_000)),
            ),
        )

        assertEquals(6_000, TimelineMath.timelineDuration(timeline))
    }
}
