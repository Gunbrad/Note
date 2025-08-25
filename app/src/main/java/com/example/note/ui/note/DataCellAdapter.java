package com.example.note.ui.note;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;
import com.example.note.data.entity.Cell;
import com.example.note.data.entity.Column;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据单元格适配器 - 支持内联编辑
 * 使用ColumnWidthProvider提供基于缩放的动态尺寸
 */
public class DataCellAdapter extends RecyclerView.Adapter<DataCellAdapter.DataCellViewHolder> {
    
    private List<Cell> cells = new ArrayList<>();
    private List<Column> columns = new ArrayList<>();
    private int rowIndex;
    private int rowHeightDp = 44;
    private OnCellChangeListener listener;
    private ColumnWidthProvider widthProvider;
    
    public interface OnCellChangeListener {
        void onCellClick(int rowIndex, int columnIndex);
        void onCellLongClick(int rowIndex, int columnIndex);
        void onCellContentChanged(int rowIndex, int columnIndex, String newContent);
    }
    
    public void setOnCellChangeListener(OnCellChangeListener listener) {
        this.listener = listener;
    }
    
    public void setColumnWidthProvider(ColumnWidthProvider widthProvider) {
        this.widthProvider = widthProvider;
    }
    
    public void setRowData(int rowIndex, List<Cell> cells, List<Column> columns) {
        this.rowIndex = rowIndex;
        this.cells.clear();
        this.columns.clear();
        
        if (cells != null) {
            this.cells.addAll(cells);
        }
        if (columns != null) {
            this.columns.addAll(columns);
        }
        
        // 确保单元格数量与列数量匹配
        while (this.cells.size() < this.columns.size()) {
            Cell emptyCell = new Cell();
            emptyCell.setRowIndex(rowIndex);
            emptyCell.setColIndex(this.cells.size());
            emptyCell.setContent("");
            this.cells.add(emptyCell);
        }
        
        notifyDataSetChanged();
    }
    
