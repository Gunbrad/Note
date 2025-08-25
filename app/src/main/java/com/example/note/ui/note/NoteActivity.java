package com.example.note.ui.note;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;
import com.example.note.adapter.ColumnHeaderAdapter;
import com.example.note.data.entity.Cell;
import com.example.note.data.entity.Column;
import com.example.note.data.repository.RowRepository;
import com.example.note.ui.note.ColumnSettingsDialog;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 笔记编辑Activity - 重新设计的类DataGrip表格界面
 * 确保行列数量与单元格完全匹配，尺寸严格绑定
 */
public class NoteActivity extends AppCompatActivity {
    
    private static final String TAG = "NoteActivity";
    public static final String EXTRA_NOTEBOOK_ID = "notebook_id";
    public static final String EXTRA_TEMPLATE_ID = "template_id";
    public static final String EXTRA_NOTEBOOK_NAME = "notebook_name";
    public static final String EXTRA_ROWS = "rows";
    public static final String EXTRA_COLS = "cols";
    public static final String EXTRA_IS_NEW = "is_new";
    
    private NoteViewModel viewModel;
    
    // UI组件
    private Toolbar mainToolbar;
    private AppBarLayout appBarLayout;
    private ProgressBar loadingProgress;
    private View emptyStateLayout;
    
    // 表格组件
    private RecyclerView columnHeadersRecycler;
    private RecyclerView rowHeadersRecycler;
    private RecyclerView dataRowsRecycler;
    private ZoomPanLayout zoomPanLayout;
    
    // 新的缩放系统
    private ZoomableRecyclerHost zoomableHost;
    private ColumnWidthProvider widthProvider;
    
    // 适配器
    private ColumnHeaderAdapter columnHeaderAdapter;
    private RowHeaderAdapter rowHeaderAdapter;
    private TableRowAdapter tableRowAdapter;
    
    // 数据
    private Map<String, Cell> cellsMap = new HashMap<>();
    private List<Column> columns;
    private int currentRowCount = 0;
    private int currentColumnCount = 0;
    private long notebookId = -1;
    
    // 优化：延迟保存Handler
    private Handler rowResizeHandler = new Handler(Looper.getMainLooper());
    private Runnable rowResizeRunnable;
    
    // 自动保存Handler
    private Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private Runnable autoSaveRunnable;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        
        initViews();
        initViewModel();
        initAdapters();
        initTableRowAdapter();
        setupObservers();
        setupClickListeners();
        
