package com.czc.myapplication.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MoneyUtilsTest {

    @Test
    public void format_zero() {
        assertEquals("0", MoneyUtils.format(0));
    }

    @Test
    public void format_integerYuan() {
        assertEquals("12", MoneyUtils.format(1200));
        assertEquals("1,000", MoneyUtils.format(100000));
    }

    @Test
    public void format_decimal() {
        assertEquals("1,234.56", MoneyUtils.format(123456));
        assertEquals("0.50", MoneyUtils.format(50));
        assertEquals("0.05", MoneyUtils.format(5));
    }

    @Test
    public void format_negative() {
        assertEquals("-32.50", MoneyUtils.format(-3250));
    }

    @Test
    public void formatSigned() {
        assertEquals("+12", MoneyUtils.formatSigned(1200));
        assertEquals("-12", MoneyUtils.formatSigned(-1200));
        assertEquals("+12.50", MoneyUtils.formatSigned(1250));
        assertEquals("0", MoneyUtils.formatSigned(0));
    }

    @Test
    public void parseToCents_normal() {
        assertEquals(3250L, MoneyUtils.parseToCents("32.50"));
        assertEquals(3200L, MoneyUtils.parseToCents("32"));
        assertEquals(123456L, MoneyUtils.parseToCents("1234.56"));
        assertEquals(123456L, MoneyUtils.parseToCents("1,234.56"));
        assertEquals(123456L, MoneyUtils.parseToCents("1，234.56"));
        assertEquals(5L, MoneyUtils.parseToCents("0.05"));
        assertEquals(1L, MoneyUtils.parseToCents("0.005")); // 四舍五入到分
    }

    @Test
    public void parseToCents_invalid() {
        assertEquals(-1L, MoneyUtils.parseToCents(""));
        assertEquals(-1L, MoneyUtils.parseToCents("abc"));
        assertEquals(-1L, MoneyUtils.parseToCents("-5"));
        assertEquals(-1L, MoneyUtils.parseToCents("12.3.4"));
    }
}
