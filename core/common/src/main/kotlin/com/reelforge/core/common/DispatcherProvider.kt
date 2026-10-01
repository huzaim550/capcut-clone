package com.reelforge.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Indirection over [Dispatchers] so that anything doing real work can be driven
 * by a test dispatcher.
 *
 * The editor engine never touches [Dispatchers] directly. Timeline maths is
 * pure and synchronous, but the repositories and the export pipeline are not,
 * and those are exactly the pieces that need deterministic tests.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
}

/** The production provider. */
object DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val io: CoroutineDispatcher = Dispatchers.IO
}
