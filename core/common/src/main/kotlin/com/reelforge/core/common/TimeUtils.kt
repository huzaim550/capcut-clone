package com.reelforge.core.common

/**
 * Time formatting and unit conversion.
 *
 * Reelforge keeps exactly one unit for timeline positions: **milliseconds, as a
 * Long**. Not seconds-as-Double, not microseconds. The reasons:
 *
 *  * Long milliseconds are exact. A timeline is built by repeatedly adding and
 *    subtracting clip durations; with floating point those additions drift, and
 *    a clip that should end exactly where the next begins ends up one
 *    microsecond short. That visible artefact is a one-frame gap at every cut.
 *  * Media3 speaks microseconds, the platform MediaMetadataRetriever speaks
 *    milliseconds, and ffprobe-style tools speak seconds. Converting at the
 *    boundary (here) beats letting three units leak into the timeline model.
 *
 * Microsecond conversion is deliberately *only* at the Media3 boundary and is
 * documented as such, so nobody has to guess which unit a field is in.
 */
object TimeUtils {

    const val MILLIS_PER_SECOND = 1_000L
    const val MICROS_PER_MILLI = 1_000L
    const val MICROS_PER_SECOND = 1_000_000L

    /** Media3 boundary: microseconds -> our canonical milliseconds. */
    fun microsToMillis(micros: Long): Long = micros / MICROS_PER_MILLI

    /** Media3 boundary: our canonical milliseconds -> microseconds. */
    fun millisToMicros(millis: Long): Long = millis * MICROS_PER_MILLI

    fun secondsToMillis(seconds: Double): Long = Math.round(seconds * MILLIS_PER_SECOND)

    /**
     * Formats a position as `M:SS`, or `H:MM:SS` once it passes an hour.
     *
     * Negative input is clamped to zero: a playhead should never read `-0:01`
     * during a transient overshoot while dragging.
     */
    fun formatClock(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0L)) / MILLIS_PER_SECOND
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /**
     * Formats a position as `M:SS.cc`. Used on the editing timeline, where the
     * second decimal is the difference between an accurate trim and a
     * guess -- a coarse `M:SS` readout makes frame-accurate trimming impossible.
     */
    fun formatClockWithCentis(millis: Long): String {
        val clamped = millis.coerceAtLeast(0L)
        val centis = (clamped % MILLIS_PER_SECOND) / 10
        return "%s.%02d".format(formatClock(clamped), centis)
    }

    /** Formats a position as `M:SS:FF` against a frame rate. */
    fun formatTimecode(millis: Long, framesPerSecond: Double): String {
        if (framesPerSecond <= 0.0) return formatClock(millis)
        val clamped = millis.coerceAtLeast(0L)
        val frame = ((clamped % MILLIS_PER_SECOND) / MILLIS_PER_SECOND * framesPerSecond).toInt()
        val maxFrame = Math.ceil(framesPerSecond).toInt().coerceAtLeast(1) - 1
        return "%s:%02d".format(formatClock(clamped), frame.coerceIn(0, maxFrame))
    }

    /**
     * Formats a duration for human reading, e.g. `2m 14s`, `1h 3m`, `18s`.
     * Used in the project browser, where a precise clock is noise.
     */
    fun formatDurationFriendly(millis: Long): String {
        val totalSeconds = millis.coerceAtLeast(0L) / MILLIS_PER_SECOND
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
