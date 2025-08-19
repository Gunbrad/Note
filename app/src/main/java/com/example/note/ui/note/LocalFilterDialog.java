package com.example.note.ui.note;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;
import com.example.note.adapter.FilterOptionAdapter;
import com.example.note.data.entity.Column;
import com.example.note.data.model.FilterOption;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * 本地筛选器对话框
 * 根据修改建议.md实现DataGrip风格的筛选界面
 */
public class LocalFilterDialog extends Dialog {
    
    private Column column;
    private List<FilterOption> filterOptions;
    private OnFilterListener listener;
    
    private TextView tvColumnName;
    private ImageButton btnClose;
    private EditText etSearch;
    private CheckBox cbSelectAll;
    private RecyclerView rvFilterValues;
    private TextView tvSelectionInfo;
    private MaterialButton btnOk;
    private MaterialButton btnCancel;
    
    private FilterOptionAdapter adapter;
    
    public interface OnFilterListener {
        void onFilterApplied(Column column, List<FilterOption> filterOptions);
        void onFilterCleared(Column column);
    }
    
    public LocalFilterDialog(@NonNull Context context, Column column, List<FilterOption> filterOptions, OnFilterListener listener) {
        super(context);
        this.column = column;
        this.filterOptions = filterOptions;
        this.listener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_local_filter);
        
        initViews();
        setupRecyclerView();
        setupListeners();
        updateUI();
    }
    
    private void initViews() {
        tvColumnName = findViewById(R.id.tv_column_name);
        btnClose = findViewById(R.id.btn_close);
        etSearch = findViewById(R.id.et_search);
        cbSelectAll = findViewById(R.id.cb_select_all);
        rvFilterValues = findViewById(R.id.rv_filter_values);
        tvSelectionInfo = findViewById(R.id.tv_selection_info);
        btnOk = findViewById(R.id.btn_ok);
        btnCancel = findViewById(R.id.btn_cancel);
        
        // 设置列名
        if (column != null) {
            tvColumnName.setText(String.format("'%s' 的本地筛选器", column.getName()));
        }
    }
    
    private void setupRecyclerView() {
        rvFilterValues.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FilterOptionAdapter(filterOptions);
        rvFilterValues.setAdapter(adapter);
        
        adapter.setOnSelectionChangeListener(this::updateSelectionUI);
    }
    
    private void setupListeners() {
        // 关闭按钮
        btnClose.setOnClickListener(v -> dismiss());
        
        // 搜索功能
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // 全选/取消全选
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (adapter != null) {
                adapter.selectAll(isChecked);
            }
        });
        
        // 按钮事件
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnOk.setOnClickListener(v -> {
            if (adapter != null && listener != null) {
                List<FilterOption> currentOptions = adapter.getFilterOptions();
                
                // DataGrip风格逻辑：检查选择状态
                boolean hasSelection = false;
                boolean allSelected = true;
                
                for (FilterOption option : currentOptions) {
                    if (option.checked) {
                        hasSelection = true;
                    } else {
                        allSelected = false;
                    }
                }
                
                // 未选中任何值或全选时清除筛选，否则应用筛选
                if (!hasSelection || allSelected) {
                    listener.onFilterCleared(column);
                } else {
                    listener.onFilterApplied(column, currentOptions);
                }
            }
            dismiss();
        });
    }
    
    private void updateUI() {
        updateSelectionUI();
    }
    
    private void updateSelectionUI() {
        if (adapter == null) return;
        
        int selectedCount = adapter.getSelectedCount();
        int totalCount = adapter.getTotalCount();
        boolean isAllSelected = adapter.isAllSelected();
        
        // 更新全选复选框状态
        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(isAllSelected);
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (adapter != null) {
                adapter.selectAll(isChecked);
            }
        });
        
        // 更新选择信息
        if (selectedCount == 0) {
            tvSelectionInfo.setText("未选择任何项");
        } else if (selectedCount == totalCount) {
            tvSelectionInfo.setText(String.format("已选择全部 %d 项", totalCount));
        } else {
            tvSelectionInfo.setText(String.format("已选择 %d / %d 项", selectedCount, totalCount));
        }
    }
    
    public static void show(Context context, Column column, List<FilterOption> filterOptions, OnFilterListener listener) {
        LocalFilterDialog dialog = new LocalFilterDialog(context, column, filterOptions, listener);
        dialog.show();
    }
}