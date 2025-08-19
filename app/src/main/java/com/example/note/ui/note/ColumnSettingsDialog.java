package com.example.note.ui.note;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.note.R;
import com.example.note.data.entity.Column;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

/**
 * 列设置对话框
 * 用于修改列的名称、宽度、显示状态等
 */
public class ColumnSettingsDialog extends Dialog {
    
    private Column column;
    private OnColumnSettingsListener listener;
    
    private TextInputEditText columnNameInput;
    private SeekBar columnWidthSeekbar;
    private TextView columnWidthText;
    private SwitchMaterial columnVisibleSwitch;
    private MaterialButton btnDeleteColumn;
    private MaterialButton btnCancel;
    private MaterialButton btnConfirm;
    
    public interface OnColumnSettingsListener {
        void onColumnUpdated(Column column);
        void onColumnDeleted(Column column);
    }
    
    public ColumnSettingsDialog(@NonNull Context context, Column column, OnColumnSettingsListener listener) {
        super(context);
        this.column = column;
        this.listener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_column_settings);
        
        initViews();
        setupListeners();
        loadColumnData();
    }
    
    private void initViews() {
        columnNameInput = findViewById(R.id.column_name_input);
        columnWidthSeekbar = findViewById(R.id.column_width_seekbar);
        columnWidthText = findViewById(R.id.column_width_text);
        columnVisibleSwitch = findViewById(R.id.column_visible_switch);
        btnDeleteColumn = findViewById(R.id.btn_delete_column);
        btnCancel = findViewById(R.id.btn_cancel);
        btnConfirm = findViewById(R.id.btn_confirm);
    }
    
    private void setupListeners() {
        // 列宽调整监听
        columnWidthSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                columnWidthText.setText(progress + "dp");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 删除列按钮
        btnDeleteColumn.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("删除列")
                    .setMessage("确定要删除列 \"" + column.getName() + "\" 吗？此操作不可撤销。")
                    .setPositiveButton("删除", (dialog, which) -> {
                        if (listener != null) {
                            listener.onColumnDeleted(column);
                        }
                        dismiss();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        
        // 取消按钮
        btnCancel.setOnClickListener(v -> dismiss());
        
        // 确定按钮
        btnConfirm.setOnClickListener(v -> {
            if (validateAndSave()) {
                dismiss();
            }
        });
    }
    
    private void loadColumnData() {
        if (column != null) {
            columnNameInput.setText(column.getName());
            columnWidthSeekbar.setProgress((int) column.getWidth());
            columnWidthText.setText((int) column.getWidth() + "dp");
            columnVisibleSwitch.setChecked(column.isVisible());
        }
    }
    
    private boolean validateAndSave() {
        String name = columnNameInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(name)) {
            columnNameInput.setError("列名不能为空");
            return false;
        }
        
        // 更新列数据
        column.setName(name);
        column.setWidth((float) columnWidthSeekbar.getProgress());
        column.setVisible(columnVisibleSwitch.isChecked());
        
        if (listener != null) {
            listener.onColumnUpdated(column);
        }
        
        return true;
    }
    
    /**
     * 设置列设置监听器
     */
    public void setOnColumnSettingsListener(OnColumnSettingsListener listener) {
        this.listener = listener;
    }
    
    public static void show(Context context, Column column, OnColumnSettingsListener listener) {
        ColumnSettingsDialog dialog = new ColumnSettingsDialog(context, column, listener);
        dialog.show();
    }
}