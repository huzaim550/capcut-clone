package com.reelforge.core.model

import com.reelforge.core.common.AppError
import com.reelforge.core.common.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectJsonTest {

    private val richProject = Project(
        id = ProjectId("project-1"),
        name = "Holiday cut",
        createdAtMs = 1_700_000_000_000,
        updatedAtMs = 1_700_000_100_000,
        assets = listOf(
            MediaAsset(
                id = AssetId("asset-1"),
                uri = "content://media/external/video/1",
                displayName = "beach.mp4",
                durationMs = 12_000,
                width = 1920,
                height = 1080,
                frameRate = 30.0,
            ),
        ),
        timeline = Timeline(
            tracks = listOf(
                Track(
                    id = TrackId("video-1"),
                    kind = TrackKind.VIDEO,
                    clips = listOf(
                        Clip(
                            id = ClipId("clip-1"),
                            assetId = AssetId("asset-1"),
                            timelineStartMs = 0,
                            sourceInMs = 1_000,
                            sourceOutMs = 5_000,
                            speed = 2f,
                            volume = 0.8f,
                            transform = Transform(scale = 1.5f, offsetX = 10f, opacity = 0.9f),
                        ),
                    ),
                ),
                Track(
                    id = TrackId("audio-1"),
                    kind = TrackKind.AUDIO,
                    isMuted = true,
                ),
            ),
        ),
    )

    @Test
    fun `a fully populated project survives an encode-decode round trip`() {
        val encoded = ProjectJson.encode(richProject)
        assertTrue(encoded is AppResult.Success)

        val decoded = ProjectJson.decode((encoded as AppResult.Success).value)
        assertEquals(richProject, decoded.getOrNull())
    }

    @Test
    fun `unknown fields written by a newer build are ignored, not rejected`() {
        // Forward compatibility: an old build must still open a project saved by
        // a newer one instead of refusing the user's work outright.
        val fromTheFuture = """
            {
              "id": "p1",
              "name": "From the future",
              "createdAtMs": 5,
              "updatedAtMs": 6,
              "schemaVersion": 1,
              "holographicTimeline": { "tracks": [] }
            }
        """.trimIndent()

        val decoded = ProjectJson.decode(fromTheFuture)

        assertEquals("From the future", decoded.getOrNull()?.name)
    }

    @Test
    fun `malformed json fails as a storage error rather than throwing`() {
        val decoded = ProjectJson.decode("{ this is not json")

        assertTrue(decoded is AppResult.Failure)
        assertTrue((decoded as AppResult.Failure).error is AppError.Storage)
    }

    @Test
    fun `missing required fields fail as a storage error`() {
        // A project with no name is not a usable project; the failure must be
        // data the caller can branch on, not an exception escaping the boundary.
        val decoded = ProjectJson.decode("""{ "id": "p1" }""")

        assertTrue(decoded is AppResult.Failure)
    }
}