        handleIntent();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 强制保存所有正在编辑的列头
        if (columnHeaderAdapter != null) {
            columnHeaderAdapter.forceFinishAllEditing();
        }
    }
    
    private void initViews() {
        mainToolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.app_bar_layout);
        loadingProgress = findViewById(R.id.loading_progress);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        
        columnHeadersRecycler = findViewById(R.id.column_headers_recycler);
        rowHeadersRecycler = findViewById(R.id.row_headers_recycler);
        dataRowsRecycler = findViewById(R.id.data_grid_recycler);
        zoomPanLayout = findViewById(R.id.zoom_pan_layout);
        
        setSupportActionBar(mainToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        
        // 初始化RowRepository
        RowRepository rowRepository = RowRepository.getInstance(getApplication());
        
        // 初始化ColumnWidthProvider
        widthProvider = new ColumnWidthProviderImpl(viewModel, this, rowRepository);
    }
    
    private void initAdapters() {
        // 列头适配器
        columnHeaderAdapter = new ColumnHeaderAdapter(null);
        columnHeaderAdapter.setContext(this);
        columnHeaderAdapter.setNoteViewModel(viewModel);
        columnHeaderAdapter.setColumnWidthProvider(widthProvider);
        if (notebookId > 0) {
            columnHeaderAdapter.setCurrentNotebookId(notebookId);
        }
        // 设置列宽调整监听器
        columnHeaderAdapter.setOnColumnResizeListener(new ColumnHeaderAdapter.OnColumnResizeListener() {
            @Override
            public void onColumnResize(int columnIndex, float newWidth, boolean isFinal) {
                if (isFinal) {
                    // 拖拽结束时的最终更新
                    updateColumnWidthFinal(columnIndex, newWidth);
                } else {
                    // 拖拽过程中的临时更新
                    updateColumnWidth(columnIndex, newWidth);
                }
            }
        });
        
        // 设置列名变化监听器
        columnHeaderAdapter.setOnColumnNameChangeListener(new ColumnHeaderAdapter.OnColumnNameChangeListener() {
            @Override
            public void onColumnNameChange(int columnIndex, String newName) {
                updateColumnName(columnIndex, newName);
            }
        });
        
        // 设置排序监听器
        columnHeaderAdapter.setOnColumnSortListener(new ColumnHeaderAdapter.OnColumnSortListener() {
            @Override
            public void onColumnSort(int columnIndex, ColumnHeaderAdapter.SortState sortState) {
                handleColumnSort(columnIndex, sortState);
            }
        });
        
        // 设置筛选监听器
        columnHeaderAdapter.setOnColumnFilterListener(new ColumnHeaderAdapter.OnColumnFilterListener() {
            @Override
            public void onColumnFilter(int columnIndex, String filterType, Object filterValue) {
                handleColumnFilter(columnIndex, filterType, filterValue);
            }
            
            @Override
            public void onColumnFilterClear(int columnIndex) {
                handleColumnFilterClear(columnIndex);
            }
            
            @Override
            public void onColumnFilterApplied(int columnIndex, Set<String> selectedValues) {
                handleColumnFilterApplied(columnIndex, selectedValues);
            }
        });
        
        // 设置列头长按监听器
        columnHeaderAdapter.setOnColumnLongClickListener(new ColumnHeaderAdapter.OnColumnLongClickListener() {
            @Override
            public void onColumnLongClick(int columnIndex) {
                showDeleteColumnDialog(columnIndex);
            }
        });
        
        // 设置实时更新监听器
        columnHeaderAdapter.setOnColumnRealtimeUpdateListener(new ColumnHeaderAdapter.OnColumnRealtimeUpdateListener() {
            @Override
            public void onColumnRealtimeUpdate(int columnIndex, int newWidthPx) {
                // 修改建议：ACTION_MOVE期间不写入widthProvider，只更新表格主体UI
                // widthProvider的更新延迟到ACTION_UP时在onColumnResize中进行
                
                // 直接使用像素值更新表格主体的可见单元格，避免dp转换和Provider查询
                if (tableRowAdapter != null) {
                    tableRowAdapter.updateColumnWidthRealtimePx(dataRowsRecycler, columnIndex, newWidthPx);
                }
            }
        });
        
        // 使用水平LinearLayoutManager，允许程序滚动
        LinearLayoutManager columnLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        columnHeadersRecycler.setLayoutManager(columnLayoutManager);
        columnHeadersRecycler.setAdapter(columnHeaderAdapter);
        
        // ✅ 禁用ItemAnimator防止拖拽时动画干扰
        columnHeadersRecycler.setItemAnimator(null);
        
        // 删除addOnItemTouchListener全拦截代码，让父级ZoomableRecyclerHost能拿到水平手势
        
        // 行头适配器
        rowHeaderAdapter = new RowHeaderAdapter();
        rowHeaderAdapter.setColumnWidthProvider(widthProvider);
        rowHeaderAdapter.setOnRowClickListener(new RowHeaderAdapter.OnRowClickListener() {
            @Override
            public void onRowClick(int rowIndex) {
                // TODO: 处理行点击
            }
            
            @Override
            public void onRowLongClick(int rowIndex) {
                showDeleteRowDialog(rowIndex);
            }
            
            @Override
            public void onRowResize(int rowIndex, int newHeight) {
                // 更新行高 - newHeight已经是dp单位，直接使用
                updateRowHeight(rowIndex, newHeight);
                
                // 延迟保存到数据库，避免拖拽过程中频繁写入
                rowResizeHandler.removeCallbacks(rowResizeRunnable);
                rowResizeRunnable = () -> {
                    // TODO: 保存行高到数据库（如果需要持久化）
                };
                rowResizeHandler.postDelayed(rowResizeRunnable, 300); // 300ms延迟
            }
        });
        
        LinearLayoutManager rowLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rowHeadersRecycler.setLayoutManager(rowLayoutManager);
        rowHeadersRecycler.setAdapter(rowHeaderAdapter);
    }
     
     private void updateRowHeight(int rowIndex, int newHeight) {
         // 立即写回Provider，确保单一真相源
         if (widthProvider != null) {
             // 使用新的方法保存特定行的高度
             widthProvider.setRowHeightDp(rowIndex, newHeight);
         }
         // 更新行高
         if (rowHeaderAdapter != null) {
             rowHeaderAdapter.updateRowHeight(rowIndex, newHeight);
         }
         if (tableRowAdapter != null) {
             tableRowAdapter.updateRowHeight(rowIndex, newHeight);
         }
       }
     
     private void updateColumnWidth(int columnIndex, float newWidth) {
         // ✅ 拖拽过程中的临时更新，实时更新可见单元格但不触发重绑定
         if (columns != null && columnIndex < columns.size()) {
             Column column = columns.get(columnIndex);
             column.setWidth(newWidth);
             
             // ✅ 拖拽过程中实时更新Provider，让可见单元格立即响应
             if (widthProvider != null) {
                 widthProvider.setColumnWidthDp(columnIndex, newWidth);
             }
             
             // ✅ 拖拽过程中实时更新可见单元格，不触发重绑定
             if (tableRowAdapter != null) {
                 tableRowAdapter.updateColumnWidthRealtime(dataRowsRecycler, columnIndex, newWidth);
             }
             
             // ❌ 拖拽过程中暂时不更新数据库，避免频繁IO
             // viewModel.updateColumn(column);
         }
       }
       
       /**
        * 拖拽结束时的最终更新，包括Provider、数据库和表体同步
        */
       private void updateColumnWidthFinal(int columnIndex, float newWidth) {
        if (columns != null && columnIndex < columns.size()) {
            Column column = columns.get(columnIndex);
            column.setWidth(newWidth);
            
            // ✅ 拖拽结束时确保Provider也被更新
            if (widthProvider != null) {
                widthProvider.setColumnWidthDp(columnIndex, newWidth);
            }
            
            // ✅ 拖拽结束时才更新数据库
            viewModel.updateColumn(column);
            
            // ✅ 拖拽结束时才同步更新表体，避免拖拽中频繁重绑定
            if (tableRowAdapter != null) {
                // 直接更新columns引用，然后只通知一次变化
                tableRowAdapter.updateColumnWidth(columnIndex, newWidth);
            }
            
            // ✅ 确保列头和单元格宽度完全同步：使用Provider的像素值进行最终同步
            if (widthProvider != null && tableRowAdapter != null) {
                int finalWidthPx = widthProvider.getColumnWidthPx(columnIndex);
                tableRowAdapter.updateColumnWidthRealtimePx(dataRowsRecycler, columnIndex, finalWidthPx);
            }
        }
    }
      
      private void updateColumnName(int columnIndex, String newName) {
         if (columns != null && columnIndex < columns.size()) {
             Column column = columns.get(columnIndex);
             column.setName(newName);
             
             // 更新数据库中的列名
             viewModel.updateColumn(column);
         }
       }
       
       /**
        * 处理列排序
        */
       private void handleColumnSort(int columnIndex, ColumnHeaderAdapter.SortState sortState) {
           if (columns != null && columnIndex < columns.size()) {
               Column column = columns.get(columnIndex);
               
               // 调用ViewModel进行排序
               String sortOrder = null;
               switch (sortState) {
                   case ASC:
                       sortOrder = "ASC";
                       break;
                   case DESC:
                       sortOrder = "DESC";
                       break;
                   case NONE:
                   default:
                       sortOrder = null;
                       break;
               }
               
               viewModel.sortByColumn(columnIndex, sortOrder);
           }
       }
       
       /**
        * 处理列筛选
        */
       private void handleColumnFilter(int columnIndex, String filterType, Object filterValue) {
           if (columns != null && columnIndex < columns.size()) {
               // 调用ViewModel进行筛选
               String filterValueStr = filterValue != null ? filterValue.toString() : "";
               viewModel.filterByColumn(columnIndex, filterType, filterValueStr);
           }
       }
       
       /**
        * 处理列筛选清除
        */
       private void handleColumnFilterClear(int columnIndex) {
           // 重新加载原始数据，清除筛选
           viewModel.reloadData();
       }
       
       /**
        * 处理列筛选应用（新的多选筛选）
        */
       private void handleColumnFilterApplied(int columnIndex, Set<String> selectedValues) {
           if (columns != null && columnIndex < columns.size()) {
               // 调用ViewModel进行多选筛选
               viewModel.filterByColumnValues(columnIndex, selectedValues);
           }
       }
     
     private void initTableRowAdapter() {
         // 表格行适配器
        tableRowAdapter = new TableRowAdapter();
        tableRowAdapter.setColumnWidthProvider(widthProvider);
        tableRowAdapter.setCellChangeListener(new DataCellAdapter.OnCellChangeListener() {
            @Override
            public void onCellClick(int rowIndex, int columnIndex) {
                // 内联编辑模式下不需要处理点击事件
            }
            
            @Override
            public void onCellLongClick(int rowIndex, int columnIndex) {
                // 处理单元格长按 - 简化处理，不显示样式对话框
            }
            
            @Override
            public void onCellContentChanged(int rowIndex, int columnIndex, String newContent) {
                // 自动保存单元格内容
                updateCellContent(rowIndex, columnIndex, newContent);
            }
        });
        
        LinearLayoutManager dataLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        dataRowsRecycler.setLayoutManager(dataLayoutManager);
        dataRowsRecycler.setAdapter(tableRowAdapter);
        
        // 初始化ZoomableRecyclerHost
        initZoomableHost();
        
        // 同步滚动
        // 滚动同步现在由ZoomableRecyclerHost处理
    }
    
    private void initZoomableHost() {
        // 创建ZoomableRecyclerHost
        zoomableHost = new ZoomableRecyclerHost(
            this,
            widthProvider,
            dataRowsRecycler,     // bodyRV
            columnHeadersRecycler, // headerRV  
            rowHeadersRecycler,   // frozenRV
            viewModel
        );
        
        // 如果有ZoomPanLayout容器，将其替换为ZoomableRecyclerHost
        if (zoomPanLayout != null && zoomPanLayout.getParent() instanceof android.view.ViewGroup) {
            android.view.ViewGroup parent = (android.view.ViewGroup) zoomPanLayout.getParent();
            int index = parent.indexOfChild(zoomPanLayout);
            parent.removeView(zoomPanLayout);
            parent.addView(zoomableHost, index, zoomPanLayout.getLayoutParams());
            
            // 将原来ZoomPanLayout的子视图移动到ZoomableRecyclerHost
            while (zoomPanLayout.getChildCount() > 0) {
                View child = zoomPanLayout.getChildAt(0);
                zoomPanLayout.removeView(child);
                zoomableHost.addView(child);
            }
        }
        
        // 设置视口状态为最大缩放状态
        viewModel.updateViewport(2.5f, 0f, 0f);
        
        // 恢复视口状态
        zoomableHost.restoreViewport();
    }
    
    // setupScrollSync方法已移除，滚动同步现在由ZoomableRecyclerHost处理
    
    private void setupObservers() {
        // 观察当前笔记本，用于预加载行高数据
        viewModel.getCurrentNotebook().observe(this, notebook -> {
            if (notebook != null && widthProvider instanceof ColumnWidthProviderImpl) {
                // 预加载行高数据到缓存，避免主线程访问数据库
                ColumnWidthProviderImpl impl = (ColumnWidthProviderImpl) widthProvider;
                impl.preloadRowHeights(notebook.getId(), 100); // 预加载前100行的数据
            }
        });
        
        // 观察列数据
        viewModel.getColumns().observe(this, columns -> {
            if (columns != null) {
                this.columns = columns;
                currentColumnCount = columns.size();
                columnHeaderAdapter.updateColumns(columns);
                updateTableData();
            }
        });
        
        // 观察行数
        viewModel.getRowCount().observe(this, rowCount -> {
            if (rowCount != null) {
                currentRowCount = rowCount;
                rowHeaderAdapter.setRowCount(rowCount);
                updateTableData();
            }
        });
        
        // 观察单元格数据
        viewModel.getFrozenColumnCells().observe(this, cells -> {
            updateCellsMap(cells);
        });
        
        viewModel.getScrollableColumnsCells().observe(this, cells -> {
            updateCellsMap(cells);
        });
        
        // 观察加载状态
        viewModel.getIsLoading().observe(this, isLoading -> {
            loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        // 观察错误信息
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 观察撤销重做状态
        viewModel.getCanUndo().observe(this, canUndo -> {
            // 更新撤销按钮状态
        });
        
        // 观察视口状态变化 - 现在由ZoomableRecyclerHost处理
        // ZoomableRecyclerHost会自动同步ViewModel的状态变化
        
        // 观察保存状态，实现自动保存功能
        viewModel.getIsSaved().observe(this, isSaved -> {
            if (isSaved != null && !isSaved) {
                // 当有未保存的更改时，延迟自动保存
                autoSaveHandler.removeCallbacks(autoSaveRunnable);
                autoSaveRunnable = () -> {
                    viewModel.saveNotebook();
                    // 自动保存完成提示已删除
                };
                autoSaveHandler.postDelayed(autoSaveRunnable, 2000); // 2秒延迟自动保存
            }
        });

    }
    
    private void updateCellsMap(List<Cell> cells) {
        if (cells != null) {
            for (Cell cell : cells) {
                String cellKey = cell.getRowIndex() + "_" + cell.getColIndex();
                cellsMap.put(cellKey, cell);
            }
            updateTableData();
        }
    }
    
    private void updateTableData() {
        if (currentRowCount > 0 && currentColumnCount > 0) {
            // 确保所有行列都有对应的单元格
            for (int row = 0; row < currentRowCount; row++) {
                for (int col = 0; col < currentColumnCount; col++) {
                    String cellKey = row + "_" + col;
                    if (!cellsMap.containsKey(cellKey)) {
                        Cell emptyCell = new Cell();
                        emptyCell.setRowIndex(row);
                        emptyCell.setColIndex(col);
                        emptyCell.setContent("");
                        cellsMap.put(cellKey, emptyCell);
                    }
                }
            }
            
            // 更新表格适配器
            tableRowAdapter.setData(currentRowCount, viewModel.getColumns().getValue(), cellsMap);
            
            // 显示表格，隐藏空状态
            emptyStateLayout.setVisibility(View.GONE);
        } else {
            // 显示空状态
            emptyStateLayout.setVisibility(View.VISIBLE);
            // 空状态文本已移除
        }
    }
    
    private void setupClickListeners() {
        // 添加行按钮
        findViewById(R.id.add_row_button).setOnClickListener(v -> {
            viewModel.addNewRow();
        });
        
        // 添加列按钮
        findViewById(R.id.add_column_button).setOnClickListener(v -> {
            viewModel.addNewColumn();
        });
        
        // 撤销按钮
        findViewById(R.id.undo_button).setOnClickListener(v -> {
            viewModel.undo();
        });
        

    }
    
    private void handleIntent() {
        this.notebookId = getIntent().getLongExtra(EXTRA_NOTEBOOK_ID, -1);
        long templateId = getIntent().getLongExtra(EXTRA_TEMPLATE_ID, -1);
        String notebookName = getIntent().getStringExtra(EXTRA_NOTEBOOK_NAME);
        int rows = getIntent().getIntExtra(EXTRA_ROWS, 0);
        int cols = getIntent().getIntExtra(EXTRA_COLS, 0);
        
        if (this.notebookId != -1) {
            // 加载现有笔记
            viewModel.loadNotebook(this.notebookId);
        } else if (templateId != -1 && notebookName != null) {
            // 从模板创建笔记
            viewModel.createNotebookWithTemplate(notebookName, templateId);
        } else if (notebookName != null) {
            // 创建空白笔记
            viewModel.createBlankNotebook(notebookName, rows, cols);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_save) {
            // 手动保存
            viewModel.saveNotebook();
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            // TODO: 打开设置页面
            Toast.makeText(this, "打开设置", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        if (viewModel.hasUnsavedChanges()) {
            // 显示保存确认对话框
            // TODO: 实现保存确认对话框
        }
        super.onBackPressed();
    }
    
    /**
     * 显示单元格编辑对话框
     */
    private void showCellEditDialog(int rowIndex, int columnIndex) {
        // 获取当前单元格内容
        String cellKey = rowIndex + "_" + columnIndex;
        Cell cell = cellsMap.get(cellKey);
        String currentContent = (cell != null && cell.getContent() != null) ? cell.getContent() : "";
        
        // 创建编辑框
        EditText editText = new EditText(this);
        editText.setText(currentContent);
        editText.setSelection(currentContent.length()); // 光标移到末尾
        editText.setSingleLine(true);
        
        // 创建对话框
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("编辑单元格 (" + (rowIndex + 1) + ", " + (columnIndex + 1) + ")")
                .setView(editText)
                .setPositiveButton("确定", (d, which) -> {
                    String newContent = editText.getText().toString();
                    updateCellContent(rowIndex, columnIndex, newContent);
                })
                .setNegativeButton("取消", null)
                .create();
        
        dialog.show();
        
        // 自动弹出键盘并聚焦到编辑框
        editText.requestFocus();
    }
    
    /**
     * 更新单元格内容
     */
    private void updateCellContent(int rowIndex, int columnIndex, String newContent) {
        // 更新数据库
        viewModel.updateCellValue(rowIndex, columnIndex, newContent);
        
        // 更新本地缓存
        String cellKey = rowIndex + "_" + columnIndex;
        Cell cell = cellsMap.get(cellKey);
        if (cell != null) {
            cell.setContent(newContent);
        } else {
            cell = new Cell();
            cell.setRowIndex(rowIndex);
            cell.setColIndex(columnIndex);
            cell.setContent(newContent);
            cellsMap.put(cellKey, cell);
        }
        
        // 注释掉刷新表格显示，避免在编辑过程中干扰用户输入
        // updateTableData();
     }
     
     /**
      * 显示列头编辑对话框
      */
     // 列头编辑弹窗相关方法已删除
     
     /**
      * 显示删除行确认对话框
      */
     private void showDeleteRowDialog(int rowIndex) {
         new AlertDialog.Builder(this)
                 .setTitle("删除行")
                 .setMessage("确定要删除第 " + (rowIndex + 1) + " 行吗？此操作不可撤销。")
                 .setPositiveButton("删除", (dialog, which) -> {
                     if (viewModel != null) {
                         viewModel.deleteRowAt(rowIndex);
                         Toast.makeText(this, "已删除第 " + (rowIndex + 1) + " 行", Toast.LENGTH_SHORT).show();
                     }
                 })
                 .setNegativeButton("取消", null)
                 .show();
     }
     
     /**
      * 显示删除列确认对话框
      */
     private void showDeleteColumnDialog(int columnIndex) {
        final String columnName;
        if (columns != null && columnIndex < columns.size()) {
            columnName = columns.get(columnIndex).getName();
        } else {
            columnName = "";
        }
         
         new AlertDialog.Builder(this)
                 .setTitle("删除列")
                 .setMessage("确定要删除列 \"" + columnName + "\" 吗？此操作不可撤销。")
                 .setPositiveButton("删除", (dialog, which) -> {
                     if (viewModel != null) {
                         viewModel.deleteColumnAt(columnIndex);
                         Toast.makeText(this, "已删除列 \"" + columnName + "\"", Toast.LENGTH_SHORT).show();
                     }
                 })
                 .setNegativeButton("取消", null)
                 .show();
     }
}