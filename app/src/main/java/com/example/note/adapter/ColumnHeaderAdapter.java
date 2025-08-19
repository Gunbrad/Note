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
import com.example.note.ui.note.DataGripFilterDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import android.app.Activity;

/**
 * 列头适配器 - 显示列名、排序、筛选和拖拽调整列宽功能
 */
public class ColumnHeaderAdapter extends RecyclerView.Adapter<ColumnHeaderAdapter.ColumnHeaderViewHolder> {
    
    // 排序状态枚举
    public enum SortState {
        NONE,    // 默认排序
        ASC,     // 升序
        DESC     // 降序
    }
    
    private List<Column> columns;
    private OnColumnResizeListener resizeListener;
    private OnColumnNameChangeListener nameChangeListener;
    private OnColumnSortListener sortListener;
    private OnColumnFilterListener filterListener;
    private Handler saveHandler = new Handler(Looper.getMainLooper());
    private Runnable saveRunnable;
    private android.content.Context context;
    private com.example.note.ui.note.NoteViewModel noteViewModel;
    private long currentNotebookId;
    
    // 排序状态跟踪
    private SortState[] sortStates;
    private int currentSortColumn = -1;
    
    // 筛选状态跟踪
    private boolean[] filterStates;
    
    public interface OnColumnResizeListener {
        void onColumnResize(int columnIndex, float newWidth);
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
    
    public void setNoteViewModel(com.example.note.ui.note.NoteViewModel noteViewModel) {
        this.noteViewModel = noteViewModel;
    }
    
    public void setCurrentNotebookId(long notebookId) {
        this.currentNotebookId = notebookId;
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
        
        notifyDataSetChanged();
    }
    
    class ColumnHeaderViewHolder extends RecyclerView.ViewHolder {
        private EditText columnTitle;
        private ImageView sortButton;
        private ImageView filterButton;
        private View filterIndicator;
        private View resizeHandle;
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
            
            // 显示列名（不触发TextWatcher）
            columnTitle.setText(column.getName());
            
            // 设置列宽 - 与单元格保持一致
            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
            layoutParams.width = (int) (column.getWidth() * itemView.getContext().getResources().getDisplayMetrics().density);
            itemView.setLayoutParams(layoutParams);
            
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
            showImprovedFilterDialog(position);
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
    
    private void showImprovedFilterDialog(int columnIndex) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            
            // 获取列的所有值
            List<String> columnValues = getColumnValues(columnIndex);
            
            DataGripFilterDialog dialog = new DataGripFilterDialog(
                activity, 
                columns.get(columnIndex), 
                columnValues,
                new DataGripFilterDialog.OnFilterListener() {
                    @Override
                    public void onFilterApplied(Column column, Set<String> selectedValues) {
                        if (filterListener != null) {
                            filterListener.onColumnFilterApplied(columnIndex, selectedValues);
                        }
                        // DataGrip风格：根据选择状态更新筛选指示器
                        filterStates[columnIndex] = selectedValues != null && !selectedValues.isEmpty();
                        notifyItemChanged(columnIndex);
                    }
                    
                    @Override
                    public void onFilterCleared(Column column) {
                        if (filterListener != null) {
                            filterListener.onColumnFilterClear(columnIndex);
                        }
                        filterStates[columnIndex] = false;
                        notifyItemChanged(columnIndex);
                    }
                }
            );
            
            dialog.show();
        }
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
    
    // 设置拖拽手柄的触摸监听器
    private void setupResizeHandle(ColumnHeaderViewHolder holder, int position) {
        if (holder.resizeHandle != null) {
            holder.resizeHandle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            holder.startX = event.getRawX();
                            holder.startWidth = holder.itemView.getLayoutParams().width;
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            float deltaX = event.getRawX() - holder.startX;
                            float newWidth = Math.max(80, holder.startWidth + deltaX); // 最小宽度80dp
                            
                            // 实时更新列宽
                            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
                            params.width = (int) newWidth;
                            holder.itemView.setLayoutParams(params);
                            
                            // 通知监听器
                            if (resizeListener != null) {
                                float widthInDp = newWidth / holder.itemView.getContext().getResources().getDisplayMetrics().density;
                                resizeListener.onColumnResize(position, widthInDp);
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
        
        // 清除其他点击监听器
        holder.columnTitle.setOnClickListener(null);
        holder.columnTitle.setOnLongClickListener(null);
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);
    }
}