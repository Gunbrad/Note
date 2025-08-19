package com.example.note.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;
import com.example.note.data.entity.Notebook;
import com.example.note.util.ColorUtils;
import com.example.note.util.DateUtils;
import com.google.android.material.card.MaterialCardView;

/**
 * 笔记本适配器
 * 用于在主页瀑布流中显示笔记本卡片
 * 支持分组显示（置顶、普通、归档）
 */
public class NotebookAdapter extends ListAdapter<Notebook, NotebookAdapter.NotebookViewHolder> {
    
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    
    public NotebookAdapter() {
        super(DIFF_CALLBACK);
    }
    
    // 接口定义
    public interface OnItemClickListener {
        void onItemClick(long notebookId);
    }
    
    public interface OnItemLongClickListener {
        void onItemLongClick(Notebook notebook);
    }
    
    // Setter方法
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }
    
    @NonNull
    @Override
    public NotebookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notebook_card, parent, false);
        return new NotebookViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull NotebookViewHolder holder, int position) {
        Notebook notebook = getItem(position);
        holder.bind(notebook);
    }
    
    /**
     * ViewHolder类
     */
    public class NotebookViewHolder extends RecyclerView.ViewHolder {
        
        private final MaterialCardView cardView;
        private final View colorIndicator;
        private final ImageView statusIcon;
        private final ImageView moreButton;
        private final TextView titleText;
        private final TextView subtitleText;
        private final TextView timeText;
        private final ImageView previewImage;
        
        public NotebookViewHolder(@NonNull View itemView) {
            super(itemView);
            
            cardView = (MaterialCardView) itemView;
            colorIndicator = itemView.findViewById(R.id.color_indicator);
            statusIcon = itemView.findViewById(R.id.status_icon);
            moreButton = itemView.findViewById(R.id.more_icon);
            titleText = itemView.findViewById(R.id.title_text_view);
            subtitleText = itemView.findViewById(R.id.subtitle_text_view);
            timeText = itemView.findViewById(R.id.time_text_view);
            previewImage = itemView.findViewById(R.id.preview_image);
            
            // 设置点击监听器
            cardView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener.onItemClick(getItem(position).getId());
                    }
                }
            });
            
            // 设置长按监听器
            cardView.setOnLongClickListener(v -> {
                if (onItemLongClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemLongClickListener.onItemLongClick(getItem(position));
                        return true;
                    }
                }
                return false;
            });
            
            // 更多按钮点击
            moreButton.setOnClickListener(v -> {
                if (onItemLongClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemLongClickListener.onItemLongClick(getItem(position));
                    }
                }
            });
        }
        
        /**
         * 绑定数据到视图
         */
        public void bind(Notebook notebook) {
            Context context = itemView.getContext();
            
            // 设置标题
            titleText.setText(notebook.getTitle());
            
            // 设置副标题（TODO: 实现单元格数量统计）
            if (notebook.getDescription() != null && !notebook.getDescription().trim().isEmpty()) {
                subtitleText.setText(notebook.getDescription());
                subtitleText.setVisibility(View.VISIBLE);
            } else {
                subtitleText.setText(context.getString(R.string.subtitle_empty));
                subtitleText.setVisibility(View.VISIBLE);
            }
            
            // 设置时间
            timeText.setText(DateUtils.formatRelativeTime(notebook.getUpdatedAt()));
            
            // 设置颜色指示器
            int color = ColorUtils.parseColor(notebook.getColor());
            colorIndicator.setBackgroundColor(color);
            
            // 设置卡片背景色（淡化版本）
            int backgroundColor = ColorUtils.getBackgroundColor(notebook.getColor());
            cardView.setCardBackgroundColor(backgroundColor);
            
            // 隐藏状态图标（当前版本不支持置顶和归档）
            statusIcon.setVisibility(View.GONE);
            
            // 根据标题行数动态调整卡片高度
            titleText.post(() -> {
                int lineCount = titleText.getLineCount();
                ViewGroup.LayoutParams params = cardView.getLayoutParams();
                
                if (lineCount > 2) {
                    // 多行标题，增加卡片高度
                    params.height = context.getResources().getDimensionPixelSize(R.dimen.notebook_card_height_large);
                } else if (lineCount > 1) {
                    // 两行标题，中等高度
                    params.height = context.getResources().getDimensionPixelSize(R.dimen.notebook_card_height_medium);
                } else {
                    // 单行标题，标准高度
                    params.height = context.getResources().getDimensionPixelSize(R.dimen.notebook_card_height_small);
                }
                
                cardView.setLayoutParams(params);
            });
            
            // TODO: 设置预览缩略图
            previewImage.setVisibility(View.GONE);
        }
    }
    
    // DiffUtil回调，用于高效更新列表
    private static final DiffUtil.ItemCallback<Notebook> DIFF_CALLBACK = new DiffUtil.ItemCallback<Notebook>() {
        @Override
        public boolean areItemsTheSame(@NonNull Notebook oldItem, @NonNull Notebook newItem) {
            return oldItem.getId() == newItem.getId();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull Notebook oldItem, @NonNull Notebook newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                   oldItem.getDescription().equals(newItem.getDescription()) &&
                   oldItem.getUpdatedAt() == newItem.getUpdatedAt() &&
                   oldItem.getColor().equals(newItem.getColor());
        }
    };
}