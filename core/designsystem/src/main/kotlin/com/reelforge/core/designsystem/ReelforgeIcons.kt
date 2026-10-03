package com.reelforge.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Reelforge's own icon set.
 *
 * We ship vectors rather than depending on `material-icons-extended`: that
 * artifact is no longer published for the Compose version this project uses, and
 * pulling a whole icon font in for five glyphs would be wasteful anyway. Each
 * icon is a 24dp outline that `Icon` tints with the current content colour, so
 * one definition works in every theme.
 */
object ReelforgeIcons {

    val Add: ImageVector by lazy {
        icon("Add", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")
    }

    val Delete: ImageVector by lazy {
        icon(
            "Delete",
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12z" +
                "M19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
        )
    }

    val ArrowBack: ImageVector by lazy {
        icon("ArrowBack", "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z")
    }

    val Movie: ImageVector by lazy {
        icon(
            "Movie",
            "M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18" +
                "c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z",
        )
    }

    val Play: ImageVector by lazy {
        icon("Play", "M8 5v14l11-7z")
    }
}

private fun icon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black),
    ).build()
