package com.reelforge.core.common

import java.util.UUID

/**
 * Identifier generation.
 *
 * Ids are opaque strings rather than auto-increment integers because a project
 * is edited offline, duplicated, and (eventually) synced. An integer id only
 * means something relative to one database, which makes merging two edits of
 * the same project ambiguous. A UUID travels with the entity it names.
 *
 * The generator is injectable so tests can produce stable, readable ids instead
 * of asserting against randomness.
 */
fun interface IdGenerator {
    fun newId(): String
}

/** Production generator. */
object UuidGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
