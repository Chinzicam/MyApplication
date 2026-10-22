package com.czc.myapplication.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.junit.Test;

public class DateUtilsTest {

    @Test
    public void startOfDay_boundary() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 16, 23, 59, 59);
        long start = DateUtils.startOfDay(c.getTimeInMillis());
        Calendar r = Calendar.getInstance();
        r.setTimeInMillis(start);
        assertEquals(2026, r.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, r.get(Calendar.MONTH));
        assertEquals(16, r.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, r.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, r.get(Calendar.MINUTE));
        assertEquals(0, r.get(Calendar.SECOND));
        assertEquals(0, r.get(Calendar.MILLISECOND));
    }

    @Test
    public void monthRange_isOneMonthWide() {
        long start = DateUtils.monthStartMillis(2026, 10);
        long end = DateUtils.monthEndExclusive(2026, 10);
        assertEquals(31L * 24 * 3600 * 1000, end - start); // 10 月 31 天
        assertEquals(DateUtils.monthStartMillis(2026, 11), end);
    }

    @Test
    public void monthRange_february() {
        long start = DateUtils.monthStartMillis(2024, 2);
        long end = DateUtils.monthEndExclusive(2024, 2);
        assertEquals(29L * 24 * 3600 * 1000, end - start); // 2024 闰年
    }

    @Test
    public void yearRange_isOneYearWide() {
        long start = DateUtils.yearStartMillis(2026);
        long end = DateUtils.yearEndExclusive(2026);
        assertEquals(DateUtils.monthStartMillis(2027, 1), end);
        // 2026 非闰年 = 365 天
        assertEquals(365L * 24 * 3600 * 1000, end - start);
    }

    @Test
    public void formatHelpers() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 16, 12, 30, 0);
        assertTrue(DateUtils.formatDate(c.getTimeInMillis()).startsWith("2026-10-16"));
        assertTrue(DateUtils.formatTime(c.getTimeInMillis()).startsWith("12:30"));
        assertEquals("2026年10月", DateUtils.formatMonth(2026, 10));
        assertEquals("10月", DateUtils.formatMonthShort(10));
        assertEquals("2026年", DateUtils.formatYear(2026));
    }
}
