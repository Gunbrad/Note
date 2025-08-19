package com.example.note.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * 键盘工具类
 * 处理软键盘的显示和隐藏
 */
public class KeyboardUtils {
    
    /**
     * 显示软键盘
     */
    public static void showKeyboard(View view) {
        if (view == null) return;
        
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            view.requestFocus();
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    /**
     * 隐藏软键盘
     */
    public static void hideKeyboard(View view) {
        if (view == null) return;
        
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    
    /**
     * 隐藏软键盘（通过Activity）
     */
    public static void hideKeyboard(Activity activity) {
        if (activity == null) return;
        
        View currentFocus = activity.getCurrentFocus();
        if (currentFocus != null) {
            hideKeyboard(currentFocus);
        }
    }
    
    /**
     * 切换软键盘显示状态
     */
    public static void toggleKeyboard(Context context) {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(0, 0);
        }
    }
    
    /**
     * 判断软键盘是否显示
     * 注意：这个方法可能不够准确，建议配合其他方式使用
     */
    public static boolean isKeyboardVisible(Activity activity) {
        if (activity == null) return false;
        
        View rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return false;
        
        int screenHeight = rootView.getHeight();
        int[] location = new int[2];
        rootView.getLocationOnScreen(location);
        int visibleHeight = screenHeight - location[1];
        
        // 如果可见高度小于屏幕高度的75%，认为键盘显示
        return visibleHeight < screenHeight * 0.75;
    }
}