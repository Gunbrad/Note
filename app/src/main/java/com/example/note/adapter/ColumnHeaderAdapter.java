package com.example.note.adapter;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.note.R;
import com.example.note.data.entity.Column;
import com.example.note.data.model.FilterOption;
import com.example.note.ui.note.LocalFilterDialog;
import com.example.note.ui.note.NoteViewModel;
import com.example.note.ui.note.ColumnWidthProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import android.app.Activity;

/**
 * 列头适配器 - 显示列名、排序、筛选和拖拽调整列宽功能
 * 使用ColumnWidthProvider提供基于缩放的动态尺寸
 */
public class ColumnHeaderAdapter extends RecyclerView.Adapter<ColumnHeaderAdapter.ColumnHeaderViewHolder> {
    
    // 默认列宽常量
    private static final float DEFAULT_COLUMN_WIDTH_PX = 120f;
    
    // 列宽调整的最小和最大值
    private static final int MIN_COLUMN_WIDTH_DP = 50;
    private static final int MAX_COLUMN_WIDTH_DP = 600;
    
    // 工具方法
    private static int dpToPx(View v, float dp) {
        float density = v.getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
    
    // 新增：简化版 dp 转 px（无需依赖某个 View 实例）
    private int dpToPx(float dp) {
        float density = (context != null)
                ? context.getResources().getDisplayMetrics().density
                : 3f; // 兜底
        return Math.round(dp * density);
    }
    
    // 新增：确保缓存尺寸 & 首次填充
    private void ensureWidthCache() {
        int n = getItemCount();
        if (widthCachePx == null || widthCachePx.length != n) {
            widthCachePx = new int[n];
            for (int i = 0; i < n; i++) {
                // 以 Provider 为准初始化；没有则用列自身或默认
                if (widthProvider != null) {
                    widthCachePx[i] = widthProvider.getColumnWidthPx(i);
                } else if (columns != null && i < columns.size()) {
                    widthCachePx[i] = Math.max(dpToPx(MIN_COLUMN_WIDTH_DP), 
                            dpToPx(columns.get(i).getWidth() > 0 ? columns.get(i).getWidth() : DEFAULT_COLUMN_WIDTH_PX));
                } else {
                    widthCachePx[i] = dpToPx(DEFAULT_COLUMN_WIDTH_PX);
                }
            }
        }
    }
    
    /**
     * 清除宽度缓存，强制在下次bind时重新计算宽度
     * 用于缩放变化时确保使用最新的缩放因子
     */
    public void clearWidthCache() {
        widthCachePx = null;
    }
    
    // 排序状态枚举
    public enum SortState {
        NONE,    // 默认排序
        ASC,     // 升序
        DESC     // 降序
    }
    
    private List<Column> columns;
    private OnColumnResizeListener resizeListener;
    private OnColumnRealtimeUpdateListener realtimeUpdateListener;
    private OnColumnNameChangeListener nameChangeListener;
    private OnColumnSortListener sortListener;
    private OnColumnFilterListener filterListener;
    private OnColumnLongClickListener longClickListener;
    private Handler saveHandler = new Handler(Looper.getMainLooper());
    private Runnable saveRunnable;
    private android.content.Context context;
    private NoteViewModel noteViewModel;
    private long currentNotebookId;
    private ColumnWidthProvider widthProvider;
    
    // 排序状态跟踪
    private SortState[] sortStates;
    private int currentSortColumn = -1;
    
    // 筛选状态跟踪
    private boolean[] filterStates;
    
    // 新增：像素宽度缓存（与 columns 同步长度）
    private int[] widthCachePx;
    
    public interface OnColumnResizeListener {
        void onColumnResize(int columnIndex, float newWidth, boolean isFinal);
    }
    
    public interface OnColumnRealtimeUpdateListener {
        void onColumnRealtimeUpdate(int columnIndex, int newWidthPx);
    }
    
    public interface OnColumnNameChangeListener {
        void onColumnNameChange(int columnIndex, String newName);
    }
    
    public interface OnColumnSortListener {
        void onColumnSort(int columnIndex, SortState sortState);
    }
    
    public interface OnColumnFilterListener {
        void onColumnFilter(int columnIndex, String filterType, Object filterValue);
        void onColumnFilterClear(int columnIndex);
        void onColumnFilterApplied(int columnIndex, Set<String> selectedValues);
    }
    
    public interface OnColumnLongClickListener {
        void onColumnLongClick(int columnIndex);
    }
    
    public ColumnHeaderAdapter(List<Column> columns) {
        this.columns = columns != null ? columns : new ArrayList<>();
        int size = this.columns.size();
        this.sortStates = new SortState[size];
        this.filterStates = new boolean[size];
        
        // 初始化排序状态为默认
        for (int i = 0; i < sortStates.length; i++) {
            sortStates[i] = SortState.NONE;
            filterStates[i] = false;
        }
    }
    
    public void setContext(android.content.Context context) {
        this.context = context;
    }
    
    public void setNoteViewModel(NoteViewModel noteViewModel) {
        this.noteViewModel = noteViewModel;
    }
    
    public void setCurrentNotebookId(long notebookId) {
        this.currentNotebookId = notebookId;
    }
    
    public void setColumnWidthProvider(ColumnWidthProvider widthProvider) {
        this.widthProvider = widthProvider;
        // Provider 变更后重建缓存
        widthCachePx = null;
        ensureWidthCache();
    }
    
    private List<String> getColumnValues(int columnIndex) {
        List<String> values = new ArrayList<>();
        
        if (noteViewModel != null) {
            // 使用源数据而不是当前显示的筛选后数据
            List<com.example.note.data.entity.Cell> allCells = noteViewModel.getSourceCells();
            if (allCells != null) {
                // 收集指定列的所有值
                Set<String> uniqueValues = new java.util.LinkedHashSet<>();
                for (com.example.note.data.entity.Cell cell : allCells) {
                    if (cell.getColIndex() == columnIndex) {
                        String content = cell.getContent();
                        if (content == null || content.trim().isEmpty()) {
                            content = "(空白)";
                        }
                        uniqueValues.add(content);
                    }
                }
                values.addAll(uniqueValues);
            }
        }
        
        // 如果没有数据，添加默认提示
        if (values.isEmpty()) {
            values.add("(无数据)");
        }
        
        return values;
    }
    
    public void setOnColumnResizeListener(OnColumnResizeListener listener) {
        this.resizeListener = listener;
    }
    
    public void setOnColumnNameChangeListener(OnColumnNameChangeListener listener) {
        this.nameChangeListener = listener;
    }
    
    public void setOnColumnSortListener(OnColumnSortListener listener) {
        this.sortListener = listener;
    }
    
    public void setOnColumnFilterListener(OnColumnFilterListener listener) {
        this.filterListener = listener;
    }
    
    public void setOnColumnLongClickListener(OnColumnLongClickListener listener) {
        this.longClickListener = listener;
    }
    
    public void setOnColumnRealtimeUpdateListener(OnColumnRealtimeUpdateListener listener) {
        this.realtimeUpdateListener = listener;
    }
    
    @NonNull
    @Override
    public ColumnHeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_column_header, parent, false);
        return new ColumnHeaderViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ColumnHeaderViewHolder holder, int position) {
        Column column = columns.get(position);
        holder.bind(column, position);
    }
    
    @Override
    public int getItemCount() {
        return columns != null ? columns.size() : 0;
    }
    
    public void updateColumns(List<Column> newColumns) {
        this.columns = newColumns != null ? newColumns : new ArrayList<>();
        int size = this.columns.size();
        
        // 重新初始化状态数组
        this.sortStates = new SortState[size];
        this.filterStates = new boolean[size];
        
        // 根据Column实体的sortOrder同步排序状态
        currentSortColumn = -1;
        for (int i = 0; i < size; i++) {
            Column column = this.columns.get(i);
            String sortOrder = column.getSortOrder();
            
            if ("ASC".equals(sortOrder)) {
                sortStates[i] = SortState.ASC;
                currentSortColumn = i;
            } else if ("DESC".equals(sortOrder)) {
                sortStates[i] = SortState.DESC;
                currentSortColumn = i;
            } else {
                sortStates[i] = SortState.NONE;
            }
            
            // 初始化筛选状态（暂时设为false，后续可根据需要扩展）
            filterStates[i] = false;
        }
        
        // 关键：列集合变化后重建像素缓存
        widthCachePx = null;
        ensureWidthCache();
        
        notifyDataSetChanged();
    }
    
    class ColumnHeaderViewHolder extends RecyclerView.ViewHolder {
        private EditText columnTitle;
        private ImageView sortButton;
        private ImageView filterButton;
        private View filterIndicator;
        private View resizeHandle;
        private View columnRoot; // 新增：可调宽度的根容器
        private float startX;
        private float startWidth;
        private TextWatcher textWatcher;
        private String originalName;
        private boolean isEditing = false;

        public ColumnHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            columnTitle = itemView.findViewById(R.id.column_name_edit);
            sortButton = itemView.findViewById(R.id.sort_button);
            filterButton = itemView.findViewById(R.id.filter_button);
            filterIndicator = itemView.findViewById(R.id.filter_indicator);
            resizeHandle = itemView.findViewById(R.id.column_resize_handle);
            columnRoot = itemView.findViewById(R.id.column_root); // 获取可调宽度的根容器
            
            if (columnTitle == null) {
                // 如果布局中没有EditText，创建一个
                columnTitle = new EditText(itemView.getContext());
                columnTitle.setPadding(8, 8, 8, 8);
                ((ViewGroup) itemView).addView(columnTitle);
            }
        }
        
        public void bind(Column column, int position) {
            // 移除之前的监听器，避免重复监听
            if (textWatcher != null) {
                columnTitle.removeTextChangedListener(textWatcher);
            }
            columnTitle.setOnFocusChangeListener(null);
            columnTitle.setOnEditorActionListener(null);
            
            // 保存原始名称
            originalName = column.getName();
            
            // 显示列名（不触发TextWatcher），只在内容真正改变且不在编辑状态时才更新
            String columnName = column.getName();
            String currentText = columnTitle.getText().toString();
            
            if (!columnName.equals(currentText) && !isEditing) {
                columnTitle.setText(columnName);
            }
            
            // 改：同时设置外层itemView和内层columnRoot的宽度，确保列头整体宽度正确
            ensureWidthCache(); // 确保缓存已初始化
            int targetWidth = (widthCachePx != null && position < widthCachePx.length && widthCachePx[position] > 0)
                    ? widthCachePx[position]
                    : (widthProvider != null ? widthProvider.getColumnWidthPx(position) : dpToPx(DEFAULT_COLUMN_WIDTH_PX));
            
            // 设置外层itemView的宽度
            ViewGroup.LayoutParams itemLayoutParams = itemView.getLayoutParams();
            if (itemLayoutParams != null) {
                itemLayoutParams.width = targetWidth;
                itemLayoutParams.height = (widthProvider != null) ? widthProvider.getRowHeightPx() : itemLayoutParams.height;
                itemView.setLayoutParams(itemLayoutParams);
            }
            
            // 设置内层columnRoot的宽度
            if (columnRoot != null) {
                ViewGroup.LayoutParams columnLayoutParams = columnRoot.getLayoutParams();
                columnLayoutParams.width = targetWidth;
                columnLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                columnRoot.setLayoutParams(columnLayoutParams);
            }
            
            // 设置焦点变化监听器 - 只在失去焦点时保存
            columnTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus && isEditing) {
                        // 失去焦点时保存
                        saveColumnName(position);
                        isEditing = false;
                    } else if (hasFocus) {
                        isEditing = true;
                    }
                }
            });
            
            // 设置编辑完成监听器 - 按回车或完成时保存
            columnTitle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE || 
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        saveColumnName(position);
                        columnTitle.clearFocus();
                        isEditing = false;
                        return true;
                    }
                    return false;
                }
            });
            
            // 添加简单的TextWatcher只用于标记编辑状态
            textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    isEditing = true;
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            };
            columnTitle.addTextChangedListener(textWatcher);
            
            // 设置排序和筛选按钮
            setupSortButton(ColumnHeaderViewHolder.this, position);
            setupFilterButton(ColumnHeaderViewHolder.this, position);
            setupResizeHandle(ColumnHeaderViewHolder.this, position);
        }
    
    private void setupFilterButton(ColumnHeaderViewHolder holder, int position) {
        // 更新筛选指示器
        updateFilterIndicator(holder, position);
        
        // 设置筛选按钮点击事件
        holder.filterButton.setOnClickListener(v -> {
            if (noteViewModel != null && context != null && position < columns.size()) {
                Column column = columns.get(position);
                
                // 构建筛选选项
                List<FilterOption> filterOptions = noteViewModel.buildValueCountsForColumn(column.getColumnIndex());
                
                // 显示筛选对话框
                LocalFilterDialog.show(context, column, filterOptions, new LocalFilterDialog.OnFilterListener() {
                    @Override
                    public void onFilterApplied(Column column, List<FilterOption> filterOptions) {
                        // 应用筛选
                        noteViewModel.applyValueFilter(column.getColumnIndex(), filterOptions);
                        
                        // 更新筛选状态
                        filterStates[position] = true;
                        notifyItemChanged(position);
                        
                        // 通知外部监听器
                        if (filterListener != null) {
                            Set<String> selectedValues = new java.util.HashSet<>();
                            for (FilterOption option : filterOptions) {
                                if (option.checked) {
                                    selectedValues.add(option.value);
                                }
                            }
                            filterListener.onColumnFilterApplied(position, selectedValues);
                        }
                    }
                    
                    @Override
                    public void onFilterCleared(Column column) {
                        // 清除筛选
                        noteViewModel.applyValueFilter((int)column.getId(), null);
                        
                        // 更新筛选状态
                        filterStates[position] = false;
                        notifyItemChanged(position);
                        
                        // 通知外部监听器
                        if (filterListener != null) {
                            filterListener.onColumnFilterClear(position);
                        }
                    }
                });
            }
        });
    }
    
    private void updateFilterIndicator(ColumnHeaderViewHolder holder, int position) {
        // 检查数组边界，防止越界异常
        if (filterStates == null || position < 0 || position >= filterStates.length) {
            holder.filterIndicator.setVisibility(View.GONE);
            return;
        }
        
        boolean isFiltered = filterStates[position];
        if (isFiltered) {
            // 使用漏斗图标表示已筛选
            holder.filterButton.setImageResource(R.drawable.ic_filter);
            holder.filterButton.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_blue_dark));
        } else {
            // 使用漏斗图标表示未筛选
            holder.filterButton.setImageResource(R.drawable.ic_filter);
            holder.filterButton.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray));
        }
        
        // 根据筛选状态显示或隐藏筛选指示器
        holder.filterIndicator.setVisibility(filterStates[position] ? View.VISIBLE : View.GONE);
    }
    

      
      private void setupFilterMenuEvents(View popupView, PopupWindow popupWindow, int columnIndex) {
          // TODO: 实现筛选菜单的事件处理逻辑
          // 这里将在后续实现具体的筛选逻辑
          
          // 清除按钮
           View clearButton = popupView.findViewById(R.id.filter_clear_button);
           if (clearButton != null) {
               clearButton.setOnClickListener(v -> {
                   // 清除筛选
                   filterStates[columnIndex] = false;
                   notifyItemChanged(columnIndex);
                   
                   if (filterListener != null) {
                       filterListener.onColumnFilterClear(columnIndex);
                   }
                   
                   popupWindow.dismiss();
               });
           }
           
           // 应用按钮
           View applyButton = popupView.findViewById(R.id.filter_apply_button);
          if (applyButton != null) {
              applyButton.setOnClickListener(v -> {
                  // TODO: 获取筛选条件并应用
                  filterStates[columnIndex] = true;
                  notifyItemChanged(columnIndex);
                  
                  popupWindow.dismiss();
              });
          }
      }
    
    private void updateSortButtonIcon(ColumnHeaderViewHolder holder, int position) {
        // 检查数组边界，防止越界异常
        if (sortStates == null || position < 0 || position >= sortStates.length) {
            holder.sortButton.setImageResource(android.R.drawable.ic_menu_sort_alphabetically);
            return;
        }
        
        SortState state = sortStates[position];
        int iconRes;
        
        switch (state) {
            case ASC:
                // 使用向上箭头表示升序
                iconRes = android.R.drawable.arrow_up_float;
                break;
            case DESC:
                // 使用向下箭头表示降序
                iconRes = android.R.drawable.arrow_down_float;
                break;
            case NONE:
            default:
                // 使用默认排序图标
                iconRes = android.R.drawable.ic_menu_sort_alphabetically;
                break;
        }
        
        holder.sortButton.setImageResource(iconRes);
     }
    
    private void setupSortButton(ColumnHeaderViewHolder holder, int position) {
        // 更新排序按钮图标
        updateSortButtonIcon(holder, position);
        
        // 设置排序按钮点击事件
        holder.sortButton.setOnClickListener(v -> {
            // 检查数组边界，防止越界异常
            if (sortStates == null || position < 0 || position >= sortStates.length) {
                return;
            }
            
            // 切换排序状态：默认 -> 升序 -> 降序 -> 默认
            SortState currentState = sortStates[position];
            SortState newState;
            
            switch (currentState) {
                case NONE:
                    newState = SortState.ASC;
                    break;
                case ASC:
                    newState = SortState.DESC;
                    break;
                case DESC:
                default:
                    newState = SortState.NONE;
                    break;
            }
            
            // 清除其他列的排序状态
            if (newState != SortState.NONE) {
                for (int i = 0; i < sortStates.length; i++) {
                    if (i != position) {
                        sortStates[i] = SortState.NONE;
                    }
                }
                currentSortColumn = position;
            } else {
                currentSortColumn = -1;
            }
            
            // 更新当前列的排序状态
            sortStates[position] = newState;
            
            // 只更新排序按钮图标，避免递归调用
            updateSortButtonIcon(holder, position);
            
            // 回调排序监听器
            if (sortListener != null) {
                sortListener.onColumnSort(position, newState);
            }
        });
    }
        
        private void saveColumnName(int position) {
        String newName = columnTitle.getText().toString().trim();
        if (!newName.equals(originalName) && nameChangeListener != null && !newName.isEmpty()) {
            nameChangeListener.onColumnNameChange(position, newName);
            originalName = newName; // 更新原始名称
        }
    }
    
    }
    
    /**
     * 强制保存所有正在编辑的列头名称
     */
    public void forceFinishAllEditing() {
        // 遍历所有ViewHolder，保存正在编辑的列头
        for (int i = 0; i < getItemCount(); i++) {
            RecyclerView recyclerView = null;
            // 尝试从context获取RecyclerView
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                recyclerView = activity.findViewById(R.id.column_headers_recycler);
            }
            
            if (recyclerView != null) {
                ColumnHeaderViewHolder holder = (ColumnHeaderViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                if (holder != null && holder.isEditing) {
                    holder.saveColumnName(i);
                    holder.isEditing = false;
                    holder.columnTitle.clearFocus();
                }
            }
        }
    }
    
    // 设置拖拽手柄的触摸监听器（按修改建议3重构版）
    private void setupResizeHandle(ColumnHeaderViewHolder holder, int position) {
        final int minWidthPx = dpToPx(holder.itemView, MIN_COLUMN_WIDTH_DP);
        final int maxWidthPx = dpToPx(holder.itemView, MAX_COLUMN_WIDTH_DP);
        final android.view.ViewConfiguration vc =
                android.view.ViewConfiguration.get(holder.itemView.getContext());
        final int touchSlop = vc.getScaledTouchSlop();

        holder.resizeHandle.setOnTouchListener(new View.OnTouchListener() {
            private float downRawX;
            private float startWidthPx;
            private boolean dragging = false;
            private long lastFrameTime = 0L;
            private RecyclerView recyclerView;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: {
                        downRawX = event.getRawX();
                        
                        // ✅ 修改建议：使用缓存宽度作为起点，而不是widthProvider
                        ensureWidthCache();
                        startWidthPx = (widthCachePx != null && position < widthCachePx.length && widthCachePx[position] > 0)
                                ? widthCachePx[position]
                                : (holder.columnRoot != null ? holder.columnRoot.getWidth() : holder.itemView.getWidth());

                        // 获取RecyclerView引用，用于suppressLayout
                        View parent = holder.itemView;
                        while (parent != null && !(parent instanceof RecyclerView)) {
                            if (parent.getParent() instanceof View) {
                                parent = (View) parent.getParent();
                            } else {
                                break;
                            }
                        }
                        recyclerView = (parent instanceof RecyclerView) ? (RecyclerView) parent : null;

                        // 强制不要被父级拦截（水平滚动冲突）
                        ViewParent viewParent = v.getParent();
                        if (viewParent != null) viewParent.requestDisallowInterceptTouchEvent(true);
                        if (android.os.Build.VERSION.SDK_INT >= 26) {
                            v.requestPointerCapture();
                        }
                        // 触感反馈（可选）
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                        return true;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        float dx = event.getRawX() - downRawX;

                        if (!dragging && Math.abs(dx) < touchSlop) {
                            // 未超过触发阈值前，不做视觉更新，防抖
                            return true;
                        }
                        
                        if (!dragging) {
                            dragging = true;
                            // ✅ 修改建议：开始拖拽时抑制RecyclerView布局更新
                            if (recyclerView != null) {
                                recyclerView.suppressLayout(true);
                            }
                        }

                        // 计算新宽度（可放大可缩小）
                        float desiredPxF = clamp(startWidthPx + dx, minWidthPx, maxWidthPx);
                        int desiredPx = (int) desiredPxF;

                        // 简单限帧（~60fps）
                        long now = System.currentTimeMillis();
                        if (now - lastFrameTime < 16) return true;
                        lastFrameTime = now;

                        // ✅ 先更新缓存
                        ensureWidthCache();
                        if (position < getItemCount()) {
                            widthCachePx[position] = desiredPx;
                        }

                        // ✅ 修改建议：同时更新外层itemView和内层columnRoot的宽度
                        // 设置外层itemView的宽度
                        ViewGroup.LayoutParams itemLp = holder.itemView.getLayoutParams();
                        if (itemLp != null && itemLp.width != desiredPx) {
                            itemLp.width = desiredPx;
                            holder.itemView.setLayoutParams(itemLp);
                            holder.itemView.requestLayout();
                        }
                        
                        // 设置内层columnRoot的宽度
                        if (holder.columnRoot != null) {
                            ViewGroup.LayoutParams columnLp = holder.columnRoot.getLayoutParams();
                            if (columnLp.width != desiredPx) {
                                columnLp.width = desiredPx;
                                holder.columnRoot.setLayoutParams(columnLp);
                                holder.columnRoot.requestLayout();
                            }
                        }

                        // ✅ 修改建议：只调用实时更新监听器，不写入widthProvider
                        if (realtimeUpdateListener != null) {
                            realtimeUpdateListener.onColumnRealtimeUpdate(position, desiredPx);
                        }
                        
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        // 结束拖拽时，做一次最终收尾与持久化
                        float dx = event.getRawX() - downRawX;
                        int finalWidthPx = (int) clamp(startWidthPx + dx, minWidthPx, maxWidthPx);

                        // ✅ 最终同步一次缓存与 UI
                        ensureWidthCache();
                        if (position < getItemCount()) {
                            widthCachePx[position] = finalWidthPx;
                        }
                        
                        // ✅ 修改建议：同时设置外层itemView和内层columnRoot的最终宽度
                        // 设置外层itemView的最终宽度
                        ViewGroup.LayoutParams itemLp = holder.itemView.getLayoutParams();
                        if (itemLp != null) {
                            itemLp.width = finalWidthPx;
                            holder.itemView.setLayoutParams(itemLp);
                            holder.itemView.requestLayout();
                        }
                        
                        // 设置内层columnRoot的最终宽度
                        if (holder.columnRoot != null) {
                            ViewGroup.LayoutParams columnLp = holder.columnRoot.getLayoutParams();
                            columnLp.width = finalWidthPx;
                            holder.columnRoot.setLayoutParams(columnLp);
                            holder.columnRoot.requestLayout();
                        }

                        // ✅ 恢复RecyclerView布局更新
                        if (recyclerView != null) {
                            recyclerView.suppressLayout(false);
                        }

                        // ✅ 拖拽结束时统一处理Provider更新、数据库更新和表体同步
                        if (resizeListener != null) {
                            // 转换为DP值传递给监听器
                            // 注意：不要除以scale，因为Column.getWidth()存储的就是基础dp值
                            // 而widthProvider.getColumnWidthPx()已经乘以了scale
                            float density = holder.itemView.getContext().getResources()
                                    .getDisplayMetrics().density;
                            float finalWidthDp = finalWidthPx / density;
                            
                            // 调用监听器进行最终的Provider更新、数据库更新和表体同步
                            resizeListener.onColumnResize(position, finalWidthDp, true);
                        }

                        if (android.os.Build.VERSION.SDK_INT >= 26) {
                            v.releasePointerCapture();
                        }
                        ViewParent viewParent = v.getParent();
                        if (viewParent != null) viewParent.requestDisallowInterceptTouchEvent(false);
                        
                        dragging = false;
                        return true;
                    }
                }
                return false;
            }
        });
        
        // 设置列头点击事件：点击进入编辑模式
        holder.columnTitle.setOnClickListener(v -> {
            holder.columnTitle.requestFocus();
            holder.columnTitle.selectAll();
        });
        
        // 设置列头长按事件：弹出删除菜单
        holder.columnTitle.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onColumnLongClick(position);
                return true;
            }
            return false;
        });
        
        // 设置整个列头区域的长按事件（作为备选）
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onColumnLongClick(position);
                return true;
            }
            return false;
        });
        
        // 清除itemView的点击监听器，避免与EditText冲突
        holder.itemView.setOnClickListener(null);
    }
}