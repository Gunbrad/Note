package com.example.note.util;

import android.graphics.Color;

import java.util.Arrays;
import java.util.List;

/**
 * 颜色工具类
 * 提供预定义颜色和颜色处理功能
 */
public class ColorUtils {
    
    // 预定义的笔记卡片颜色
    public static final String[] NOTEBOOK_COLORS = {
        "#FFFFFF", // 白色（默认）
        "#FFE0E0", // 浅红
        "#E0F0FF", // 浅蓝
        "#E0FFE0", // 浅绿
        "#FFFFE0", // 浅黄
        "#F0E0FF", // 浅紫
        "#FFE0F0", // 浅粉
        "#E0FFFF", // 浅青
        "#F0F0E0", // 浅橄榄
        "#FFE0D0"  // 浅橙
    };
    
    // 预定义的单元格背景色
    public static final String[] CELL_BACKGROUND_COLORS = {
        "#FFFFFF", // 白色（默认）
        "#FFCCCC", // 红色
        "#CCDDFF", // 蓝色
        "#CCFFCC", // 绿色
        "#FFFFCC", // 黄色
        "#DDCCFF", // 紫色
        "#FFCCDD", // 粉色
        "#CCFFFF", // 青色
        "#F0F0F0"  // 灰色
    };
    
    // 预定义的文本颜色
    public static final String[] TEXT_COLORS = {
        "#000000", // 黑色（默认）
        "#FF0000", // 红色
        "#0000FF", // 蓝色
        "#008000", // 绿色
        "#FFA500", // 橙色
        "#800080", // 紫色
        "#FF1493", // 深粉
        "#008B8B", // 深青
        "#696969"  // 深灰
    };
    
    /**
     * 将颜色字符串转换为Color整数
     */
    public static int parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return Color.WHITE;
        }
        try {
            return Color.parseColor(colorStr);
        } catch (IllegalArgumentException e) {
            return Color.WHITE;
        }
    }
    
    /**
     * 将Color整数转换为颜色字符串
     */
    public static String colorToString(int color) {
        return String.format("#%08X", color);
    }
    
    /**
     * 获取预设颜色列表
     */
    public static List<String> getPresetColors() {
        return Arrays.asList(NOTEBOOK_COLORS);
    }
    
    /**
     * 获取默认笔记本颜色
     */
    public static String getDefaultNotebookColor() {
        return NOTEBOOK_COLORS[0];
    }
    
    /**
     * 获取默认单元格背景色
     */
    public static String getDefaultCellBackgroundColor() {
        return CELL_BACKGROUND_COLORS[0];
    }
    
    /**
     * 获取默认文本颜色
     */
    public static String getDefaultTextColor() {
        return TEXT_COLORS[0];
    }
    
    /**
     * 获取背景颜色（用于卡片背景）
     */
    public static int getBackgroundColor(String colorStr) {
        return parseColor(colorStr);
    }
    
    /**
     * 判断颜色是否为深色
     */
    public static boolean isDarkColor(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }
    
    /**
     * 判断颜色字符串是否为深色
     */
    public static boolean isDarkColor(String colorStr) {
        return isDarkColor(parseColor(colorStr));
    }
    
    /**
     * 根据背景色获取合适的文本颜色（黑色或白色）
     */
    public static int getContrastTextColor(int backgroundColor) {
        return isDarkColor(backgroundColor) ? Color.WHITE : Color.BLACK;
    }
    
    /**
     * 根据背景色字符串获取合适的文本颜色字符串
     */
    public static String getContrastTextColor(String backgroundColorStr) {
        int backgroundColor = parseColor(backgroundColorStr);
        int textColor = getContrastTextColor(backgroundColor);
        return colorToString(textColor);
    }
    
    /**
     * 为颜色添加透明度
     */
    public static int addAlpha(int color, float alpha) {
        int alphaInt = Math.round(alpha * 255);
        return Color.argb(alphaInt, Color.red(color), Color.green(color), Color.blue(color));
    }
    
    /**
     * 验证颜色字符串格式是否正确
     */
    public static boolean isValidColorString(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return false;
        }
        try {
            Color.parseColor(colorStr);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 获取随机笔记颜色
     */
    public static String getRandomNotebookColor() {
        int index = (int) (Math.random() * NOTEBOOK_COLORS.length);
        return NOTEBOOK_COLORS[index];
    }
}