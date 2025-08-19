package com.example.note.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;
import com.example.note.data.model.FilterOption;

import java.util.ArrayList;
import java.util.List;

/**
 * 筛选选项适配器
 * 用于显示筛选对话框中的选项列表
 */
public class FilterOptionAdapter extends RecyclerView.Adapter<FilterOptionAdapter.ViewHolder> {
    
    private List<FilterOption> originalOptions;
    private List<FilterOption> filteredOptions;
    private OnSelectionChangeListener selectionChangeListener;
    
    public interface OnSelectionChangeListener {
        void onSelectionChanged();
    }
    
    public FilterOptionAdapter(List<FilterOption> options) {
        this.originalOptions = new ArrayList<>(options);
        this.filteredOptions = new ArrayList<>(options);
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_filter_option, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FilterOption option = filteredOptions.get(position);
        holder.bind(option);
    }
    
    @Override
    public int getItemCount() {
        return filteredOptions.size();
    }
    
    /**
     * 过滤选项
     */
    public void filter(String query) {
        filteredOptions.clear();
        
        if (query == null || query.trim().isEmpty()) {
            filteredOptions.addAll(originalOptions);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (FilterOption option : originalOptions) {
                if (option.display.toLowerCase().contains(lowerQuery)) {
                    filteredOptions.add(option);
                }
            }
        }
        
        notifyDataSetChanged();
        notifySelectionChanged();
    }
    
    /**
     * 全选/取消全选
     */
    public void selectAll(boolean selected) {
        for (FilterOption option : originalOptions) {
            option.checked = selected;
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }
    
    /**
     * 获取选中数量
     */
    public int getSelectedCount() {
        int count = 0;
        for (FilterOption option : originalOptions) {
            if (option.checked) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 获取总数量
     */
    public int getTotalCount() {
        return originalOptions.size();
    }
    
    /**
     * 是否全选
     */
    public boolean isAllSelected() {
        for (FilterOption option : originalOptions) {
            if (!option.checked) {
                return false;
            }
        }
        return !originalOptions.isEmpty();
    }
    
    /**
     * 获取筛选选项
     */
    public List<FilterOption> getFilterOptions() {
        return new ArrayList<>(originalOptions);
    }
    
    /**
     * 设置选择变化监听器
     */
    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }
    
    private void notifySelectionChanged() {
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged();
        }
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private CheckBox cbValue;
        private TextView tvValue;
        private TextView tvCount;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbValue = itemView.findViewById(R.id.cb_value);
            tvValue = itemView.findViewById(R.id.tv_value);
            tvCount = itemView.findViewById(R.id.tv_count);
            
            // 点击整个项目切换选择状态
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    FilterOption option = filteredOptions.get(position);
                    option.checked = !option.checked;
                    cbValue.setChecked(option.checked);
                    notifySelectionChanged();
                }
            });
        }
        
        public void bind(FilterOption option) {
            cbValue.setChecked(option.checked);
            tvValue.setText(option.display);
            tvCount.setText(String.valueOf(option.count));
        }
    }
}