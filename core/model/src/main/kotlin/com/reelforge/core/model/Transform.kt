package com.reelforge.core.model

import kotlinx.serialization.Serializable

/**
 * The visual state of a clip: how it is scaled, positioned, rotated, and faded.
 *
 * Every field defaults to the identity value, so a clip that has never been
 * touched renders exactly as its source. That is deliberate: the preview and
 * the export can always apply a [Transform] without first asking "was this set?",
 * and the no-op case costs nothing.
 */
@Serializable
data class Transform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDegrees: Float = 0f,
    val opacity: Float = 1f,
) {
    /** True when applying this transform is a no-op. */
    val isIdentity: Boolean
        get() = scale == 1f &&
            offsetX == 0f &&
            offsetY == 0f &&
            rotationDegrees == 0f &&
            opacity == 1f
}
