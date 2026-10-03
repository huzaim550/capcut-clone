package com.reelforge.editor.engine

import com.reelforge.core.common.AppResult
import com.reelforge.core.model.Project

/**
 * Holds the project being edited and the undo/redo history around it.
 *
 * Undo is **snapshot-based**: before a command succeeds, the whole previous
 * [Project] is pushed. This is deliberately unglamorous. The alternative --
 * commands that can invert themselves -- means every new command needs a
 * correct inverse, and a bug in one inverse silently corrupts history in a way
 * that is very hard to reproduce. Snapshots are obviously correct, and the
 * projects involved are small in-memory trees of ids, numbers, and short lists.
 *
 * The trade-off is memory growing with history depth, so the stack is capped at
 * [historyLimit]. Dropping the oldest entries is safe: they only become
 * unreachable, never inconsistent. If projects ever grow large enough for this
 * to matter, command inversion is the optimisation to revisit.
 *
 * The engine is synchronous and single-threaded by design. Timeline maths is
 * cheap; the UI layer decides which thread to call it on, and tests need no
 * dispatcher.
 */
class EditorEngine(
    initial: Project,
    private val historyLimit: Int = DEFAULT_HISTORY_LIMIT,
) {
    var project: Project = initial
        private set

    private val undoStack = ArrayDeque<Project>()
    private val redoStack = ArrayDeque<Project>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /**
     * Applies [command] and records it for undo on success. On failure the
     * project and both stacks are left exactly as they were, so a rejected edit
     * is a true no-op.
     */
    fun dispatch(command: EditCommand): AppResult<Project> {
        val result = command.apply(project)
        if (result is AppResult.Success) {
            undoStack.addLast(project)
            while (undoStack.size > historyLimit) undoStack.removeFirst()
            redoStack.clear()
            project = result.value
        }
        return result
    }

    /** Reverts the last successful command. Returns the new project, or null if there is nothing to undo. */
    fun undo(): Project? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(project)
        project = previous
        return project
    }

    /** Re-applies the last undone command. Returns the new project, or null if there is nothing to redo. */
    fun redo(): Project? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(project)
        project = next
        return project
    }

    companion object {
        const val DEFAULT_HISTORY_LIMIT: Int = 50
    }
}
