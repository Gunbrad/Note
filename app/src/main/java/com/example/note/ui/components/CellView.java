package com.example.note.ui.components;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.note.data.entity.Cell;

/**
 * 简化的单元格视图组件
 * 只支持文本编辑
 */
public class CellView extends FrameLayout {
    
    private Cell cell;
    private boolean isEditing = false;
    private View currentView;
    private OnCellValueChangedListener valueChangedListener;
    private OnCellClickListener clickListener;
    
    public interface OnCellValueChangedListener {
        void onValueChanged(Cell cell, String newValue);
    }
    
    public interface OnCellClickListener {
        void onCellClick(Cell cell);
        void onCellLongClick(Cell cell);
    }
    
    public CellView(Context context) {
        super(context);
        init();
    }
    
    public CellView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public CellView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        setPadding(8, 8, 8, 8);
        setMinimumHeight(48);
        setBackgroundColor(Color.WHITE);
    }
    
    /**
     * 设置单元格数据
     */
    public void setCell(Cell cell) {
        this.cell = cell;
        updateView();
    }
    
    /**
     * 获取当前单元格
     */
    public Cell getCell() {
        return cell;
    }
    
    /**
     * 设置编辑模式
     */
    public void setEditing(boolean editing) {
        if (this.isEditing != editing) {
            this.isEditing = editing;
            updateView();
        }
    }
    
    /**
     * 是否处于编辑模式
     */
    public boolean isEditing() {
        return isEditing;
    }
    
    /**
     * 设置值变化监听器
     */
    public void setOnCellValueChangedListener(OnCellValueChangedListener listener) {
        this.valueChangedListener = listener;
    }
    
    /**
     * 设置点击监听器
     */
    public void setOnCellClickListener(OnCellClickListener listener) {
        this.clickListener = listener;
    }
    
    /**
     * 更新视图
     */
    private void updateView() {
        removeAllViews();
        
        if (cell == null) {
            return;
        }
        
        if (isEditing) {
            currentView = createEditView();
        } else {
            currentView = createDisplayView();
        }
        
        if (currentView != null) {
            addView(currentView);
        }
    }
    
    /**
     * 创建显示视图
     */
    private View createDisplayView() {
        TextView textView = new TextView(getContext());
        textView.setText(cell.getContent() != null ? cell.getContent() : "");
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setSingleLine(false);
        textView.setMaxLines(3);
        textView.setTextColor(Color.BLACK);
        textView.setTextSize(14);
        
        setupClickListeners(textView);
        return textView;
    }
    
    /**
     * 创建编辑视图
     */
    private View createEditView() {
        EditText editText = new EditText(getContext());
        editText.setText(cell.getContent() != null ? cell.getContent() : "");
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.setPadding(0, 0, 0, 0);
        editText.setGravity(Gravity.CENTER_VERTICAL);
        editText.setTextColor(Color.BLACK);
        editText.setTextSize(14);
        
        // 添加文本变化监听器
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (valueChangedListener != null && cell != null) {
                    valueChangedListener.onValueChanged(cell, s.toString());
                }
            }
        });
        
        // 请求焦点
        editText.requestFocus();
        
        return editText;
    }
    
    /**
     * 设置点击监听器
     */
    private void setupClickListeners(View view) {
        view.setOnClickListener(v -> {
            if (clickListener != null && cell != null) {
                clickListener.onCellClick(cell);
            }
        });
        
        view.setOnLongClickListener(v -> {
            if (clickListener != null && cell != null) {
                clickListener.onCellLongClick(cell);
            }
            return true;
        });
    }
    
    /**
     * 完成编辑
     */
    public void finishEditing() {
        if (isEditing) {
            setEditing(false);
        }
    }
    
    /**
     * 开始编辑
     */
    public void startEditing() {
        if (!isEditing) {
            setEditing(true);
        }
    }
}