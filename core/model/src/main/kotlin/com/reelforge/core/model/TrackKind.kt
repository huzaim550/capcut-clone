package com.reelforge.core.model

import kotlinx.serialization.Serializable

/**
 * What a [Track] carries.
 *
 * Kept as an enum rather than free-form strings so the engine and the UI can
 * exhaustively `when` over it. New kinds are added here deliberately; they are
 * not a plugin point.
 */
@Serializable
enum class TrackKind {
    /** The main picture track. */
    VIDEO,

    /** Music, voiceover, and other audio. */
    AUDIO,

    /** Rendered text / captions. */
    TEXT,

    /** Picture and stickers layered over [VIDEO]. */
    OVERLAY,
}
