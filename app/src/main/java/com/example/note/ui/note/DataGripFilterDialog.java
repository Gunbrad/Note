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
import com.example.note.adapter.DataGripFilterAdapter;
import com.example.note.data.entity.Column;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DataGrip风格的列筛选对话框
 * 支持显示值和计数，以及复选框多选功能
 * 不勾选或全选时显示全部数据，否则只显示勾选的数据
 */
public class DataGripFilterDialog extends Dialog {
    
    private Column column;
    private List<String> columnValues;
    private OnFilterListener listener;
    
    private TextView tvColumnName;
    private ImageButton btnClose;
    private EditText etSearch;
    private CheckBox cbSelectAll;
    private RecyclerView rvFilterValues;
    private TextView tvSelectionInfo;
    private MaterialButton btnOk;
    private MaterialButton btnCancel;
    
    private DataGripFilterAdapter adapter;
    
    public interface OnFilterListener {
        void onFilterApplied(Column column, Set<String> selectedValues);
        void onFilterCleared(Column column);
    }
    
    public DataGripFilterDialog(@NonNull Context context, Column column, List<String> columnValues, OnFilterListener listener) {
        super(context);
        this.column = column;
        this.columnValues = columnValues != null ? columnValues : new ArrayList<>();
        this.listener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_datagrip_filter);
        
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
        
        // 统计值的出现次数
        Map<String, Integer> valueCountMap = new HashMap<>();
        for (String value : columnValues) {
            String key = value != null ? value : "";
            valueCountMap.put(key, valueCountMap.getOrDefault(key, 0) + 1);
        }
        
        // 创建FilterValue列表
        List<DataGripFilterAdapter.FilterValue> filterValues = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : valueCountMap.entrySet()) {
            filterValues.add(new DataGripFilterAdapter.FilterValue(entry.getKey(), entry.getValue()));
        }
        
        // 按值排序
        filterValues.sort((a, b) -> {
            String val1 = a.getValue();
            String val2 = b.getValue();
            
            // 空值排在最后
            if (val1.isEmpty() && val2.isEmpty()) return 0;
            if (val1.isEmpty()) return 1;
            if (val2.isEmpty()) return -1;
            
            return val1.compareToIgnoreCase(val2);
        });
        
        adapter = new DataGripFilterAdapter(filterValues);
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
                Set<String> selectedValues = adapter.getSelectedValues();
                
                // DataGrip风格逻辑：不勾选或全选时显示全部数据，否则只显示勾选的数据
                if (adapter.hasNoSelection() || adapter.isAllSelected()) {
                    // 无选中或全选时，清除筛选显示全部数据
                    listener.onFilterCleared(column);
                } else {
                    // 有部分选中时，应用筛选
                    listener.onFilterApplied(column, selectedValues);
                }
            }
            dismiss();
        });
    }
    
    private void updateUI() {
        if (adapter != null) {
            updateSelectionUI(adapter.getSelectedCount(), adapter.getItemCount(), adapter.isAllSelected());
        }
    }
    
    private void updateSelectionUI(int selectedCount, int totalCount, boolean isAllSelected) {
        // 更新选择信息
        if (selectedCount == 0) {
            tvSelectionInfo.setText("选择一个复选框来筛选行");
        } else if (isAllSelected) {
            tvSelectionInfo.setText(String.format("已选择全部 %d 项", totalCount));
        } else {
            tvSelectionInfo.setText(String.format("已选择 %d / %d 项", selectedCount, totalCount));
        }
        
        // 更新全选复选框状态
        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(isAllSelected);
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (adapter != null) {
                adapter.selectAll(isChecked);
            }
        });
    }
    
    /**
     * 设置当前选中的值
     */
    public void setSelectedValues(Set<String> selectedValues) {
        if (adapter != null && selectedValues != null) {
            adapter.setSelectedValues(selectedValues);
        }
    }
    
    public static void show(Context context, Column column, List<String> columnValues, OnFilterListener listener) {
        DataGripFilterDialog dialog = new DataGripFilterDialog(context, column, columnValues, listener);
        dialog.show();
    }
}