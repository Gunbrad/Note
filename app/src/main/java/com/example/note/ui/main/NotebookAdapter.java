package com.example.note.ui.main;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.PopupMenu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;
import com.example.note.data.entity.Notebook;
import com.example.note.util.ColorUtils;

import com.example.note.data.dao.CellDao;
import com.example.note.data.dao.ColumnDao;
import com.example.note.data.database.AppDatabase;

import java.util.Objects;
import com.google.android.material.card.MaterialCardView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 笔记本适配器
 * 用于在主页瀑布流中显示笔记本卡片
 * 支持分组显示（置顶、普通、归档）
 */
public class NotebookAdapter extends ListAdapter<Notebook, NotebookAdapter.NotebookViewHolder> {
    
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnMenuActionListener onMenuActionListener;
    private CellDao cellDao;
    private ColumnDao columnDao;
    private ExecutorService executor;

    public NotebookAdapter(Context context) {
        super(DIFF_CALLBACK);
        setHasStableIds(true);
        AppDatabase database = AppDatabase.getInstance(context);
        this.cellDao = database.cellDao();
        this.columnDao = database.columnDao();
        this.executor = Executors.newCachedThreadPool();
    }
    
    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }
    
    // 接口定义
    public interface OnItemClickListener {
        void onItemClick(long notebookId);
    }
    
    public interface OnItemLongClickListener {
        void onItemLongClick(Notebook notebook);
    }
    
    public interface OnMenuActionListener {
        void onDeleteNotebook(Notebook notebook);
        void onPinNotebook(Notebook notebook);
    }
    
    // Setter方法
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }
    
    public void setOnMenuActionListener(OnMenuActionListener listener) {
        this.onMenuActionListener = listener;
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
        private final ImageView moreIcon;
        private final TextView titleText;
        private final TextView subtitleText;

        private final ImageView previewImage;
        private final ImageView notebookIcon;
        private final TextView dimensionsText;
        
        public NotebookViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            titleText = itemView.findViewById(R.id.title_text_view);
            statusIcon = itemView.findViewById(R.id.status_icon);

            moreIcon = itemView.findViewById(R.id.more_icon);
            notebookIcon = itemView.findViewById(R.id.notebook_icon);
            dimensionsText = itemView.findViewById(R.id.dimensions_text_view);
            
            // 这些元素在新布局中不存在，设为null
            colorIndicator = null;
            subtitleText = null;
            previewImage = null;
            
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
            moreIcon.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showPopupMenu(v, getItem(position));
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
            

            
            // 设置卡片背景色（淡化版本）
            int backgroundColor = ColorUtils.getBackgroundColor(notebook.getColor());
            cardView.setCardBackgroundColor(backgroundColor);
            
            // 设置笔记本图标
            notebookIcon.setImageResource(R.drawable.ic_notebook_24);
            notebookIcon.setVisibility(View.VISIBLE);
            
            // 异步获取并设置尺寸信息
            loadNotebookDimensions(notebook.getId());
            
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
            
            // 隐藏状态图标（在新布局中不需要显示）
            statusIcon.setVisibility(View.GONE);
        }
        
        /**
         * 异步加载笔记本尺寸信息
         */
        private void loadNotebookDimensions(long notebookId) {
            executor.execute(() -> {
                try {
                    int maxRowIndex = cellDao.getMaxRowIndex(notebookId);
                    int maxColumnIndex = cellDao.getMaxColumnIndex(notebookId);
                    int rowCount = maxRowIndex + 1; // 索引从0开始，所以行数要+1
                    int columnCount = maxColumnIndex + 1; // 索引从0开始，所以列数要+1
                    
                    // 切换到主线程更新UI
                    dimensionsText.post(() -> {
                        String dimensions = rowCount + "行 × " + columnCount + "列";
                        dimensionsText.setText(dimensions);
                        dimensionsText.setVisibility(View.VISIBLE);
                    });
                } catch (Exception e) {
                    // 如果出错，隐藏尺寸显示
                    dimensionsText.post(() -> {
                        dimensionsText.setVisibility(View.GONE);
                    });
                }
            });
        }
        
        /**
         * 显示弹出菜单
         */
        private void showPopupMenu(View anchor, Notebook notebook) {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
            popup.getMenuInflater().inflate(R.menu.menu_notebook_popup, popup.getMenu());
            
            // 根据置顶状态更新菜单项文本
            MenuItem pinItem = popup.getMenu().findItem(R.id.action_pin);
            if (pinItem != null) {
                if (notebook.isPinned()) {
                    pinItem.setTitle("取消置顶");
                } else {
                    pinItem.setTitle("置顶笔记");
                }
            }
            
            popup.setOnMenuItemClickListener(item -> {
                if (onMenuActionListener != null) {
                    int itemId = item.getItemId();
                    if (itemId == R.id.action_delete) {
                        onMenuActionListener.onDeleteNotebook(notebook);
                        return true;
                    } else if (itemId == R.id.action_pin) {
                        onMenuActionListener.onPinNotebook(notebook);
                        return true;
                    }
                }
                return false;
            });
            
            popup.show();
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
            return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                   Objects.equals(oldItem.getDescription(), newItem.getDescription()) &&
                   oldItem.getUpdatedAt() == newItem.getUpdatedAt() &&
                   Objects.equals(oldItem.getColor(), newItem.getColor()) &&
                   oldItem.isPinned() == newItem.isPinned() &&
                   oldItem.isDeleted() == newItem.isDeleted();
        }
    };
}