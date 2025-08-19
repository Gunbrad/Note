package com.example.note.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * 显示工具类
 * 处理屏幕尺寸、密度转换等显示相关功能
 */
public class DisplayUtils {
    
    /**
     * dp转px
     */
    public static int dp2px(Context context, float dpValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, metrics);
    }
    
    /**
     * px转dp
     */
    public static int px2dp(Context context, float pxValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (pxValue / metrics.density + 0.5f);
    }
    
    /**
     * sp转px
     */
    public static int sp2px(Context context, float spValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, metrics);
    }
    
    /**
     * px转sp
     */
    public static int px2sp(Context context, float pxValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (pxValue / metrics.scaledDensity + 0.5f);
    }
    
    /**
     * 获取屏幕宽度（px）
     */
    public static int getScreenWidth(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.widthPixels;
    }
    
    /**
     * 获取屏幕高度（px）
     */
    public static int getScreenHeight(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.heightPixels;
    }
    
    /**
     * 获取屏幕密度
     */
    public static float getScreenDensity(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.density;
    }
    
    /**
     * 获取屏幕密度DPI
     */
    public static int getScreenDensityDpi(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.densityDpi;
    }
    
    /**
     * 获取状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? resources.getDimensionPixelSize(resourceId) : 0;
    }
    
    /**
     * 获取导航栏高度
     */
    public static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        return resourceId > 0 ? resources.getDimensionPixelSize(resourceId) : 0;
    }
    
    /**
     * 判断是否为平板设备
     */
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK)
                >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
    
    /**
     * 获取瀑布流列数
     * 根据屏幕宽度和设备类型确定合适的列数
     */
    public static int getWaterfallColumns(Context context) {
        int screenWidth = getScreenWidth(context);
        int minColumnWidth = dp2px(context, 160); // 最小列宽160dp
        
        if (isTablet(context)) {
            // 平板设备：3-5列
            int columns = Math.max(3, Math.min(5, screenWidth / minColumnWidth));
            return columns;
        } else {
            // 手机设备：2-3列
            int columns = Math.max(2, Math.min(3, screenWidth / minColumnWidth));
            return columns;
        }
    }
    
    /**
     * 获取瀑布流间距
     */
    public static int getWaterfallSpacing(Context context) {
        return dp2px(context, isTablet(context) ? 12 : 8);
    }
    
    /**
     * 获取卡片圆角半径
     */
    public static int getCardCornerRadius(Context context) {
        return dp2px(context, 8);
    }
    
    /**
     * 获取卡片阴影半径
     */
    public static int getCardElevation(Context context) {
        return dp2px(context, 4);
    }
}