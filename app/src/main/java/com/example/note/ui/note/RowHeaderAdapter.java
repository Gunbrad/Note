package com.example.note.ui.note;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 行头适配器 - 确保行头数量与实际行数完全匹配
 */
public class RowHeaderAdapter extends RecyclerView.Adapter<RowHeaderAdapter.RowHeaderViewHolder> {
    
    private int rowCount = 0;
    private int rowHeightDp = 44; // 默认行高
    private OnRowClickListener listener;
    
    public interface OnRowClickListener {
        void onRowClick(int rowIndex);
        void onRowLongClick(int rowIndex);
        void onRowResize(int rowIndex, int newHeight);
    }
    
    public void setOnRowClickListener(OnRowClickListener listener) {
        this.listener = listener;
    }
    
    public void setRowCount(int rowCount) {
        int newRowCount = Math.max(0, rowCount);
        int oldRowCount = this.rowCount;
        
        if (newRowCount > oldRowCount) {
            // 添加了新行，使用精确通知
            this.rowCount = newRowCount;
            notifyItemRangeInserted(oldRowCount, newRowCount - oldRowCount);
        } else if (newRowCount < oldRowCount) {
            // 删除了行，使用精确通知
            this.rowCount = newRowCount;
            notifyItemRangeRemoved(newRowCount, oldRowCount - newRowCount);
        } else if (oldRowCount == 0) {
            // 首次设置或从0开始，使用完整刷新
            this.rowCount = newRowCount;
            notifyDataSetChanged();
        }
        // 如果行数相同，不需要通知
    }
    
    public void setRowHeight(int rowHeightDp) {
        this.rowHeightDp = rowHeightDp;
        notifyDataSetChanged();
    }
    
    public void updateRowHeight(int rowIndex, int heightDp) {
        if (rowIndex >= 0 && rowIndex < rowCount) {
            this.rowHeightDp = heightDp;
            notifyItemChanged(rowIndex);
        }
    }
    
    @NonNull
    @Override
    public RowHeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_row_header, parent, false);
        return new RowHeaderViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RowHeaderViewHolder holder, int position) {
        holder.bind(position + 1); // 行号从1开始
    }
    
    @Override
    public int getItemCount() {
        return rowCount;
    }
    
    class RowHeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView rowNumberText;
        private View rowResizeHandle;
        
        public RowHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            rowNumberText = itemView.findViewById(R.id.row_number_text);
            rowResizeHandle = itemView.findViewById(R.id.row_resize_handle);
        }
        
        public void bind(int rowNumber) {
            rowNumberText.setText(String.valueOf(rowNumber));
            
            // 设置行高度
            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
            layoutParams.height = (int) (rowHeightDp * itemView.getContext().getResources().getDisplayMetrics().density);
            itemView.setLayoutParams(layoutParams);
            
            // 点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRowClick(getAdapterPosition());
                }
            });
            
            // 长按事件
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onRowLongClick(getAdapterPosition());
                    return true;
                }
                return false;
            });
            
            // 调整行高的拖拽处理 - 优化：减少频繁回调
            rowResizeHandle.setOnTouchListener(new View.OnTouchListener() {
                private float startY;
                private int startHeight;
                private long lastUpdateTime = 0;
                private static final long UPDATE_INTERVAL = 16; // 约60fps
                
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getRawY();
                            startHeight = rowHeightDp;
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            // 限制更新频率，提高流畅性
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
                                return true;
                            }
                            lastUpdateTime = currentTime;
                            
                            float deltaY = event.getRawY() - startY;
                            int newHeight = Math.max(40, (int) (startHeight + deltaY / itemView.getContext().getResources().getDisplayMetrics().density));
                            
                            // 立即更新UI
                            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                            layoutParams.height = (int) (newHeight * itemView.getContext().getResources().getDisplayMetrics().density);
                            itemView.setLayoutParams(layoutParams);
                            
                            if (listener != null) {
                                listener.onRowResize(getAdapterPosition(), newHeight);
                            }
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            return true;
                    }
                    return false;
                }
            });
        }
    }
}