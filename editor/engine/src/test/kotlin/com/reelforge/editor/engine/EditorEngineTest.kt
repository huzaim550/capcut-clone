package com.reelforge.editor.engine

import com.reelforge.core.common.AppResult
import com.reelforge.core.model.ClipId
import com.reelforge.core.model.TrackId
import com.reelforge.editor.engine.Fixtures.clip
import com.reelforge.editor.engine.Fixtures.videoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorEngineTest {

    private fun engine() = EditorEngine(Fixtures.projectWith(videoTrack()))

    private fun add(engine: EditorEngine, id: String, startMs: Long) =
        engine.dispatch(AddClip(Fixtures.VIDEO, clip(id, startMs, 0, 1_000)))

    @Test
    fun `dispatch applies a command and records it for undo`() {
        val engine = engine()

        val result = add(engine, "a", 0)

        assertTrue(result is AppResult.Success)
        assertEquals(ClipId("a"), engine.project.timeline.clip(ClipId("a"))?.id)
        assertTrue(engine.canUndo)
        assertFalse(engine.canRedo)
    }

    @Test
    fun `a rejected command leaves the project and history untouched`() {
        val engine = engine()
        val before = engine.project

        // Not a real track.
        val result = engine.dispatch(AddClip(TrackId("nope"), clip("a", 0, 0, 1_000)))

        assertTrue(result is AppResult.Failure)
        assertEquals(before, engine.project)
        assertFalse(engine.canUndo)
    }

    @Test
    fun `undo restores the previous project`() {
        val engine = engine()
        add(engine, "a", 0)

        val restored = engine.undo()

        assertNull(engine.project.timeline.clip(ClipId("a")))
        assertEquals(engine.project, restored)
        assertFalse(engine.canUndo)
        assertTrue(engine.canRedo)
    }

    @Test
    fun `redo re-applies what undo reverted`() {
        val engine = engine()
        add(engine, "a", 0)
        engine.undo()

        engine.redo()

        assertEquals(ClipId("a"), engine.project.timeline.clip(ClipId("a"))?.id)
        assertTrue(engine.canUndo)
        assertFalse(engine.canRedo)
    }

    @Test
    fun `a new command after undo clears the redo stack`() {
        val engine = engine()
        add(engine, "a", 0)
        engine.undo()

        add(engine, "b", 0)

        assertFalse(engine.canRedo)
        assertEquals(ClipId("b"), engine.project.timeline.clip(ClipId("b"))?.id)
    }

    @Test
    fun `undo and redo return null when there is nothing to do`() {
        val engine = engine()

        assertNull(engine.undo())
        assertNull(engine.redo())
    }

    @Test
    fun `history is capped so memory does not grow without bound`() {
        val engine = EditorEngine(Fixtures.projectWith(videoTrack()), historyLimit = 3)
        repeat(5) { index -> add(engine, "clip-$index", index * 2_000L) }

        // Only three undos are possible; the oldest two snapshots were dropped.
        var undos = 0
        while (engine.undo() != null) undos++

        assertEquals(3, undos)
    }
}
