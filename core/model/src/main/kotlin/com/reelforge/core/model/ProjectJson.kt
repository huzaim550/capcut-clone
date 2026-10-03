package com.reelforge.core.model

import com.reelforge.core.common.AppError
import com.reelforge.core.common.AppResult
import kotlinx.serialization.json.Json

/**
 * The single JSON boundary for projects.
 *
 * Two decisions are load-bearing:
 *
 *  * **`ignoreUnknownKeys = true`** is the forward-compatibility guarantee. A
 *    project written by a newer build may carry fields this build has never
 *    heard of; the old build must still open it rather than refusing the user's
 *    work. Combined with [Project.schemaVersion], that is how updates stay
 *    lossless for the fields they *do* understand.
 *  * **Failures come back as [AppResult], never as a thrown
 *    `SerializationException`.** Decoding happens on data the user cannot
 *    control (a corrupted file, a hand-edited export), and a stack trace is not
 *    a recovery path. `Storage` is the honest user-facing framing: something
 *    went wrong with their saved project.
 */
object ProjectJson {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    /** Serializes [project], mapping any encoder failure to [AppError.Storage]. */
    fun encode(project: Project): AppResult<String> =
        storageResult("ProjectJson.encode") { json.encodeToString(project) }

    /** Parses [text], mapping malformed input to [AppError.Storage]. */
    fun decode(text: String): AppResult<Project> =
        storageResult("ProjectJson.decode") { json.decodeFromString<Project>(text) }

    private inline fun <T> storageResult(context: String, block: () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        // Coroutine cancellation is control flow, not failure. Rethrowing keeps
        // us from turning a cancelled load into a spurious "storage" error.
        throw cancellation
    } catch (t: Throwable) {
        AppResult.Failure(AppError.Storage(detail = context, cause = t))
    }
}
