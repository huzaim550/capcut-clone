package com.reelforge.core.common

/**
 * Every failure that can reach the UI, as data rather than an exception.
 *
 * Two rules drive this type:
 *
 *  1. A user must never see a stack trace or a raw FFmpeg/MediaCodec string.
 *     [userMessage] is the sentence we are willing to show; the technical
 *     detail rides along in [technicalDetail] for logs only.
 *  2. Failures must be recoverable-by-decision. [isRecoverable] tells the UI
 *     whether offering "try again" is honest, or whether the user has to pick
 *     a different file.
 */
sealed class AppError(
    val userMessage: String,
    val technicalDetail: String? = null,
    val cause: Throwable? = null,
) {
    /** The media could not be opened, parsed, or decoded by the platform. */
    class UnsupportedMedia(
        detail: String? = null,
        cause: Throwable? = null,
    ) : AppError(
        userMessage = "This file can't be used. Its format or codec isn't supported.",
        technicalDetail = detail,
        cause = cause,
    )

    /** The file was valid once but is gone or unreadable now (deleted, moved, permission revoked). */
    class MediaUnavailable(
        detail: String? = null,
        cause: Throwable? = null,
    ) : AppError(
        userMessage = "A clip in this project is missing or can't be opened. Re-import it to continue.",
        technicalDetail = detail,
        cause = cause,
    )

    /** Reading or writing our own project data failed. */
    class Storage(
        detail: String? = null,
        cause: Throwable? = null,
    ) : AppError(
        userMessage = "Couldn't save your project. Check that you have free storage space.",
        technicalDetail = detail,
        cause = cause,
    )

    /** The requested edit is not legal against the current timeline. */
    class InvalidEdit(
        val reason: String,
        identity: String? = null,
    ) : AppError(
        userMessage = reason,
        technicalDetail = identity,
    )

    /** Rendering failed. Always recoverable: the user can lower the settings and retry. */
    class ExportFailed(
        detail: String? = null,
        cause: Throwable? = null,
    ) : AppError(
        userMessage = "Export failed. Try again, or choose a lower resolution or frame rate.",
        technicalDetail = detail,
        cause = cause,
    )

    /** The user asked to stop. Not an error the user needs telling about. */
    data object Cancelled : AppError(userMessage = "")

    /** Anything genuinely unexpected. Deliberately vague in the UI. */
    class Unexpected(
        detail: String? = null,
        cause: Throwable? = null,
    ) : AppError(
        userMessage = "Something went wrong. Please try again.",
        technicalDetail = detail,
        cause = cause,
    )

    /**
     * Whether showing a "Retry" affordance is honest. A cancelled export is not
     * retried automatically, and unsupported media needs a different file, not
     * another attempt at the same one.
     */
    val isRecoverable: Boolean
        get() = when (this) {
            is ExportFailed, is Storage, is Unexpected -> true
            is UnsupportedMedia, is MediaUnavailable, is InvalidEdit, Cancelled -> false
        }

    /** Whether this failure is worth surfacing at all. Cancellation is not. */
    val isUserVisible: Boolean get() = this !is Cancelled
}
