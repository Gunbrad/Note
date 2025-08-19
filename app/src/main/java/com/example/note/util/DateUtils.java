package com.example.note.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期工具类
 * 统一使用 epochMillis (UTC) 作为时间戳格式
 */
public class DateUtils {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    
    /**
     * 获取当前时间戳（毫秒）
     */
    public static long now() {
        return System.currentTimeMillis();
    }
    
    /**
     * 将时间戳转换为日期字符串
     */
    public static String formatDate(long epochMillis) {
        LocalDate date = Instant.ofEpochMilli(epochMillis)
                .atZone(SYSTEM_ZONE)
                .toLocalDate();
        return date.format(DATE_FORMATTER);
    }
    
    /**
     * 将时间戳转换为日期时间字符串
     */
    public static String formatDateTime(long epochMillis) {
        LocalDateTime dateTime = Instant.ofEpochMilli(epochMillis)
                .atZone(SYSTEM_ZONE)
                .toLocalDateTime();
        return dateTime.format(DATETIME_FORMATTER);
    }
    
    /**
     * 将日期字符串解析为时间戳
     */
    public static long parseDate(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
        return date.atStartOfDay(SYSTEM_ZONE).toInstant().toEpochMilli();
    }
    
    /**
     * 获取今日开始时间戳
     */
    public static long getTodayStart() {
        return LocalDate.now().atStartOfDay(SYSTEM_ZONE).toInstant().toEpochMilli();
    }
    
    /**
     * 获取今日结束时间戳
     */
    public static long getTodayEnd() {
        return LocalDate.now().plusDays(1).atStartOfDay(SYSTEM_ZONE).toInstant().toEpochMilli() - 1;
    }
    
    /**
     * 获取本周开始时间戳（周一）
     */
    public static long getWeekStart() {
        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        return monday.atStartOfDay(SYSTEM_ZONE).toInstant().toEpochMilli();
    }
    
    /**
     * 获取本月开始时间戳
     */
    public static long getMonthStart() {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        return firstDay.atStartOfDay(SYSTEM_ZONE).toInstant().toEpochMilli();
    }
    
    /**
     * 判断是否为今天
     */
    public static boolean isToday(long epochMillis) {
        LocalDate date = Instant.ofEpochMilli(epochMillis)
                .atZone(SYSTEM_ZONE)
                .toLocalDate();
        return date.equals(LocalDate.now());
    }
    
    /**
     * 计算两个时间戳之间的天数差
     */
    public static long daysBetween(long startMillis, long endMillis) {
        LocalDate startDate = Instant.ofEpochMilli(startMillis)
                .atZone(SYSTEM_ZONE)
                .toLocalDate();
        LocalDate endDate = Instant.ofEpochMilli(endMillis)
                .atZone(SYSTEM_ZONE)
                .toLocalDate();
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * 获取相对时间描述
     */
    public static String getRelativeTime(long epochMillis) {
        return formatRelativeTime(epochMillis);
    }
    
    /**
     * 格式化相对时间描述
     */
    public static String formatRelativeTime(long epochMillis) {
        long now = System.currentTimeMillis();
        long diff = now - epochMillis;
        
        if (diff < 60 * 1000) {
            return "刚刚";
        } else if (diff < 60 * 60 * 1000) {
            return (diff / (60 * 1000)) + "分钟前";
        } else if (diff < 24 * 60 * 60 * 1000) {
            return (diff / (60 * 60 * 1000)) + "小时前";
        } else if (diff < 7 * 24 * 60 * 60 * 1000) {
            return (diff / (24 * 60 * 60 * 1000)) + "天前";
        } else {
            return formatDate(epochMillis);
        }
    }
}