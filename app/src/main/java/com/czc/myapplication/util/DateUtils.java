package com.czc.myapplication.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** 日期工具：全部基于本地时区（与 SQLite 'localtime' 修饰符一致） */
public class DateUtils {

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat MONTH_FMT = new SimpleDateFormat("yyyy年M月", Locale.getDefault());
    private static final SimpleDateFormat YEAR_FMT = new SimpleDateFormat("yyyy年", Locale.getDefault());

    private DateUtils() {}

    public static long todayStartMillis() {
        return startOfDay(System.currentTimeMillis());
    }

    /** 某时刻所在日的 00:00:00.000 */
    public static long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** 某时刻所在日的下一天 00:00:00.000（范围查询的右开区间端点） */
    public static long endOfDayExclusive(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfDay(millis));
        c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    /** 某年某月（1-12）的 1 日 00:00 */
    public static long monthStartMillis(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, 1, 0, 0, 0);
        return c.getTimeInMillis();
    }

    /** 某年某月的下一个月 1 日 00:00（右开区间端点） */
    public static long monthEndExclusive(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, 1, 0, 0, 0);
        c.add(Calendar.MONTH, 1);
        return c.getTimeInMillis();
    }

    /** 某年 1 月 1 日 00:00 */
    public static long yearStartMillis(int year) {
        return monthStartMillis(year, 1);
    }

    /** 某年下一年 1 月 1 日 00:00（右开区间端点） */
    public static long yearEndExclusive(int year) {
        return monthStartMillis(year + 1, 1);
    }

    public static int currentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    public static int currentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    /** 所在月的年份（用于月份选择器） */
    public static int yearOf(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c.get(Calendar.YEAR);
    }

    public static int monthOf(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c.get(Calendar.MONTH) + 1;
    }

    public static String formatTime(long millis) {
        return TIME_FMT.format(new Date(millis));
    }

    public static String formatDate(long millis) {
        return DATE_FMT.format(new Date(millis));
    }

    /** 完整日期时间：yyyy-MM-dd HH:mm */
    public static String formatDateTime(long millis) {
        return DATE_FMT.format(new Date(millis)) + " " + TIME_FMT.format(new Date(millis));
    }

    /** 月份标签：2026年10月 */
    public static String formatMonth(int year, int month) {
        return year + "年" + month + "月";
    }

    /** 月份短标签（柱状图 x 轴）：10月 */
    public static String formatMonthShort(int month) {
        return month + "月";
    }

    public static String formatYear(int year) {
        return year + "年";
    }

    /** 星期中文 */
    public static String formatWeekday(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        String[] weeks = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return weeks[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static String todayLabel() {
        return formatDate(System.currentTimeMillis()) + " " + formatWeekday(System.currentTimeMillis());
    }
}
