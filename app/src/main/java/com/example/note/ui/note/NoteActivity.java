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
    
    private void initViews() {
        mainToolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.app_bar_layout);
        loadingProgress = findViewById(R.id.loading_progress);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        
        columnHeadersRecycler = findViewById(R.id.column_headers_recycler);
        rowHeadersRecycler = findViewById(R.id.row_headers_recycler);
        dataRowsRecycler = findViewById(R.id.data_grid_recycler);
        
        setSupportActionBar(mainToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);
    }
    
    private void initAdapters() {
        // 列头适配器
        columnHeaderAdapter = new ColumnHeaderAdapter(null);
        columnHeaderAdapter.setContext(this);
        columnHeaderAdapter.setNoteViewModel(viewModel);
        if (notebookId > 0) {
            columnHeaderAdapter.setCurrentNotebookId(notebookId);
        }
        // 设置列宽调整监听器
        columnHeaderAdapter.setOnColumnResizeListener(new ColumnHeaderAdapter.OnColumnResizeListener() {
            @Override
            public void onColumnResize(int columnIndex, float newWidth) {
                updateColumnWidth(columnIndex, newWidth);
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
        
        // 使用自定义LayoutManager禁用水平滚动
        LinearLayoutManager columnLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false) {
            @Override
            public boolean canScrollHorizontally() {
                return false; // 禁用水平滚动，防止列头拖拽导致错位
            }
        };
        columnHeadersRecycler.setLayoutManager(columnLayoutManager);
        columnHeadersRecycler.setAdapter(columnHeaderAdapter);
        
        // 行头适配器
        rowHeaderAdapter = new RowHeaderAdapter();
        rowHeaderAdapter.setOnRowClickListener(new RowHeaderAdapter.OnRowClickListener() {
            @Override
            public void onRowClick(int rowIndex) {
                // TODO: 处理行点击
            }
            
            @Override
            public void onRowLongClick(int rowIndex) {
                // TODO: 处理行长按
            }
            
            @Override
            public void onRowResize(int rowIndex, int newHeight) {
                // 更新行高 - 优化：转换为dp单位并延迟保存
                float density = getResources().getDisplayMetrics().density;
                int newHeightDp = Math.round(newHeight / density);
                updateRowHeight(rowIndex, newHeightDp);
                
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
         // 更新行高
         if (rowHeaderAdapter != null) {
             rowHeaderAdapter.updateRowHeight(rowIndex, newHeight);
         }
         if (tableRowAdapter != null) {
             tableRowAdapter.updateRowHeight(rowIndex, newHeight);
         }
     }
     
     private void updateColumnWidth(int columnIndex, float newWidth) {
         if (columns != null && columnIndex < columns.size()) {
             Column column = columns.get(columnIndex);
             column.setWidth(newWidth);
             
             // 更新数据库中的列宽
             viewModel.updateColumn(column);
             
             // 同步更新数据行的列宽
             if (tableRowAdapter != null) {
                 tableRowAdapter.updateColumnWidth(columnIndex, newWidth);
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
        
        // 同步滚动
        setupScrollSync();
    }
    
    private void setupScrollSync() {
        // 同步行头和数据区域的垂直滚动
        dataRowsRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                rowHeadersRecycler.scrollBy(0, dy);
            }
        });
        
        rowHeadersRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                dataRowsRecycler.scrollBy(0, dy);
            }
        });
    }
    
    private void setupObservers() {
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
        
        viewModel.getCanRedo().observe(this, canRedo -> {
            // 更新重做按钮状态
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
        
        // 重做按钮
        findViewById(R.id.redo_button).setOnClickListener(v -> {
            viewModel.redo();
        });
    }
    
    private void handleIntent() {
        this.notebookId = getIntent().getLongExtra(EXTRA_NOTEBOOK_ID, -1);
        long templateId = getIntent().getLongExtra(EXTRA_TEMPLATE_ID, -1);
        String notebookName = getIntent().getStringExtra(EXTRA_NOTEBOOK_NAME);
        int rows = getIntent().getIntExtra(EXTRA_ROWS, 10);
        int cols = getIntent().getIntExtra(EXTRA_COLS, 5);
        
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
            viewModel.saveNotebook();
            // 显示保存成功提示
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
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
}