    public void setRowHeight(int heightDp) {
        // 不再设置全局rowHeightDp，行高现在由ColumnWidthProvider管理
        // 只需要通知数据变化以刷新UI
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public DataCellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_data_cell, parent, false);
        return new DataCellViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DataCellViewHolder holder, int position) {
        if (position < cells.size() && position < columns.size()) {
            Cell cell = cells.get(position);
            Column column = columns.get(position);
            holder.bind(cell, column, position);
        }
    }
    
    @Override
    public int getItemCount() {
        return Math.min(cells.size(), columns.size());
    }
    
    @Override
    public void onViewRecycled(@NonNull DataCellViewHolder holder) {
        super.onViewRecycled(holder);
        // 强制结束编辑状态，防止回收时状态污染
        holder.forceFinishEditing();
    }
    
    class DataCellViewHolder extends RecyclerView.ViewHolder implements EditingStateHolder.EditingCell {
        private EditText editText;
        private int currentRowIndex;
        private int currentColumnIndex;
        private boolean isEditing = false;
        private boolean isUpdatingText = false;
        
        public DataCellViewHolder(@NonNull View itemView) {
            super(itemView);
            
            // 创建EditText作为主要组件
            editText = new EditText(itemView.getContext());
            editText.setPadding(12, 8, 12, 8);
            editText.setTextSize(14);
            editText.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
            editText.setBackground(null);
            editText.setSingleLine(true);
            editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            
            // 将EditText添加到容器中
            FrameLayout container = itemView.findViewById(R.id.cell_container);
            if (container != null) {
                container.addView(editText);
            } else {
                // 如果没有容器，直接添加到itemView
                ((ViewGroup) itemView).addView(editText);
            }
            
            setupEditText();
        }
        
        private void setupEditText() {
            // 多指触控保护：吞掉多指事件，避免同时触发多个点击/焦点
            editText.setOnTouchListener((v, event) -> {
                if (event.getPointerCount() > 1) {
                    return true; // 消费多指事件
                }
                return false; // 允许单指事件继续传递
            });
            
            // 点击开始编辑
            editText.setOnClickListener(v -> startEditing());
            
            // 焦点变化监听
            editText.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    startEditing();
                } else {
                    finishEditing();
                }
            });
            
            // 输入完成监听
            editText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    finishEditing();
                    return true;
                }
                return false;
            });
            
            // 文本变化监听（实时保存）
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    // 只有在用户编辑时才触发保存，避免程序设置文本时触发
                    if (isEditing && !isUpdatingText && listener != null) {
                        listener.onCellContentChanged(currentRowIndex, currentColumnIndex, s.toString());
                    }
                }
            });
        }
        
        private void startEditing() {
            if (!isEditing) {
                isEditing = true;
                // 使用新的beginEditing API
                EditingStateHolder.beginEditing(this);
                
                // 显式设置UI状态
                editText.setEnabled(true);
                editText.setFocusable(true);
                editText.setFocusableInTouchMode(true);
                editText.setCursorVisible(true);
                editText.requestFocus();
                editText.setSelection(editText.getText().length());
                
                // 显示软键盘
                editText.post(() -> {
                    InputMethodManager imm = (InputMethodManager) itemView.getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        }
        
        private void finishEditing() {
            if (isEditing) {
                isEditing = false;
                // 使用新的endEditing API
                EditingStateHolder.endEditing(this);
                
                // 重置UI状态，但保持可点击以便重新编辑
                editText.setCursorVisible(false);
                editText.setFocusable(true); // 保持可获得焦点
                editText.setFocusableInTouchMode(true); // 保持触摸模式下可获得焦点
                editText.setEnabled(true); // 保持启用状态
                editText.clearFocus();
                
                // 隐藏软键盘
                InputMethodManager imm = (InputMethodManager) itemView.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                }
            }
        }
        
        @Override
        public void forceFinishEditing() {
            finishEditing();
        }
        
        @Override
        public String getCellIdentifier() {
            return currentRowIndex + "_" + currentColumnIndex;
        }
        
        public void bind(Cell cell, Column column, int columnIndex) {
            this.currentRowIndex = rowIndex;
            this.currentColumnIndex = columnIndex;
            
            // 设置内容时避免触发TextWatcher，并且只在内容真正改变时才更新
            String content = cell.getContent() != null ? cell.getContent() : "";
            String currentText = editText.getText().toString();
            
            // 只有在内容真正改变且不在编辑状态时才更新文本，避免光标跳转
            if (!content.equals(currentText) && !isEditing) {
                isUpdatingText = true;
                editText.setText(content);
                isUpdatingText = false;
            }
            
            // 检查是否为当前编辑单元格，确保回收复用时状态正确
            boolean shouldBeEditing = EditingStateHolder.isCurrentEditingCell(this);
            if (shouldBeEditing && !isEditing) {
                // 如果应该编辑但当前不是编辑状态，开始编辑
                startEditing();
            } else if (!shouldBeEditing && isEditing) {
                // 如果不应该编辑但当前是编辑状态，结束编辑
                finishEditing();
            } else if (!shouldBeEditing) {
                // 确保非编辑单元格的UI状态正确（防止回收复用带来的状态污染）
                editText.setCursorVisible(false);
                editText.setFocusable(true); // 保持可获得焦点以响应点击
                editText.setFocusableInTouchMode(true); // 保持触摸模式下可获得焦点
                editText.setEnabled(true); // 保持可用以响应点击
                editText.clearFocus();
                isEditing = false;
            }
            
            // 使用ColumnWidthProvider获取effective尺寸
            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
            if (widthProvider != null) {
                layoutParams.width = widthProvider.getColumnWidthPx(columnIndex);
                // 使用行索引获取该行的具体高度
                layoutParams.height = widthProvider.getRowHeightPx(currentRowIndex);
                
                // 设置文本大小
                editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, widthProvider.getTextSizePx());
            } else {
                // 回退到原始方式
                layoutParams.width = (int) (column.getWidth() * itemView.getContext().getResources().getDisplayMetrics().density);
                layoutParams.height = (int) (rowHeightDp * itemView.getContext().getResources().getDisplayMetrics().density);
            }
            itemView.setLayoutParams(layoutParams);
            
            // 设置长按监听器
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onCellLongClick(currentRowIndex, currentColumnIndex);
                }
                return true;
            });
        }
    }
}