package com.reelforge.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun `formatClock renders minutes and zero-padded seconds`() {
        assertEquals("0:00", TimeUtils.formatClock(0))
        assertEquals("0:04", TimeUtils.formatClock(4_000))
        assertEquals("0:17", TimeUtils.formatClock(17_000))
        assertEquals("1:00", TimeUtils.formatClock(60_000))
        assertEquals("2:14", TimeUtils.formatClock(134_000))
    }

    @Test
    fun `formatClock switches to hours past sixty minutes`() {
        assertEquals("1:00:00", TimeUtils.formatClock(3_600_000))
        assertEquals("1:03:05", TimeUtils.formatClock(3_785_000))
        assertEquals("10:00:00", TimeUtils.formatClock(36_000_000))
    }

    @Test
    fun `formatClock clamps negative positions instead of rendering a minus sign`() {
        // A playhead can overshoot past zero for a frame while being dragged.
        // Showing "-0:01" would be a bug the user sees, not a value they need.
        assertEquals("0:00", TimeUtils.formatClock(-1))
        assertEquals("0:00", TimeUtils.formatClock(-5_000))
    }

    @Test
    fun `formatClock truncates sub-second remainders rather than rounding up`() {
        // 4_999 ms is still inside the fourth second; rounding would make the
        // readout disagree with the frame on screen.
        assertEquals("0:04", TimeUtils.formatClock(4_999))
    }

    @Test
    fun `formatClockWithCentis exposes hundredths for frame-accurate trimming`() {
        assertEquals("0:04.50", TimeUtils.formatClockWithCentis(4_500))
        assertEquals("0:00.00", TimeUtils.formatClockWithCentis(0))
        assertEquals("0:17.09", TimeUtils.formatClockWithCentis(17_099))
    }

    @Test
    fun `formatTimecode counts frames within the current second`() {
        assertEquals("0:01:15", TimeUtils.formatTimecode(1_500, 30.0))
        assertEquals("0:00:00", TimeUtils.formatTimecode(0, 30.0))
    }

    @Test
    fun `formatTimecode falls back to a plain clock when the frame rate is unknown`() {
        // Probed media can legitimately report no frame rate; that must not
        // produce a division by zero or a garbage frame number.
        assertEquals("0:03", TimeUtils.formatTimecode(3_000, 0.0))
        assertEquals("0:03", TimeUtils.formatTimecode(3_000, -1.0))
    }

    @Test
    fun `microsecond conversion round-trips at the Media3 boundary`() {
        assertEquals(4_500L, TimeUtils.microsToMillis(4_500_000))
        assertEquals(4_500_000L, TimeUtils.millisToMicros(4_500))
        // Sub-millisecond precision is dropped, not rounded: Media3 positions
        // are interpolated, and rounding them makes seeks land inconsistently.
        assertEquals(4L, TimeUtils.microsToMillis(4_999))
    }

    @Test
    fun `formatDurationFriendly omits components that are zero`() {
        assertEquals("18s", TimeUtils.formatDurationFriendly(18_000))
        assertEquals("2m 14s", TimeUtils.formatDurationFriendly(134_000))
        assertEquals("1h 3m", TimeUtils.formatDurationFriendly(3_780_000))
        assertEquals("2h", TimeUtils.formatDurationFriendly(7_200_000))
    }
}
