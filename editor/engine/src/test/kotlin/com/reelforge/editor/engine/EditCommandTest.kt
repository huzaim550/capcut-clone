package com.reelforge.editor.engine

import com.reelforge.core.common.AppError
import com.reelforge.core.common.AppResult
import com.reelforge.core.model.ClipId
import com.reelforge.core.model.TrackId
import com.reelforge.editor.engine.Fixtures.clip
import com.reelforge.editor.engine.Fixtures.videoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditCommandTest {

    private fun <T> AppResult<T>.value(): T {
        assertTrue("expected success but was $this", this is AppResult.Success)
        return (this as AppResult.Success<T>).value
    }

    private fun AppResult<*>.assertInvalidEdit() {
        assertTrue("expected InvalidEdit but was $this", this is AppResult.Failure)
        assertTrue((this as AppResult.Failure).error is AppError.InvalidEdit)
    }

    // --- tracks --------------------------------------------------------------

    @Test
    fun `AddTrack appends a track`() {
        val project = Fixtures.emptyProject()
        val command = AddTrack(videoTrack())

        val result = command.apply(project).value()

        assertEquals(1, result.timeline.tracks.size)
        assertEquals(TrackId("video"), result.timeline.tracks.single().id)
    }

    @Test
    fun `AddTrack rejects a duplicate id`() {
        val project = Fixtures.projectWith(videoTrack())

        AddTrack(videoTrack()).apply(project).assertInvalidEdit()
    }

    @Test
    fun `RemoveTrack drops the track and its clips`() {
        val project = Fixtures.projectWith(videoTrack(clip("a", 0, 0, 1_000)))

        val result = RemoveTrack(Fixtures.VIDEO).apply(project).value()

        assertTrue(result.timeline.tracks.isEmpty())
    }

    @Test
    fun `RemoveTrack rejects an unknown track`() {
        RemoveTrack(TrackId("nope")).apply(Fixtures.projectWith(videoTrack())).assertInvalidEdit()
    }

    // --- adding and placing clips -------------------------------------------

    @Test
    fun `AddClip places a clip and keeps the track sorted by start`() {
        val project = Fixtures.projectWith(videoTrack())
        val late = clip("late", 5_000, 0, 1_000)
        val early = clip("early", 1_000, 0, 1_000)

        val afterLate = AddClip(Fixtures.VIDEO, late).apply(project).value()
        val afterEarly = AddClip(Fixtures.VIDEO, early).apply(afterLate).value()

        assertEquals(listOf(ClipId("early"), ClipId("late")), afterEarly.timeline.track(Fixtures.VIDEO)!!.clips.map { it.id })
    }

    @Test
    fun `AddClip overwrites a fully covered clip`() {
        val project = Fixtures.projectWith(videoTrack(clip("old", 1_000, 0, 2_000)))
        val replacement = clip("new", 0, 0, 3_000)

        val result = AddClip(Fixtures.VIDEO, replacement).apply(project).value()

        assertEquals(listOf(ClipId("new")), result.timeline.track(Fixtures.VIDEO)!!.clips.map { it.id })
    }

    @Test
    fun `AddClip splits a clip it lands inside`() {
        val project = Fixtures.projectWith(videoTrack(clip("long", 0, 0, 4_000)))
        val middle = clip("middle", 1_000, 0, 1_000)

        val result = AddClip(Fixtures.VIDEO, middle).apply(project).value()

        assertEquals(
            listOf(ClipId("long"), ClipId("middle"), ClipId("long~r@2000")),
            result.timeline.track(Fixtures.VIDEO)!!.clips.map { it.id },
        )
    }

    @Test
    fun `AddClip rejects a source that is not in the project`() {
        val project = Fixtures.emptyProject(assets = emptyList())

        AddClip(Fixtures.VIDEO, clip("a", 0, 0, 1_000)).apply(project).assertInvalidEdit()
    }

    @Test
    fun `AddClip rejects a trim past the end of the source`() {
        val project = Fixtures.projectWith(videoTrack())
        // The asset is 10 seconds long.
        val tooLong = clip("a", 0, 0, 20_000)

        AddClip(Fixtures.VIDEO, tooLong).apply(project).assertInvalidEdit()
    }

    @Test
    fun `AddClip rejects an empty or inverted source range`() {
        val project = Fixtures.projectWith(videoTrack())

        AddClip(Fixtures.VIDEO, clip("a", 0, 1_000, 1_000)).apply(project).assertInvalidEdit()
        AddClip(Fixtures.VIDEO, clip("a", 0, 2_000, 1_000)).apply(project).assertInvalidEdit()
    }

    @Test
    fun `AddClip rejects a non-positive speed`() {
        val project = Fixtures.projectWith(videoTrack())

        AddClip(Fixtures.VIDEO, clip("a", 0, 0, 1_000, speed = 0f)).apply(project).assertInvalidEdit()
    }

    @Test
    fun `AddClip rejects a placement that rounds to zero timeline length`() {
        val project = Fixtures.projectWith(videoTrack())

        // 1s of source at 5000x rounds to 0ms of timeline occupancy.
        AddClip(Fixtures.VIDEO, clip("a", 0, 0, 1_000, speed = 5_000f)).apply(project).assertInvalidEdit()
    }

    @Test
    fun `AddClip rejects a duplicate clip id`() {
        val project = Fixtures.projectWith(videoTrack(clip("a", 0, 0, 1_000)))

        AddClip(Fixtures.VIDEO, clip("a", 5_000, 0, 1_000)).apply(project).assertInvalidEdit()
    }

    // --- moving, trimming, retiming -----------------------------------------

    @Test
    fun `MoveClip slides a clip and overwrites its destination`() {
        val project = Fixtures.projectWith(
            videoTrack(clip("a", 0, 0, 1_000), clip("b", 2_000, 0, 1_000)),
        )

        val result = MoveClip(ClipId("a"), 2_000).apply(project).value()

        val ids = result.timeline.track(Fixtures.VIDEO)!!.clips.map { it.id }
        assertEquals(listOf(ClipId("a")), ids)
        assertEquals(2_000, result.timeline.clip(ClipId("a"))!!.timelineStartMs)
    }

    @Test
    fun `MoveClip rejects a negative start`() {
        val project = Fixtures.projectWith(videoTrack(clip("a", 0, 0, 1_000)))

        MoveClip(ClipId("a"), -1).apply(project).assertInvalidEdit()
    }

    @Test
    fun `TrimClip shortens a clip without moving it`() {
        val project = Fixtures.projectWith(videoTrack(clip("a", 1_000, 0, 4_000)))

        val result = TrimClip(ClipId("a"), newSourceInMs = 1_000, newSourceOutMs = 3_000).apply(project).value()

        val trimmed = result.timeline.clip(ClipId("a"))!!
        assertEquals(1_000, trimmed.timelineStartMs)
        assertEquals(2_000, trimmed.timelineDurationMs)
    }

    @Test
    fun `TrimClip growing a clip overwrites its neighbour`() {
        val project = Fixtures.projectWith(
            videoTrack(clip("a", 0, 0, 1_000), clip("b", 1_000, 0, 1_000)),
        )

        // Extend a to two seconds, which reaches over all of b.
        val result = TrimClip(ClipId("a"), 0, 2_000).apply(project).value()

        assertEquals(listOf(ClipId("a")), result.timeline.track(Fixtures.VIDEO)!!.clips.map { it.id })
    }

    @Test
    fun `SetClipSpeed changes occupancy and trims what it reaches`() {
        val project = Fixtures.projectWith(
            videoTrack(clip("a", 0, 0, 4_000), clip("b", 4_000, 0, 1_000)),
        )

        // Half speed doubles a's occupancy to 8s, reaching over all of b.
        val result = SetClipSpeed(ClipId("a"), 0.5f).apply(project).value()

        assertEquals(8_000, result.timeline.clip(ClipId("a"))!!.timelineDurationMs)
        assertEquals(listOf(ClipId("a")), result.timeline.track(Fixtures.VIDEO)!!.clips.map { it.id })
    }

    @Test
    fun `SetClipSpeed rejects zero or negative speed`() {
        val project = Fixtures.projectWith(videoTrack(clip("a", 0, 0, 1_000)))

        SetClipSpeed(ClipId("a"), 0f).apply(project).assertInvalidEdit()
        SetClipSpeed(ClipId("a"), -1f).apply(project).assertInvalidEdit()
    }

    // --- properties ----------------------------------------------------------

    @Test
    fun `SetClipVolume updates gain and rejects out-of-range values`() {
        val project = Fixtures.projectWith(videoTrack(clip("a", 0, 0, 1_000)))

        val result = SetClipVolume(ClipId("a"), 0.25f).apply(project).value()
        assertEquals(0.25f, result.timeline.clip(ClipId("a"))!!.volume, 0.0001f)

        SetClipVolume(ClipId("a"), 1.5f).apply(project).assertInvalidEdit()
        SetClipVolume(ClipId("a"), -0.1f).apply(project).assertInvalidEdit()
    }

    @Test
    fun `SetTrackMuted toggles the flag`() {
        val project = Fixtures.projectWith(videoTrack(clip("a", 0, 0, 1_000)))

        val result = SetTrackMuted(Fixtures.VIDEO, true).apply(project).value()

        assertEquals(true, result.timeline.track(Fixtures.VIDEO)!!.isMuted)
    }
}
