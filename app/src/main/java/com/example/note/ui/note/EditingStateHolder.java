package com.example.note.ui.note;

import java.lang.ref.WeakReference;

/**
 * 编辑状态管理器
 * 用于控制ZoomableRecyclerHost是否拦截触摸事件
 * 当单元格处于编辑模式时，不拦截触摸事件，让EditText正常工作
 * 同时确保同一时刻只有一个单元格处于编辑状态
 */
public class EditingStateHolder {
    
    private static boolean isEditing = false;
    private static OnEditingStateChangeListener listener;
    private static WeakReference<EditingCell> currentEditingCell;
    
    public interface OnEditingStateChangeListener {
        void onEditingStateChanged(boolean isEditing);
    }
    
    public interface EditingCell {
        void forceFinishEditing();
        String getCellIdentifier();
    }
    
    /**
     * 开始编辑（新API）
     * @param editingCell 要开始编辑的单元格
     */
    public static void beginEditing(EditingCell editingCell) {
        if (editingCell == null) {
            return;
        }
        
        // 如果有其他单元格正在编辑，先强制结束它的编辑状态（内部结束，避免重入竞态）
        if (currentEditingCell != null) {
            EditingCell previousCell = currentEditingCell.get();
            if (previousCell != null && previousCell != editingCell) {
                // 内部强制结束，不通过endEditing避免竞态
                previousCell.forceFinishEditing();
            }
        }
        
        // 设置新的编辑单元格
        currentEditingCell = new WeakReference<>(editingCell);
        
        if (!isEditing) {
            isEditing = true;
            if (listener != null) {
                listener.onEditingStateChanged(true);
            }
        }
    }
    
    /**
     * 结束编辑（新API）
     * @param editingCell 要结束编辑的单元格
     */
    public static void endEditing(EditingCell editingCell) {
        if (editingCell == null) {
            return;
        }
        
        // 只有当前正在编辑的单元格才能结束编辑（身份校验，防止竞态）
        if (currentEditingCell != null) {
            EditingCell currentCell = currentEditingCell.get();
            if (currentCell == editingCell) {
                // 只有当前编辑单元格才能清空状态
                currentEditingCell = null;
                if (isEditing) {
                    isEditing = false;
                    if (listener != null) {
                        listener.onEditingStateChanged(false);
                    }
                }
            }
            // 如果不是当前编辑单元格，忽略此次结束调用（防止旧单元格的晚到调用）
        }
    }
    
    /**
     * 设置编辑状态（兼容旧接口）
     * @param editing 是否正在编辑
     * @param editingCell 正在编辑的单元格（如果editing为true）
     */
    public static void setEditing(boolean editing, EditingCell editingCell) {
        if (editing) {
            beginEditing(editingCell);
        } else {
            endEditing(editingCell);
        }
    }
    
    /**
     * 设置编辑状态（兼容旧接口）
     * @param editing 是否正在编辑
     */
    public static void setEditing(boolean editing) {
        setEditing(editing, null);
    }
    
    /**
     * 获取当前编辑状态
     * @return 是否正在编辑
     */
    public static boolean isEditing() {
        return isEditing;
    }
    
    /**
     * 检查指定单元格是否为当前编辑单元格
     * @param editingCell 要检查的单元格
     * @return 是否为当前编辑单元格
     */
    public static boolean isCurrentEditingCell(EditingCell editingCell) {
        if (currentEditingCell == null || editingCell == null) {
            return false;
        }
        EditingCell currentCell = currentEditingCell.get();
        return currentCell == editingCell;
    }
    
    /**
     * 获取当前编辑单元格的标识符
     * @return 当前编辑单元格的标识符，如果没有则返回null
     */
    public static String getCurrentEditingCellIdentifier() {
        if (currentEditingCell == null) {
            return null;
        }
        EditingCell currentCell = currentEditingCell.get();
        return currentCell != null ? currentCell.getCellIdentifier() : null;
    }
    
    /**
     * 设置编辑状态变化监听器
     * @param listener 监听器
     */
    public static void setOnEditingStateChangeListener(OnEditingStateChangeListener listener) {
        EditingStateHolder.listener = listener;
    }
    
    /**
     * 移除监听器
     */
    public static void removeListener() {
        EditingStateHolder.listener = null;
    }
}