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
 */
public class DataCellAdapter extends RecyclerView.Adapter<DataCellAdapter.DataCellViewHolder> {
    
    private List<Cell> cells = new ArrayList<>();
    private List<Column> columns = new ArrayList<>();
    private int rowIndex;
    private int rowHeightDp = 44;
    private OnCellChangeListener listener;
    
    public interface OnCellChangeListener {
        void onCellClick(int rowIndex, int columnIndex);
        void onCellLongClick(int rowIndex, int columnIndex);
        void onCellContentChanged(int rowIndex, int columnIndex, String newContent);
    }
    
    public void setOnCellChangeListener(OnCellChangeListener listener) {
        this.listener = listener;
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
        if (this.rowHeightDp != heightDp) {
            this.rowHeightDp = heightDp;
            notifyDataSetChanged();
        }
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
    
    class DataCellViewHolder extends RecyclerView.ViewHolder {
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
                editText.setEnabled(true);
                editText.setFocusable(true);
                editText.setFocusableInTouchMode(true);
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
                editText.clearFocus();
                
                // 隐藏软键盘
                InputMethodManager imm = (InputMethodManager) itemView.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                }
            }
        }
        
        public void bind(Cell cell, Column column, int columnIndex) {
            this.currentRowIndex = rowIndex;
            this.currentColumnIndex = columnIndex;
            
            // 设置内容时避免触发TextWatcher
            String content = cell.getContent() != null ? cell.getContent() : "";
            isUpdatingText = true;
            editText.setText(content);
            isUpdatingText = false;
            
            // 设置尺寸
            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
            layoutParams.width = (int) (column.getWidth() * itemView.getContext().getResources().getDisplayMetrics().density);
            layoutParams.height = (int) (rowHeightDp * itemView.getContext().getResources().getDisplayMetrics().density);
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