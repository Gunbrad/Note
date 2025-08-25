package com.example.note.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * 权限管理工具类
 * 处理Android各版本的运行时权限检查
 */
public class PermissionUtils {
    
    /**
     * 检查通知权限（Android 13+）
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        // Android 13以下默认有通知权限
        return true;
    }
    
    /**
     * 请求通知权限（Android 13+）
     */
    public static void requestNotificationPermission(AppCompatActivity activity, 
            ActivityResultLauncher<String> permissionLauncher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
            !hasNotificationPermission(activity)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
    
    /**
     * 创建通知权限请求启动器
     */
    public static ActivityResultLauncher<String> createNotificationPermissionLauncher(
            AppCompatActivity activity, PermissionCallback callback) {
        return activity.registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (callback != null) {
                    callback.onPermissionResult(isGranted);
                }
            }
        );
    }
    
    /**
     * 检查相机权限
     */
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 权限回调接口
     */
    public interface PermissionCallback {
        void onPermissionResult(boolean granted);
    }
}