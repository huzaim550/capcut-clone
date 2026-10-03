package com.reelforge.core.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.reelforge.core.common.AppError
import com.reelforge.core.common.AppResult
import com.reelforge.core.common.DispatcherProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Room is exercised on the JVM through Robolectric, so these tests need no
 * emulator. They cover the store's contract -- round-tripping, absence, corruption,
 * upsert semantics, ordering, deletion -- rather than SQL details.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProjectStoreTest {

    private lateinit var database: ProjectDatabase
    private lateinit var store: ProjectStore

    // The real dispatchers would work, but running the store's work inline keeps
    // the tests deterministic and free of thread timing.
    private val directDispatchers = object : DispatcherProvider {
        override val main = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = ProjectDatabase.inMemory(context)
        store = ProjectStore(database.projectDao(), directDispatchers)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun project(
        id: String,
        name: String = "Project $id",
        updatedAtMs: Long = 0,
        withClip: Boolean = false,
    ): Project {
        val asset = MediaAsset(
            id = AssetId("asset-$id"),
            uri = "content://media/$id",
            displayName = "clip.mp4",
            durationMs = 10_000,
        )
        val tracks = if (!withClip) {
            emptyList()
        } else {
            listOf(
                Track(
                    id = TrackId("video-$id"),
                    kind = TrackKind.VIDEO,
                    clips = listOf(Clip(ClipId("clip-$id"), asset.id, 0, 0, 2_000)),
                ),
            )
        }
        return Project(
            id = ProjectId(id),
            name = name,
            createdAtMs = 0,
            updatedAtMs = updatedAtMs,
            assets = listOf(asset),
            timeline = Timeline(tracks = tracks),
        )
    }

    @Test
    fun `save then load round-trips a full project`() = runTest {
        val original = project("a", withClip = true)

        assertTrue(store.save(original) is AppResult.Success)

        val loaded = store.load(original.id)
        assertTrue(loaded.isSuccess)
        assertEquals(original, (loaded as AppResult.Success<Project?>).value)
    }

    @Test
    fun `load returns null for an unknown project rather than failing`() = runTest {
        val loaded = store.load(ProjectId("missing"))

        assertTrue(loaded.isSuccess)
        assertNull((loaded as AppResult.Success<Project?>).value)
    }

    @Test
    fun `load reports a corrupt document as a storage failure`() = runTest {
        database.projectDao().upsert(ProjectEntity("bad", "bad", 0, 0, 1, "{ not valid json"))

        val loaded = store.load(ProjectId("bad"))

        assertTrue(loaded is AppResult.Failure)
        assertTrue((loaded as AppResult.Failure).error is AppError.Storage)
    }

    @Test
    fun `save is an upsert that replaces the previous version`() = runTest {
        store.save(project("a", name = "First", updatedAtMs = 1))
        store.save(project("a", name = "Second", updatedAtMs = 2))

        val loaded = store.load(ProjectId("a"))

        assertEquals("Second", (loaded as AppResult.Success<Project?>).value?.name)
    }

    @Test
    fun `observeSummaries lists the most recently updated first`() = runTest {
        store.save(project("old", updatedAtMs = 100))
        store.save(project("new", updatedAtMs = 200))

        val summaries = store.observeSummaries().first()

        assertEquals(listOf(ProjectId("new"), ProjectId("old")), summaries.map { it.id })
        assertEquals("Project new", summaries.first().name)
    }

    @Test
    fun `delete removes a project`() = runTest {
        store.save(project("a"))

        assertTrue(store.delete(ProjectId("a")) is AppResult.Success)

        val loaded = store.load(ProjectId("a"))
        assertNull((loaded as AppResult.Success<Project?>).value)
    }

    @Test
    fun `deleting a missing project is not an error`() = runTest {
        assertTrue(store.delete(ProjectId("never-existed")) is AppResult.Success)
    }
}
