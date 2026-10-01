package com.reelforge.core.common

/**
 * A success-or-[AppError] result.
 *
 * Kotlin's `Result<T>` carries a `Throwable`, which pushes every caller back
 * into exception-shaped thinking. Reelforge models failure as a value
 * ([AppError]) so that "what can go wrong here" is visible in the type, and so
 * a `when` over the failure cases is exhaustive at compile time.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>

    val isSuccess: Boolean get() = this is Success<T>
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.value
    fun errorOrNull(): AppError? = (this as? Failure)?.error
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

inline fun <T, R> AppResult<T>.flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
    is AppResult.Success -> transform(value)
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> = apply {
    if (this is AppResult.Success) action(value)
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> = apply {
    if (this is AppResult.Failure) action(error)
}

/**
 * Runs [block], converting any escaping throwable into an [AppError.Unexpected].
 *
 * Used at the boundary between our code and the platform/media libraries, which
 * throw. Everything deeper than that boundary returns [AppResult].
 */
inline fun <T> runCatchingApp(technicalContext: String, block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
    // Coroutine cancellation is control flow, not failure. Rethrowing is what
    // stops us from swallowing it into a generic error.
    throw cancellation
} catch (t: Throwable) {
    AppResult.Failure(AppError.Unexpected(detail = technicalContext, cause = t))
}
