package com.reelforge.core.model

import kotlinx.serialization.Serializable

/**
 * Opaque identifier for a [Project].
 *
 * These are value classes rather than bare `String`s so that the compiler
 * refuses to let a track id be passed where a clip id is expected. The usual
 * id-mixup bug is a whole class of runtime failure that simply cannot happen
 * once the types differ.
 *
 * They are not auto-increment integers because a project is edited offline,
 * duplicated, and (eventually) synced: an integer id only means something
 * relative to one database, which makes merging two edits of the same project
 * ambiguous. A UUID travels with the entity it names.
 */
@Serializable
@JvmInline
value class ProjectId(val value: String)

/** Opaque identifier for a [Track]. See [ProjectId]. */
@Serializable
@JvmInline
value class TrackId(val value: String)

/** Opaque identifier for a [Clip]. See [ProjectId]. */
@Serializable
@JvmInline
value class ClipId(val value: String)

/** Opaque identifier for a [MediaAsset]. See [ProjectId]. */
@Serializable
@JvmInline
value class AssetId(val value: String)
