package com.example.note.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DataGrip风格的筛选值适配器
 * 显示列中的唯一值及其计数，支持复选框选择
 */
public class DataGripFilterAdapter extends RecyclerView.Adapter<DataGripFilterAdapter.FilterValueViewHolder> {
    
    public static class FilterValue {
        private String value;
        private int count;
        private boolean selected;
        
        public FilterValue(String value, int count) {
            this.value = value;
            this.count = count;
            this.selected = true; // 默认全选
        }
        
        public String getValue() {
            return value;
        }
        
        public int getCount() {
            return count;
        }
        
        public boolean isSelected() {
            return selected;
        }
        
        public void setSelected(boolean selected) {
            this.selected = selected;
        }
        
        public String getDisplayValue() {
            return TextUtils.isEmpty(value) ? "(空值)" : value;
        }
    }
    
    private List<FilterValue> originalValues;
    private List<FilterValue> filteredValues;
    private OnSelectionChangeListener listener;
    
    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount, int totalCount, boolean isAllSelected);
    }
    
    public DataGripFilterAdapter(List<FilterValue> values) {
        this.originalValues = values != null ? values : new ArrayList<>();
        this.filteredValues = new ArrayList<>(this.originalValues);
    }
    
    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public FilterValueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_datagrip_filter_value, parent, false);
        return new FilterValueViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FilterValueViewHolder holder, int position) {
        FilterValue filterValue = filteredValues.get(position);
        holder.bind(filterValue);
    }
    
    @Override
    public int getItemCount() {
        return filteredValues.size();
    }
    
    /**
     * 筛选值列表
     */
    public void filter(String query) {
        filteredValues.clear();
        
        if (TextUtils.isEmpty(query)) {
            filteredValues.addAll(originalValues);
        } else {
            String lowerQuery = query.toLowerCase();
            for (FilterValue value : originalValues) {
                if (value.getDisplayValue().toLowerCase().contains(lowerQuery)) {
                    filteredValues.add(value);
                }
            }
        }
        
        notifyDataSetChanged();
    }
    
    /**
     * 全选/取消全选
     */
    public void selectAll(boolean selected) {
        for (FilterValue value : originalValues) {
            value.setSelected(selected);
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }
    
    /**
     * 获取选中的值
     */
    public Set<String> getSelectedValues() {
        Set<String> selectedValues = new HashSet<>();
        for (FilterValue value : originalValues) {
            if (value.isSelected()) {
                selectedValues.add(value.getValue());
            }
        }
        return selectedValues;
    }
    
    /**
     * 设置选中的值
     */
    public void setSelectedValues(Set<String> selectedValues) {
        for (FilterValue value : originalValues) {
            value.setSelected(selectedValues.contains(value.getValue()));
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }
    
    /**
     * 获取选中数量
     */
    public int getSelectedCount() {
        int count = 0;
        for (FilterValue value : originalValues) {
            if (value.isSelected()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 是否全选
     */
    public boolean isAllSelected() {
        return getSelectedCount() == originalValues.size();
    }
    
    /**
     * 是否有选中项
     */
    public boolean hasSelection() {
        return getSelectedCount() > 0;
    }
    
    /**
     * 是否无选中项
     */
    public boolean hasNoSelection() {
        return getSelectedCount() == 0;
    }
    
    private void notifySelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(getSelectedCount(), originalValues.size(), isAllSelected());
        }
    }
    
    class FilterValueViewHolder extends RecyclerView.ViewHolder {
        private CheckBox cbValue;
        private TextView tvValue;
        private TextView tvCount;
        
        public FilterValueViewHolder(@NonNull View itemView) {
            super(itemView);
            cbValue = itemView.findViewById(R.id.cb_value);
            tvValue = itemView.findViewById(R.id.tv_value);
            tvCount = itemView.findViewById(R.id.tv_count);
            
            // 设置整行点击事件
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < filteredValues.size()) {
                    FilterValue filterValue = filteredValues.get(position);
                    filterValue.setSelected(!filterValue.isSelected());
                    cbValue.setChecked(filterValue.isSelected());
                    notifySelectionChanged();
                }
            });
        }
        
        public void bind(FilterValue filterValue) {
            cbValue.setChecked(filterValue.isSelected());
            tvValue.setText(filterValue.getDisplayValue());
            tvCount.setText(String.valueOf(filterValue.getCount()));
        }
    }
}