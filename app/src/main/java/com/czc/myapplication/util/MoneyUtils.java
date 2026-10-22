package com.czc.myapplication.util;

import java.text.NumberFormat;

/** 金额工具：内部以“分”为整数，展示为“元”，带千分位，整数不带小数 */
public class MoneyUtils {

    private MoneyUtils() {}

    /** 分 → 展示字符串。1200 → "12"；123456 → "1,234.56" */
    public static String format(long cents) {
        boolean negative = cents < 0;
        long abs = Math.abs(cents);
        StringBuilder sb = new StringBuilder();
        if (negative) sb.append('-');
        if (abs % 100 == 0) {
            sb.append(group(abs / 100));
        } else {
            sb.append(group(abs / 100)).append('.');
            long dec = abs % 100;
            if (dec < 10) sb.append('0');
            sb.append(dec);
        }
        return sb.toString();
    }

    /** 千分位分组（元整数部分） */
    private static String group(long n) {
        String s = String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            sb.append(s.charAt(i));
            count++;
            if (count % 3 == 0 && i > 0) sb.append(',');
        }
        return sb.reverse().toString();
    }

    /** 带符号显示：+1,234.56 / -1,234.56（对比差值用）。format 本身已处理负号，这里只追加符号前缀 */
    public static String formatSigned(long cents) {
        return (cents > 0 ? "+" : cents < 0 ? "-" : "") + format(Math.abs(cents));
    }

    /** 用户输入的元字符串 → 分；解析失败返回 -1 */
    public static long parseToCents(String yuanText) {
        if (yuanText == null) return -1;
        String s = yuanText.trim().replace(",", "").replace("，", "");
        if (s.isEmpty()) return -1;
        try {
            java.math.BigDecimal bd = new java.math.BigDecimal(s);
            if (bd.signum() < 0) return -1;
            return bd.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            return -1;
        }
    }
}
