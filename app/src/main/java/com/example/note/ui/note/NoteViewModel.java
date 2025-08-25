package com.example.note.ui.note;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.note.data.entity.Notebook;
import com.example.note.data.entity.Template;
import com.example.note.data.entity.Column;
import com.example.note.data.entity.Cell;
import com.example.note.data.entity.CellType;
import com.example.note.data.model.FilterOption;
import com.example.note.data.repository.NotebookRepository;
import com.example.note.data.repository.TemplateRepository;
import com.example.note.data.repository.ColumnRepository;
import com.example.note.data.repository.CellRepository;
import com.example.note.util.ColorUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Stack;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 笔记编辑ViewModel
 * 处理笔记创建、编辑和保存的业务逻辑
 */
public class NoteViewModel extends AndroidViewModel {
    
    private static final String TAG = "NoteViewModel";
    
    private final NotebookRepository notebookRepository;
    private final TemplateRepository templateRepository;
    private final ColumnRepository columnRepository;
    private final CellRepository cellRepository;
    
    // LiveData
    private final MutableLiveData<Notebook> _currentNotebook = new MutableLiveData<>();
    public final LiveData<Notebook> currentNotebook = _currentNotebook;
    
    private final MutableLiveData<Template> _currentTemplate = new MutableLiveData<>();
    public final LiveData<Template> currentTemplate = _currentTemplate;
    
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;
    
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;
    
    private final MutableLiveData<Boolean> _isSaved = new MutableLiveData<>(true);
    public final LiveData<Boolean> isSaved = _isSaved;
    
    // DataGrip风格表格相关LiveData
    private final MutableLiveData<List<Column>> _columns = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Column>> columns = _columns;
    
    private final MutableLiveData<List<Cell>> _frozenColumnCells = new MutableLiveData<>(new ArrayList<>());
    
    // 源数据缓存（排序时不改动，只在加载/增删结构时更新）
    private List<Cell> sourceFrozenCells = new ArrayList<>();
    private List<Cell> sourceScrollableCells = new ArrayList<>();
    
    // 撤销重做相关
    private final Stack<TableOperation> undoStack = new Stack<>();
    private final MutableLiveData<Boolean> _canUndo = new MutableLiveData<>(false);
    public final LiveData<Boolean> canUndo = _canUndo;
    
    // 延迟保存相关
    private final Handler delayedSaveHandler = new Handler(Looper.getMainLooper());
    private Runnable delayedSaveRunnable;
    

    
    public LiveData<Boolean> getCanUndo() {
        return _canUndo;
    }
    

    public final LiveData<List<Cell>> frozenColumnCells = _frozenColumnCells;
    
    private final MutableLiveData<List<Cell>> _scrollableColumnsCells = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Cell>> scrollableColumnsCells = _scrollableColumnsCells;
    
    private final MutableLiveData<Integer> _rowCount = new MutableLiveData<>(0);
    public final LiveData<Integer> rowCount = _rowCount;
    
    // 视口状态管理
    private final MutableLiveData<Float> _scale = new MutableLiveData<>(1.0f);
    public final LiveData<Float> scale = _scale;
    
    private final MutableLiveData<Float> _offsetX = new MutableLiveData<>(0f);
    public final LiveData<Float> offsetX = _offsetX;
    
    private final MutableLiveData<Float> _offsetY = new MutableLiveData<>(0f);
    public final LiveData<Float> offsetY = _offsetY;
    
    private final MutableLiveData<Integer> _columnCount = new MutableLiveData<>(0);
    public final LiveData<Integer> columnCount = _columnCount;
    
    // 行顺序追踪字段
    private int[] originalRowOrder = null; // 初次加载时建立，不再修改
    private int[] currentRowOrder = null; // 最近一次排序的行序
    
    // 当前可见的"原始行索引"；null 代表未筛选（全部可见）
    private List<Integer> activeVisibleRows = null;
    
    /**
     * 判断指定列索引是否为冻结列
     * @param colIndex 列索引
     * @return 如果是冻结列返回true，否则返回false
     */
    private boolean isFrozenColumnIndex(int colIndex) {
        List<Column> currentColumns = _columns.getValue();
        if (currentColumns != null && colIndex >= 0 && colIndex < currentColumns.size()) {
            return currentColumns.get(colIndex).isFrozen();
        }
        // 如果列信息不可用，默认不冻结
        return false;
    }
    
    /**
     * 获取第一个冻结列的索引
     * @return 第一个冻结列的索引，如果没有冻结列则返回-1
     */
    private int getFirstFrozenColumnIndex() {
        List<Column> currentColumns = _columns.getValue();
        if (currentColumns != null) {
            for (int i = 0; i < currentColumns.size(); i++) {
                if (currentColumns.get(i).isFrozen()) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    public NoteViewModel(@NonNull Application application) {
        super(application);
        notebookRepository = NotebookRepository.getInstance(application);
        templateRepository = TemplateRepository.getInstance(application);
        columnRepository = ColumnRepository.getInstance(application);
        cellRepository = CellRepository.getInstance(application);
    }
    
    // Getter methods for LiveData
    public LiveData<Notebook> getCurrentNotebook() {
        return currentNotebook;
    }
    
    public LiveData<Template> getCurrentTemplate() {
        return currentTemplate;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsSaved() {
        return isSaved;
    }
    
    // 视口状态管理方法已在其他位置定义
    
    /**
     * 创建空白笔记
     */
    public void createBlankNotebook(String name, int rows, int cols) {
        _isLoading.postValue(true);
        
        notebookRepository.createNotebook(name, ColorUtils.getDefaultNotebookColor(), new NotebookRepository.RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long notebookId) {
                _isLoading.postValue(false);
                
                // 创建笔记本成功，设置当前笔记本ID
                Notebook notebook = new Notebook();
                notebook.setId(notebookId);
                notebook.setTitle(name);
                notebook.setColor(ColorUtils.getDefaultNotebookColor());
                _currentNotebook.postValue(notebook);
                
                // 初始化空表格数据
                initializeEmptyTableData();
                
                Log.d(TAG, "Blank notebook created with ID: " + notebookId + ", rows: " + rows + ", cols: " + cols);
            }
            
            @Override
            public void onError(Exception error) {
                _isLoading.postValue(false);
                _errorMessage.postValue("创建笔记失败: " + error.getMessage());
                Log.e(TAG, "Failed to create blank notebook", error);
            }
        });
    }
    
    /**
     * 初始化空表格数据
     */
    private void initializeEmptyTableData() {
        // 初始化空的列列表
        _columns.postValue(new ArrayList<>());
        
        // 初始化空的单元格列表
        _frozenColumnCells.postValue(new ArrayList<>());
        _scrollableColumnsCells.postValue(new ArrayList<>());
        
        // 设置行数为0
        _rowCount.postValue(0);
        
        // 清空源数据缓存
        sourceFrozenCells.clear();
        sourceScrollableCells.clear();
        
        Log.d(TAG, "Empty table data initialized");
    }
    
    /**
     * 使用模板创建笔记
     */
    public void createNotebookWithTemplate(String name, long templateId) {
        _isLoading.postValue(true);
        
        // 首先获取模板信息
        LiveData<Template> templateLiveData = templateRepository.getTemplateById(templateId);
        Observer<Template> templateObserver = new Observer<Template>() {
            @Override
            public void onChanged(Template template) {
                templateLiveData.removeObserver(this);
                
                if (template == null) {
                    _isLoading.postValue(false);
                    _errorMessage.postValue("模板不存在");
                    return;
                }
                
                _currentTemplate.postValue(template);
                
                // 创建笔记本
                notebookRepository.createNotebook(name, ColorUtils.getDefaultNotebookColor(), new NotebookRepository.RepositoryCallback<Long>() {
                    @Override
                    public void onSuccess(Long notebookId) {
                        _isLoading.postValue(false);
                        
                        // 创建笔记本成功，应用模板
                        // TODO: 实现应用模板的逻辑，根据模板的data字段创建表格结构
                        Log.d(TAG, "Notebook created with template. NotebookId: " + notebookId + ", TemplateId: " + templateId);
                        
                        // 加载创建的笔记本
                        loadNotebook(notebookId);
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        _isLoading.postValue(false);
                        _errorMessage.postValue("创建笔记失败: " + error.getMessage());
                        Log.e(TAG, "Failed to create notebook with template", error);
                    }
                });
            }
        };
        templateLiveData.observeForever(templateObserver);
    }
    
    /**
     * 加载现有笔记
     */
    public void loadNotebook(long notebookId) {
        _isLoading.postValue(true);
        
        // 观察笔记本数据
        LiveData<Notebook> notebookLiveData = notebookRepository.getNotebookById(notebookId);
        Observer<Notebook> notebookObserver = new Observer<Notebook>() {
             @Override
             public void onChanged(Notebook notebook) {
                 notebookLiveData.removeObserver(this);
                 
                 if (notebook != null) {
                     _currentNotebook.postValue(notebook);
                     Log.d(TAG, "Notebook loaded: " + notebook.getTitle());
                     
                     // 加载笔记的表格数据
                     loadTableData(notebookId);
                 } else {
                     _errorMessage.postValue("笔记不存在");
                     Log.e(TAG, "Notebook not found: " + notebookId);
                     _isLoading.postValue(false);
                 }
             }
         };
        notebookLiveData.observeForever(notebookObserver);
    }
    
    /**
     * 加载表格数据（列和单元格）
     */
    private void loadTableData(long notebookId) {
        // 加载列数据
        LiveData<List<Column>> columnsLiveData = columnRepository.getColumnsByNotebookId(notebookId);
        Observer<List<Column>> columnsObserver = new Observer<List<Column>>() {
            @Override
            public void onChanged(List<Column> columns) {
                columnsLiveData.removeObserver(this);
                
                if (columns != null && !columns.isEmpty()) {
                    _columns.postValue(columns);
                    Log.d(TAG, "Loaded " + columns.size() + " columns for notebook " + notebookId);
                    
                    // 加载单元格数据（单元格数据加载时会设置正确的行数和列数）
                    loadCellData(notebookId);
                } else {
                    Log.d(TAG, "No existing columns found, creating default table structure");
                    // 如果没有现有列数据，创建默认的表格结构
                    initializeTableData(5, 5);
                    _isLoading.postValue(false);
                }
            }
        };
        columnsLiveData.observeForever(columnsObserver);
    }
    
    /**
     * 加载单元格数据
     */
    private void loadCellData(long notebookId) {
        // 加载所有单元格数据
        LiveData<List<Cell>> cellsLiveData = cellRepository.getCellsByNotebookId(notebookId);
        Observer<List<Cell>> cellsObserver = new Observer<List<Cell>>() {
            @Override
            public void onChanged(List<Cell> cells) {
                cellsLiveData.removeObserver(this);
                
                if (cells != null) {
                    Log.d(TAG, "Loaded " + cells.size() + " cells for notebook " + notebookId);
                    
                    // 分离冻结列和可滚动列的单元格
                    List<Cell> frozenCells = new ArrayList<>();
                    List<Cell> scrollableCells = new ArrayList<>();
                    
                    // 计算实际的行数和列数
                int maxRow = -1;
                int maxCol = -1;
                
                for (Cell cell : cells) {
                    maxRow = Math.max(maxRow, cell.getRowIndex());
                    maxCol = Math.max(maxCol, cell.getColIndex());
                    
                    if (isFrozenColumnIndex(cell.getColIndex())) {
                        frozenCells.add(cell);
                    } else {
                        scrollableCells.add(cell);
                    }
                }
                
                // 初始化源数据缓存（深拷贝）
                sourceFrozenCells = deepCopyCells(frozenCells);
                sourceScrollableCells = deepCopyCells(scrollableCells);
                
                // 设置实际的行数和列数（索引+1）
                int rows, cols;
                if (maxRow >= 0 && maxCol >= 0) {
                    rows = maxRow + 1;
                    // 列数应该与实际的列数据保持一致，而不是单元格的最大列索引
                    List<Column> currentColumns = _columns.getValue();
                    if (currentColumns != null && !currentColumns.isEmpty()) {
                        cols = currentColumns.size();
                    } else {
                        cols = maxCol + 1;
                    }
                } else {
                    // 即使没有单元格数据，也要根据列数据设置正确的行数和列数
                    List<Column> currentColumns = _columns.getValue();
                    if (currentColumns != null && !currentColumns.isEmpty()) {
                        cols = currentColumns.size();
                        rows = 1; // 如果有列但没有单元格，至少应该有1行（标题行）
                    } else {
                        rows = 0;
                        cols = 0;
                    }
                }
                
                _rowCount.postValue(rows);
                _columnCount.postValue(cols);
                
                // 初始化行顺序追踪数组
                if (rows > 0) {
                    originalRowOrder = new int[rows];
                    currentRowOrder = new int[rows];
                    for (int i = 0; i < rows; i++) {
                        originalRowOrder[i] = i;
                        currentRowOrder[i] = i;
                    }
                } else {
                    originalRowOrder = new int[0];
                    currentRowOrder = new int[0];
                }
                
                // 使用网格重建逻辑，确保与排序路径的数据形态一致
                if (rows > 0 && cols > 0) {
                    // 构建完整网格（使用源数据）
                    List<List<Cell>> grid = buildFullGrid(sourceFrozenCells, sourceScrollableCells, rows, cols);
                    // 按原序输出（0..rows-1）
                    int[] order = new int[rows];
                    for (int i = 0; i < rows; i++) {
                        order[i] = i;
                    }
                    emitGridInOrder(grid, order);
                } else {
                    _frozenColumnCells.postValue(new ArrayList<>());
                    _scrollableColumnsCells.postValue(new ArrayList<>());
                }
                } else {
                    Log.d(TAG, "No existing cells found");
                    // 空数据情况已在上面统一处理
                    _rowCount.postValue(0);
                    _columnCount.postValue(0);
                    _frozenColumnCells.postValue(new ArrayList<>());
                    _scrollableColumnsCells.postValue(new ArrayList<>());
                }
                
                _isLoading.postValue(false);
            }
        };
        cellsLiveData.observeForever(cellsObserver);
    }
    
    /**
     * 保存笔记
     */
    public void saveNotebook() {
        Notebook notebook = _currentNotebook.getValue();
        if (notebook == null) {
            _errorMessage.postValue("没有可保存的笔记");
            return;
        }
        
        _isLoading.postValue(true);
        
        // 保存列数据
        List<Column> columns = _columns.getValue();
        if (columns != null && !columns.isEmpty()) {
            for (Column column : columns) {
                column.setNotebookId(notebook.getId());
            }
            
            columnRepository.saveColumns(columns, new ColumnRepository.RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Columns saved successfully");
                    // 保存单元格数据
                    saveCellsData(notebook.getId());
                }
                
                @Override
                public void onError(Exception e) {
                    _isLoading.postValue(false);
                    _errorMessage.postValue("保存列数据失败: " + e.getMessage());
                    Log.e(TAG, "Failed to save columns", e);
                }
            });
        } else {
            // 如果没有列数据，直接保存单元格数据
            saveCellsData(notebook.getId());
        }
    }
    
    /**
     * 保存单元格数据
     */
    private void saveCellsData(long notebookId) {
        List<Cell> allCells = new ArrayList<>();
        
        // 从源数据缓存收集冻结列单元格（避免使用显示数据）
        if (sourceFrozenCells != null) {
            for (Cell cell : sourceFrozenCells) {
                // 创建Cell的副本，避免修改原始对象
                Cell cellCopy = createCellCopy(cell);
                cellCopy.setNotebookId(notebookId);
                cellCopy.setId(0); // 清零id，避免主键冲突，让Room走自增主键
                allCells.add(cellCopy);
            }
        }
        
        // 从源数据缓存收集可滚动列单元格（避免使用显示数据）
        if (sourceScrollableCells != null) {
            for (Cell cell : sourceScrollableCells) {
                // 创建Cell的副本，避免修改原始对象
                Cell cellCopy = createCellCopy(cell);
                cellCopy.setNotebookId(notebookId);
                cellCopy.setId(0); // 清零id，避免主键冲突，让Room走自增主键
                allCells.add(cellCopy);
            }
        }
        
        if (!allCells.isEmpty()) {
            cellRepository.replaceAllCells(notebookId, allCells, new CellRepository.RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // 更新笔记本的updatedAt时间戳
                    notebookRepository.touchNotebook(notebookId);
                    _isLoading.postValue(false);
                    _isSaved.postValue(true);
                    Log.d(TAG, "Notebook saved successfully: " + allCells.size() + " cells");
                }
                
                @Override
                public void onError(Exception e) {
                    _isLoading.postValue(false);
                    _errorMessage.postValue("保存单元格数据失败: " + e.getMessage());
                    Log.e(TAG, "Failed to save cells", e);
                }
            });
        } else {
            // 即使没有单元格数据也要更新笔记本的updatedAt时间戳
            notebookRepository.touchNotebook(notebookId);
            _isLoading.postValue(false);
            _isSaved.postValue(true);
            Log.d(TAG, "Notebook saved: no cells to save");
        }
    }
    
    /**
     * 保存为模板
     */
    public void saveAsTemplate(String templateName, String description) {
        Notebook notebook = _currentNotebook.getValue();
        if (notebook == null) {
            _errorMessage.postValue("没有可保存的笔记");
            return;
        }
        
        _isLoading.postValue(true);
        
        // TODO: 实现保存为模板的逻辑
        // 需要序列化当前表格结构为JSON格式
        
        _isLoading.postValue(false);
        Log.d(TAG, "Notebook saved as template: " + templateName);
    }
    
    /**
     * 添加列
     */
    public void addColumn() {
        // TODO: 实现添加列的逻辑
        _isSaved.postValue(false);
        Log.d(TAG, "Column added");
    }
    
    /**
     * 添加行
     */
    public void addRow() {
        // TODO: 实现添加行的逻辑
        _isSaved.postValue(false);
        Log.d(TAG, "Row added");
    }
    
    /**
     * 标记数据已修改
     */
    public void markAsModified() {
        _isSaved.postValue(false);
    }
    
    /**
     * 检查是否有未保存的更改
     */
    public boolean hasUnsavedChanges() {
        Boolean saved = _isSaved.getValue();
        return saved != null && !saved;
    }
    
    /**
     * 标记为已保存
     */
    public void markAsSaved() {
        _isSaved.postValue(true);
    }
    
    /**
     * 触发延迟保存
     */
    private void triggerDelayedSave() {
        // 取消之前的延迟保存任务
        delayedSaveHandler.removeCallbacks(delayedSaveRunnable);
        
        // 创建新的延迟保存任务
        delayedSaveRunnable = () -> {
            saveNotebook();
            Log.d(TAG, "Delayed save triggered after adding row/column");
        };
        
        // 延迟500ms执行保存
        delayedSaveHandler.postDelayed(delayedSaveRunnable, 500);
    }
    
    // DataGrip风格表格相关方法
    
    /**
     * 初始化表格数据
     */
    public void initializeTableData(int rows, int cols) {
        // 创建列数据
        List<Column> columnList = new ArrayList<>();
        for (int i = 0; i < cols; i++) {
            Column column = new Column();
            column.setColumnIndex(i);
            column.setName("列" + (i + 1));
            column.setWidth(150); // 使用新的默认宽度
            column.setType("TEXT");
            column.setVisible(true);
            column.setFrozen(false); // 默认不设置冻结列
            columnList.add(column);
        }
        _columns.postValue(columnList);
        
        // 创建冻结列单元格数据
        List<Cell> frozenCells = new ArrayList<>();
        int firstFrozenColIndex = getFirstFrozenColumnIndex();
        if (firstFrozenColIndex >= 0) {
            for (int row = 0; row < rows; row++) {
                Cell cell = new Cell();
                cell.setRowIndex(row);
                cell.setColIndex(firstFrozenColIndex);
                cell.setContent("");
                frozenCells.add(cell);
            }
        }
        _frozenColumnCells.postValue(frozenCells);
        
        // 创建可滚动列单元格数据
        List<Cell> scrollableCells = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 1; col < cols; col++) {
                Cell cell = new Cell();
                cell.setRowIndex(row);
                cell.setColIndex(col);
                cell.setContent("");
                scrollableCells.add(cell);
            }
        }
        _scrollableColumnsCells.postValue(scrollableCells);
        
        // ✅ 同步初始化源数据缓存（深拷贝）
        sourceFrozenCells = deepCopyCells(frozenCells);
        sourceScrollableCells = deepCopyCells(scrollableCells);
        
        _rowCount.postValue(rows);
        _columnCount.postValue(cols);
    }
    
    /**
     * 获取列数据
     */
    public LiveData<List<Column>> getColumns() {
        return columns;
    }
    
    /**
     * 获取冻结列单元格数据
     */
    public LiveData<List<Cell>> getFrozenColumnCells() {
        return frozenColumnCells;
    }
    
    /**
     * 获取可滚动列单元格数据
     */
    public LiveData<List<Cell>> getScrollableColumnsCells() {
        return scrollableColumnsCells;
    }
    
    /**
     * 获取所有单元格数据（冻结列+可滚动列）
     */
    public LiveData<List<Cell>> getCells() {
        return new MediatorLiveData<List<Cell>>() {
            {
                addSource(frozenColumnCells, cells -> {
                    List<Cell> allCells = new ArrayList<>();
                    if (cells != null) {
                        allCells.addAll(cells);
                    }
                    List<Cell> scrollableCells = scrollableColumnsCells.getValue();
                    if (scrollableCells != null) {
                        allCells.addAll(scrollableCells);
                    }
                    setValue(allCells);
                });
                addSource(scrollableColumnsCells, cells -> {
                    List<Cell> allCells = new ArrayList<>();
                    List<Cell> frozenCells = frozenColumnCells.getValue();
                    if (frozenCells != null) {
                        allCells.addAll(frozenCells);
                    }
                    if (cells != null) {
                        allCells.addAll(cells);
                    }
                    setValue(allCells);
                });
            }
        };
    }
    
    /**
     * 获取行数
     */
    public LiveData<Integer> getRowCount() {
        return rowCount;
    }
    
    /**
     * 获取列数
     */
    public LiveData<Integer> getColumnCount() {
        return columnCount;
    }
    
    /**
     * 设置列数据
     */
    public void setColumns(List<Column> columns) {
        _columns.postValue(columns != null ? columns : new ArrayList<>());
        markAsModified();
    }
    

    /**
     * 按列排序 - 简化版本，只更新排序状态然后调用统一渲染
     * @param columnIndex 列索引
     * @param sortOrder 排序方式：null=默认(按创建顺序), "ASC"=升序, "DESC"=降序
     */
    public void sortByColumn(int columnIndex, String sortOrder) {
        Integer rows = _rowCount.getValue();
        Integer cols = _columnCount.getValue();
        if (rows == null || cols == null || rows < 0 || cols <= 0) return;

        // 只做"单列排序状态管理"
        List<Column> colsDef = _columns.getValue();
        if (colsDef != null) {
            for (Column c : colsDef) {
                c.setSortOrder(c.getColumnIndex() == columnIndex ? sortOrder : null);
            }
            _columns.postValue(colsDef);
        }

        // 统一渲染（会对 activeVisibleRows 这批"可见行"应用排序/恢复默认顺序）
        refreshViewRespectingFilterAndSort();
    }
    
    /**
     * 在行中查找指定列的单元格
     */
    private Cell findCellInRow(List<Cell> row, int columnIndex) {
        for (Cell cell : row) {
            if (cell.getColIndex() == columnIndex) {
                return cell;
            }
        }
        return null;
    }
    
    /**
     * 获取单元格内容，处理空值情况
     */
    private String getCellContent(Cell cell) {
        return (cell != null && cell.getContent() != null) ? cell.getContent().trim() : "";
    }
    
    /**
     * 创建Cell对象的副本，确保数据完整性
     */
    private Cell createCellCopy(Cell original) {
        Cell copy = new Cell();
        
        // 复制所有属性
        copy.setId(original.getId());
        copy.setNotebookId(original.getNotebookId());
        copy.setRowIndex(original.getRowIndex());
        copy.setColIndex(original.getColIndex());
        copy.setContent(original.getContent());
        copy.setTextColor(original.getTextColor());
        copy.setBackgroundColor(original.getBackgroundColor());
        copy.setBold(original.isBold());
        copy.setItalic(original.isItalic());
        copy.setTextSize(original.getTextSize());
        copy.setTextAlignment(original.getTextAlignment());
        copy.setImageId(original.getImageId());
        copy.setCreatedAt(original.getCreatedAt());
        copy.setUpdatedAt(original.getUpdatedAt());
        
        return copy;
    }
    
    /**
     * Excel风格的比较方法
     * 数字按数值大小排序，字符串按字典序排序
     */
    private int compareExcelStyle(String content1, String content2) {
        // 空值处理：空值排在最后
        if (content1.isEmpty() && content2.isEmpty()) return 0;
        if (content1.isEmpty()) return 1;
        if (content2.isEmpty()) return -1;
        
        // 尝试解析为数字
        Double num1 = tryParseNumber(content1);
        Double num2 = tryParseNumber(content2);
        
        // 如果都是数字，按数值比较
        if (num1 != null && num2 != null) {
            return Double.compare(num1, num2);
        }
        
        // 如果一个是数字一个不是，数字排在前面
        if (num1 != null && num2 == null) {
            return -1;
        }
        if (num1 == null && num2 != null) {
            return 1;
        }
        
        // 都不是数字，按字符串比较（忽略大小写）
        return content1.compareToIgnoreCase(content2);
    }
    
    /**
     * 比较非空内容（用于新的排序逻辑）
     */
    private int compareExcelStyleNonEmpty(String content1, String content2) {
        // c1、c2 均保证非空
        Double num1 = tryParseNumber(content1);
        Double num2 = tryParseNumber(content2);
        
        // 如果都是数字，按数值比较
        if (num1 != null && num2 != null) {
            return Double.compare(num1, num2);
        }
        
        // 如果一个是数字一个不是，数字排在前面
        if (num1 != null && num2 == null) {
            return -1;
        }
        if (num1 == null && num2 != null) {
            return 1;
        }
        
        // 都不是数字，按字符串比较（忽略大小写）
        return content1.compareToIgnoreCase(content2);
    }
    
    /**
     * 尝试将字符串解析为数字
     */
    private Double tryParseNumber(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 移除千分位分隔符和货币符号
            String cleaned = str.replaceAll("[,￥$€£¥]", "").trim();
            
            // 处理百分号
            if (cleaned.endsWith("%")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
                return Double.parseDouble(cleaned) / 100.0;
            }
            
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 根据给定的行顺序数组重新排列数据
     * @param rowOrder 行顺序数组，包含要排列的行索引
     * @return 排序后的行索引数组
     */
    private int[] argsortByArray(int[] rowOrder) {
        // 创建索引数组
        Integer[] indices = new Integer[rowOrder.length];
        for (int i = 0; i < rowOrder.length; i++) {
            indices[i] = i;
        }
        
        // 根据rowOrder中的值进行排序
        Arrays.sort(indices, (i, j) -> Integer.compare(rowOrder[i], rowOrder[j]));
        
        // 转换为int数组
        int[] result = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            result[i] = indices[i];
        }
        
        return result;
    }
    
    /**
     * 创建单元格的展示副本，避免污染源数据的行索引
     * @param original 原始单元格
     * @param displayRowIndex 展示用的行索引
     * @return 展示副本
     */
    private Cell createDisplayCopy(Cell original, int displayRowIndex) {
        Cell copy = new Cell();
        // 复制展示所需的字段，但不复制id等数据库相关字段
        copy.setNotebookId(original.getNotebookId());
        copy.setRowIndex(displayRowIndex); // 使用展示用的行索引
        copy.setColIndex(original.getColIndex());
        copy.setContent(original.getContent());
        copy.setTextColor(original.getTextColor());
        copy.setBackgroundColor(original.getBackgroundColor());
        copy.setBold(original.isBold());
        copy.setItalic(original.isItalic());
        copy.setTextSize(original.getTextSize());
        copy.setTextAlignment(original.getTextAlignment());
        copy.setImageId(original.getImageId());
        
        // 不设置稳定ID，避免UI稳定ID污染数据库
        // RecyclerView的稳定ID由Adapter的getItemId()方法提供
        
        // 注意：不复制createdAt、updatedAt等数据库字段
        return copy;
    }
    
    /**
     * 创建单元格的展示副本，使用指定的原始行键生成稳定ID
     * @param original 原始单元格
     * @param displayRowIndex 展示用的行索引
     * @param rowKey 原始行键，用于生成稳定ID
     * @return 展示副本
     */
    private Cell createDisplayCopy(Cell original, int displayRowIndex, int rowKey) {
        Cell copy = new Cell();
        copy.setNotebookId(original.getNotebookId());
        copy.setRowIndex(displayRowIndex);
        copy.setColIndex(original.getColIndex());
        copy.setContent(original.getContent());
        copy.setTextColor(original.getTextColor());
        copy.setBackgroundColor(original.getBackgroundColor());
        copy.setBold(original.isBold());
        copy.setItalic(original.isItalic());
        copy.setTextSize(original.getTextSize());
        copy.setTextAlignment(original.getTextAlignment());
        copy.setImageId(original.getImageId());
        
        // 不设置稳定ID，避免UI稳定ID污染数据库
        // RecyclerView的稳定ID由Adapter的getItemId()方法提供
        
        return copy;
    }

    /**
     * 统一渲染管线：基于源缓存、可见行集合和当前排序状态输出到LiveData
     */
    private void refreshViewRespectingFilterAndSort() {
        // 1) 获取源数据总行数和总列数
        int rows = totalRowsFromSource();
        int cols = totalColsFromColumns();
        if (rows <= 0 || cols <= 0) {
            _frozenColumnCells.postValue(new ArrayList<>());
            _scrollableColumnsCells.postValue(new ArrayList<>());
            _rowCount.postValue(0);
            return;
        }

        // 2) 基于源缓存构建完整网格
        List<List<Cell>> grid = buildFullGrid(sourceFrozenCells, sourceScrollableCells, rows, cols);

        // 3) 确定候选行（筛选逻辑）
        List<Integer> candidates;
        if (activeVisibleRows == null) {
            // 未筛选：全部行
            candidates = new ArrayList<>();
            for (int i = 0; i < rows; i++) {
                candidates.add(i);
            }
        } else {
            // 已筛选：只取可见行
            candidates = new ArrayList<>(activeVisibleRows);
        }

        // 4) 应用排序（如果有）
        Column sortedCol = getSortedColumnDef();
        if (sortedCol != null) {
            final int sortCol = sortedCol.getColumnIndex();
            final boolean desc = "DESC".equals(sortedCol.getSortOrder());
            candidates.sort((r1, r2) -> {
                String s1 = getCellContent(grid.get(r1).get(sortCol));
                String s2 = getCellContent(grid.get(r2).get(sortCol));
                if (s1.isEmpty() && s2.isEmpty()) return 0;
                if (s1.isEmpty()) return 1;   // 空值末尾
                if (s2.isEmpty()) return -1;
                int c = compareExcelStyleNonEmpty(s1, s2);
                return desc ? -c : c;
            });
        }

        // 5) 构建"可见 grid"，并更新 currentRowOrder（显示行 -> 原始行）
        List<List<Cell>> viewGrid = new ArrayList<>(candidates.size());
        currentRowOrder = new int[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            int ori = candidates.get(i);
            viewGrid.add(grid.get(ori));
            currentRowOrder[i] = ori;
        }

        // 6) 发射（identity order，因为 viewGrid 已按显示顺序排列）
        int[] order = new int[viewGrid.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;

        emitGridInOrder(viewGrid, order);
        _rowCount.postValue(viewGrid.size());
        markAsModified();
    }

    @Nullable
    private Column getSortedColumnDef() {
        List<Column> cs = _columns.getValue();
        if (cs == null) return null;
        for (Column c : cs) {
            String so = c.getSortOrder();
            if (so != null && !so.isEmpty()) return c;
        }
        return null;
    }
    
    /**
     * 构建完整的行网格结构，为空位补齐占位Cell
     * @param frozen 冻结列单元格列表
     * @param scrollable 可滚动列单元格列表
     * @param rows 总行数
     * @param cols 总列数
     * @return 完整的行网格，每行包含所有列的Cell对象
     */
    private List<List<Cell>> buildFullGrid(List<Cell> frozen, List<Cell> scrollable, int rows, int cols) {
        // 初始化空网格
        List<List<Cell>> grid = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            List<Cell> row = new ArrayList<>(cols);
            for (int c = 0; c < cols; c++) {
                // 创建空Cell占位
                Cell empty = new Cell();
                empty.setRowIndex(r);
                empty.setColIndex(c);
                empty.setContent("");
                // 设置默认的notebookId
                Notebook notebook = _currentNotebook.getValue();
                if (notebook != null) {
                    empty.setNotebookId(notebook.getId());
                }
                row.add(empty);
            }
            grid.add(row);
        }
        
        // 覆盖已有单元格 - 处理冻结列
        if (frozen != null) {
            for (Cell cell : frozen) {
                int r = cell.getRowIndex();
                int c = cell.getColIndex();
                if (r >= 0 && r < rows && c >= 0 && c < cols) {
                    grid.get(r).set(c, cell); // 用已有对象占位
                }
            }
        }
        
        // 覆盖已有单元格 - 处理可滚动列
        if (scrollable != null) {
            for (Cell cell : scrollable) {
                int r = cell.getRowIndex();
                int c = cell.getColIndex();
                if (r >= 0 && r < rows && c >= 0 && c < cols) {
                    grid.get(r).set(c, cell);
                }
            }
        }
        
        return grid;
    }
    
    /**
     * 将网格按某种行序输出成新的LiveData列表
     * @param grid 完整的行网格
     * @param rowOrder 行序数组，指定每个新行位置对应的原行索引
     */
    private void emitGridInOrder(List<List<Cell>> grid, int[] rowOrder) {
        List<Cell> newFrozen = new ArrayList<>();
        List<Cell> newScrollable = new ArrayList<>();
        int rows = grid.size();
        int cols = rows > 0 ? grid.get(0).size() : 0;
        
        for (int newRow = 0; newRow < rows; newRow++) {
            int oldRow = rowOrder[newRow];
            if (oldRow >= 0 && oldRow < rows) {
                List<Cell> oldRowCells = grid.get(oldRow);
                
                // ✅ 用该行原始行号作为 rowKey，避免过滤后错位
                int rowKey;
                if (oldRowCells != null && !oldRowCells.isEmpty()) {
                    rowKey = oldRowCells.get(0).getRowIndex(); // 这就是原始行索引
                } else {
                    rowKey = oldRow; // 兜底
                }
                
                for (int c = 0; c < cols; c++) {
                    if (c < oldRowCells.size()) {
                        Cell copied = createDisplayCopy(oldRowCells.get(c), newRow, rowKey);
                        if (isFrozenColumnIndex(c)) {
                            newFrozen.add(copied);
                        } else {
                            newScrollable.add(copied);
                        }
                    }
                }
            }
        }
        
        // 稳定排序（防御性）
        newFrozen.sort(Comparator.comparingInt(Cell::getRowIndex));
        newScrollable.sort((a, b) -> {
            int cmp = Integer.compare(a.getRowIndex(), b.getRowIndex());
            if (cmp != 0) return cmp;
            return Integer.compare(a.getColIndex(), b.getColIndex());
        });
        
        _frozenColumnCells.postValue(newFrozen);
        _scrollableColumnsCells.postValue(newScrollable);
    }
    
    /**
     * 设置冻结列单元格数据
     */
    public void setFrozenColumnCells(List<Cell> cells) {
        _frozenColumnCells.postValue(cells != null ? cells : new ArrayList<>());
        markAsModified();
    }
    
    /**
     * 设置可滚动列单元格数据
     */
    public void setScrollableColumnsCells(List<Cell> cells) {
        _scrollableColumnsCells.postValue(cells != null ? cells : new ArrayList<>());
        markAsModified();
    }
    
    /**
     * 更新单元格值（编辑即保存）
     */
    public void updateCellValue(int row, int col, String value) {
        Notebook currentNotebook = _currentNotebook.getValue();
        if (currentNotebook == null) {
            Log.e(TAG, "Cannot update cell: no current notebook");
            return;
        }
        
        // 行号映射：将显示行号转换为原始行号
        final int originalRow;
        if (currentRowOrder != null && row < currentRowOrder.length) {
            originalRow = currentRowOrder[row];
            Log.d(TAG, "Row mapping: display row " + row + " -> original row " + originalRow);
        } else {
            originalRow = row;
        }
        
        // 获取旧值用于撤销重做
        String oldValue = "";
        Cell targetCell = null;
        
        // 先获取当前值作为旧值（使用显示行号查找显示数据）
        if (isFrozenColumnIndex(col)) {
            List<Cell> frozenCells = _frozenColumnCells.getValue();
            if (frozenCells != null) {
                for (Cell cell : frozenCells) {
                    if (cell.getRowIndex() == row && cell.getColIndex() == col) {
                        oldValue = cell.getContent() != null ? cell.getContent() : "";
                        targetCell = cell;
                        break;
                    }
                }
            }
        } else {
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            if (scrollableCells != null) {
                for (Cell cell : scrollableCells) {
                    if (cell.getRowIndex() == row && cell.getColIndex() == col) {
                        oldValue = cell.getContent() != null ? cell.getContent() : "";
                        targetCell = cell;
                        break;
                    }
                }
            }
        }
        
        // 如果值没有变化，不需要记录操作
        if (oldValue.equals(value)) {
            return;
        }
        
        // 记录撤销重做操作（使用 row*1000 + col 编码位置）
        TableOperation operation = new TableOperation(
            TableOperation.OperationType.UPDATE_CELL,
            row * 1000 + col,
            oldValue,
            value
        );
        addToUndoStack(operation);
        
        // 更新内存中的数据（不触发LiveData更新，避免干扰编辑）
        if (isFrozenColumnIndex(col)) {
            // 更新冻结列（使用显示行号查找显示数据）
            List<Cell> frozenCells = _frozenColumnCells.getValue();
            if (frozenCells != null) {
                for (Cell cell : frozenCells) {
                    if (cell.getRowIndex() == row && cell.getColIndex() == col) {
                        cell.setContent(value);
                        targetCell = cell;
                        break;
                    }
                }
                // 注释掉postValue，避免在编辑过程中触发UI重新绑定
                // _frozenColumnCells.postValue(frozenCells);
            }
            
            // 同步更新源数据缓存（使用原始行号）
            if (sourceFrozenCells != null) {
                for (Cell cell : sourceFrozenCells) {
                    if (cell.getRowIndex() == originalRow && cell.getColIndex() == col) {
                        cell.setContent(value);
                        break;
                    }
                }
            }
        } else {
            // 更新可滚动列（使用显示行号查找显示数据）
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            if (scrollableCells != null) {
                for (Cell cell : scrollableCells) {
                    if (cell.getRowIndex() == row && cell.getColIndex() == col) {
                        cell.setContent(value);
                        targetCell = cell;
                        break;
                    }
                }
                // 注释掉postValue，避免在编辑过程中触发UI重新绑定
                // _scrollableColumnsCells.postValue(scrollableCells);
            }
            
            // 同步更新源数据缓存（使用原始行号）
            if (sourceScrollableCells != null) {
                for (Cell cell : sourceScrollableCells) {
                    if (cell.getRowIndex() == originalRow && cell.getColIndex() == col) {
                        cell.setContent(value);
                        break;
                    }
                }
            }
        }
        
        // 立即保存到数据库（编辑即保存，使用原始行号）
        // 无论是否找到targetCell，都进行数据库更新（关键修复点）
        if (targetCell != null) {
            targetCell.setNotebookId(currentNotebook.getId());
        }
        // 数据库中存储的是原始行号，所以必须使用originalRow
        cellRepository.updateCellContent(currentNotebook.getId(), originalRow, col, value, new CellRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // 更新笔记本的updatedAt时间戳
                notebookRepository.touchNotebook(currentNotebook.getId());
                Log.d(TAG, "Cell saved: (" + originalRow + ", " + col + ") = " + value);
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to save cell: (" + originalRow + ", " + col + ")", e);
                _errorMessage.postValue("保存失败: " + e.getMessage());
            }
        });
        
        markAsModified();
    }
    
    /**
     * 添加新列
     */
    public void addNewColumn() {
        List<Column> currentColumns = _columns.getValue();
        if (currentColumns != null) {
            int newColIndex = currentColumns.size();
            Column newColumn = new Column();
            newColumn.setColumnIndex(newColIndex);
            newColumn.setName("列" + (newColIndex + 1));
            newColumn.setWidth(150); // 使用新的默认宽度
            newColumn.setType("TEXT");
            newColumn.setVisible(true);
            newColumn.setFrozen(false);
            
            currentColumns.add(newColumn);
            _columns.postValue(currentColumns);
            
            // 为新列添加单元格（只有非冻结列才添加到scrollableCells）
            if (newColIndex > 0) { // 只有非冻结列才添加到scrollableCells
                List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
                if (scrollableCells != null) {
                    Integer rows = _rowCount.getValue();
                    if (rows != null) {
                        for (int row = 0; row < rows; row++) {
                            Cell newCell = new Cell();
                            newCell.setRowIndex(row);
                            newCell.setColIndex(newColIndex);
                            newCell.setContent("");
                            scrollableCells.add(newCell);
                            
                            // 同步更新源数据缓存
                            Cell sourceCell = new Cell();
                            sourceCell.setRowIndex(row);
                            sourceCell.setColIndex(newColIndex);
                            sourceCell.setContent("");
                            sourceScrollableCells.add(sourceCell);
                        }
                        _scrollableColumnsCells.postValue(scrollableCells);
                    }
                }
            }
            
            _columnCount.postValue(newColIndex + 1);
            markAsModified();
            
            // 立即触发保存（延迟500ms避免频繁保存）
            triggerDelayedSave();
        }
    }
    
    /**
     * 添加新行
     */
    public void addNewRow() {
        Integer currentRows = _rowCount.getValue();
        Integer currentCols = _columnCount.getValue();
        
        if (currentRows != null && currentCols != null) {
            int newRowIndex = currentRows;
            
            // 记录撤销重做操作
            TableOperation operation = new TableOperation(
                TableOperation.OperationType.ADD_ROW,
                newRowIndex,
                null,
                null
            );
            addToUndoStack(operation);
            
            // 为冻结列添加新行（只有真正存在冻结列时才操作）
            int firstFrozenColIndex = getFirstFrozenColumnIndex();
            if (firstFrozenColIndex >= 0) {
                List<Cell> frozenCells = _frozenColumnCells.getValue();
                if (frozenCells != null) {
                    Cell newFrozenCell = new Cell();
                    newFrozenCell.setRowIndex(newRowIndex);
                    newFrozenCell.setColIndex(firstFrozenColIndex);
                    newFrozenCell.setContent("");
                    frozenCells.add(newFrozenCell);
                    _frozenColumnCells.postValue(frozenCells);
                    
                    // 同步维护源数据缓存
                    Cell sourceFrozenCell = createCellCopy(newFrozenCell);
                    sourceFrozenCells.add(sourceFrozenCell);
                }
            }
            
            // 为可滚动列添加新行（遍历所有列并跳过冻结列）
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            if (scrollableCells != null) {
                for (int col = 0; col < currentCols; col++) {
                    if (isFrozenColumnIndex(col)) continue; // 跳过冻结列
                    Cell newCell = new Cell();
                    newCell.setRowIndex(newRowIndex);
                    newCell.setColIndex(col);
                    newCell.setContent("");
                    scrollableCells.add(newCell);
                    
                    // 同步维护源数据缓存
                    Cell sourceCell = createCellCopy(newCell);
                    sourceScrollableCells.add(sourceCell);
                }
                _scrollableColumnsCells.postValue(scrollableCells);
            }
            
            _rowCount.postValue(newRowIndex + 1);
            
            // 维护行顺序数组
            insertKey(newRowIndex);
            
            markAsModified();
            
            // 立即触发保存（延迟500ms避免频繁保存）
            triggerDelayedSave();
        }
    }
    
    /**
     * 更新列信息
     */
    public void updateColumn(Column column) {
        List<Column> currentColumns = _columns.getValue();
        if (currentColumns != null) {
            for (int i = 0; i < currentColumns.size(); i++) {
                // 修复：使用columnIndex而不是id来匹配列，避免新建笔记时所有列id都为0的问题
                if (currentColumns.get(i).getColumnIndex() == column.getColumnIndex()) {
                    currentColumns.set(i, column);
                    break;
                }
            }
            _columns.postValue(currentColumns);
            markAsModified();
            
            // 检查列是否已经保存到数据库（ID为0表示未保存）
            if (column.getId() == 0) {
                // 新建笔记时列还没有ID，需要先保存整个列数据以获得ID
                Notebook notebook = _currentNotebook.getValue();
                if (notebook != null) {
                    column.setNotebookId(notebook.getId());
                    columnRepository.saveColumn(column, new ColumnRepository.RepositoryCallback<Long>() {
                        @Override
                        public void onSuccess(Long id) {
                            column.setId(id);
                            Log.d(TAG, "Column saved with new ID: " + id + ", name: " + column.getName());
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Failed to save new column", e);
                            _errorMessage.postValue("保存列数据失败: " + e.getMessage());
                        }
                    });
                }
            } else {
                // 列已经有ID，可以直接更新
                // 保存到数据库 - 更新列名
                columnRepository.updateColumnName(column.getId(), column.getName(), new ColumnRepository.RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Column name updated successfully: " + column.getName());
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Failed to update column name", e);
                        _errorMessage.postValue("更新列名失败: " + e.getMessage());
                    }
                });
                
                // 保存到数据库 - 更新列宽
                columnRepository.updateColumnWidth(column.getId(), column.getWidth(), new ColumnRepository.RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Column width updated successfully: " + column.getWidth());
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Failed to update column width", e);
                        _errorMessage.postValue("更新列宽失败: " + e.getMessage());
                    }
                });
            }
        }
    }
    
    /**
     * 删除列
     */
    public void deleteColumn(long columnId) {
        List<Column> currentColumns = _columns.getValue();
        if (currentColumns != null) {
            Column columnToDelete = null;
            int deleteIndex = -1;
            
            // 找到要删除的列
            for (int i = 0; i < currentColumns.size(); i++) {
                if (currentColumns.get(i).getId() == columnId) {
                    columnToDelete = currentColumns.get(i);
                    deleteIndex = i;
                    break;
                }
            }
            
            if (columnToDelete != null && deleteIndex >= 0) {
                // 移除列
                currentColumns.remove(deleteIndex);
                
                // 更新后续列的索引
                for (int i = deleteIndex; i < currentColumns.size(); i++) {
                    Column col = currentColumns.get(i);
                    col.setColumnIndex(col.getColumnIndex() - 1);
                }
                
                _columns.postValue(currentColumns);
                
                // 删除对应的单元格数据
                int colIndex = columnToDelete.getColumnIndex();
                if (isFrozenColumnIndex(colIndex)) {
                    // 如果删除的是冻结列，清空冻结列数据
                    _frozenColumnCells.postValue(new ArrayList<>());
                } else {
                    // 删除可滚动列中对应列的单元格
                    List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
                    if (scrollableCells != null) {
                        scrollableCells.removeIf(cell -> cell.getColIndex() == colIndex);
                        // 更新后续列的索引
                        for (Cell cell : scrollableCells) {
                            if (cell.getColIndex() > colIndex) {
                                cell.setColIndex(cell.getColIndex() - 1);
                            }
                        }
                        _scrollableColumnsCells.postValue(scrollableCells);
                    }
                }
                
                _columnCount.postValue(currentColumns.size());
                markAsModified();
            }
        }
    }
    

    
    /**
     * 在指定位置插入行
     */
    public void insertRowAt(int position) {
        Integer currentRows = _rowCount.getValue();
        Integer currentCols = _columnCount.getValue();
        
        if (currentRows != null && currentCols != null && position >= 0 && position <= currentRows) {
            // 为冻结列插入新行
            List<Cell> frozenCells = _frozenColumnCells.getValue();
            if (frozenCells != null) {
                // 更新插入位置之后的行索引
                for (Cell cell : frozenCells) {
                    if (cell.getRowIndex() >= position) {
                        cell.setRowIndex(cell.getRowIndex() + 1);
                    }
                }
                
                // 插入新的冻结列单元格
                Cell newFrozenCell = new Cell();
                newFrozenCell.setRowIndex(position);
                int firstFrozenColIndex = getFirstFrozenColumnIndex();
                newFrozenCell.setColIndex(firstFrozenColIndex >= 0 ? firstFrozenColIndex : 0);
                newFrozenCell.setContent("行" + position);
                frozenCells.add(position, newFrozenCell);
                _frozenColumnCells.postValue(frozenCells);
                
                // 同步维护源数据缓存
                for (Cell cell : sourceFrozenCells) {
                    if (cell.getRowIndex() >= position) {
                        cell.setRowIndex(cell.getRowIndex() + 1);
                    }
                }
                Cell sourceFrozenCell = createCellCopy(newFrozenCell);
                sourceFrozenCells.add(position, sourceFrozenCell);
            }
            
            // 为可滚动列插入新行
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            if (scrollableCells != null) {
                // 更新插入位置之后的行索引
                for (Cell cell : scrollableCells) {
                    if (cell.getRowIndex() >= position) {
                        cell.setRowIndex(cell.getRowIndex() + 1);
                    }
                }
                
                // 插入新的可滚动列单元格
                for (int col = 1; col < currentCols; col++) {
                    Cell newCell = new Cell();
                    newCell.setRowIndex(position);
                    newCell.setColIndex(col);
                    newCell.setContent("");
                    scrollableCells.add(position * (currentCols - 1) + (col - 1), newCell);
                }
                _scrollableColumnsCells.postValue(scrollableCells);
                
                // 同步维护源数据缓存
                for (Cell cell : sourceScrollableCells) {
                    if (cell.getRowIndex() >= position) {
                        cell.setRowIndex(cell.getRowIndex() + 1);
                    }
                }
                for (int col = 1; col < currentCols; col++) {
                    Cell sourceCell = new Cell();
                    sourceCell.setRowIndex(position);
                    sourceCell.setColIndex(col);
                    sourceCell.setContent("");
                    sourceScrollableCells.add(position * (currentCols - 1) + (col - 1), sourceCell);
                }
            }
            
            _rowCount.postValue(currentRows + 1);
            
            // 维护行顺序数组
            insertKey(position);
            
            markAsModified();
        }
        Log.d(TAG, "Row inserted at position: " + position);
    }
    
    /**
     * 在指定位置插入列
     */
    public void insertColumnAt(int position) {
        List<Column> currentColumns = _columns.getValue();
        Integer currentRows = _rowCount.getValue();
        
        if (currentColumns != null && currentRows != null && position >= 0 && position <= currentColumns.size()) {
            // 更新插入位置之后的列索引
            for (Column column : currentColumns) {
                if (column.getColumnIndex() >= position) {
                    column.setColumnIndex(column.getColumnIndex() + 1);
                }
            }
            
            // 创建新列
            Column newColumn = new Column();
            newColumn.setColumnIndex(position);
            newColumn.setName("列" + (position + 1));
            newColumn.setWidth(120);
            newColumn.setType("TEXT");
            newColumn.setVisible(true);
            newColumn.setFrozen(false);
            
            currentColumns.add(position, newColumn);
            _columns.postValue(currentColumns);
            
            // 为新列添加单元格
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            if (scrollableCells != null) {
                // 更新插入位置之后的列索引
                for (Cell cell : scrollableCells) {
                    if (cell.getColIndex() >= position) {
                        cell.setColIndex(cell.getColIndex() + 1);
                    }
                }
                
                // 同步更新源缓存的列索引
                for (Cell cell : sourceScrollableCells) {
                    if (cell.getColIndex() >= position) {
                        cell.setColIndex(cell.getColIndex() + 1);
                    }
                }
                
                // 插入新的单元格
                for (int row = 0; row < currentRows; row++) {
                    Cell newCell = new Cell();
                    newCell.setRowIndex(row);
                    newCell.setColIndex(position);
                    if (row == 0) {
                        newCell.setContent("标题" + (position + 1));
                    } else {
                        newCell.setContent("");
                    }
                    scrollableCells.add(row * currentColumns.size() + position - 1, newCell);
                    
                    // 同步插入到源缓存
                    Cell sourceCell = new Cell();
                    sourceCell.setRowIndex(row);
                    sourceCell.setColIndex(position);
                    if (row == 0) {
                        sourceCell.setContent("标题" + (position + 1));
                    } else {
                        sourceCell.setContent("");
                    }
                    sourceScrollableCells.add(row * currentColumns.size() + position - 1, sourceCell);
                }
                _scrollableColumnsCells.postValue(scrollableCells);
            }
            
            _columnCount.postValue(currentColumns.size());
            markAsModified();
        }
        Log.d(TAG, "Column inserted at position: " + position);
    }
    
    /**
     * 删除指定位置的行
     */
    public void deleteRowAt(int position) {
        Integer currentRows = _rowCount.getValue();
        Integer currentCols = _columnCount.getValue();
        
        if (currentRows != null && currentCols != null && position >= 0 && position < currentRows && currentRows > 1) {
            // 快照要删除的行数据用于撤销
            List<Cell> rowSnapshot = snapshotRow(position, currentCols);
            
            // 记录撤销重做操作
            TableOperation operation = new TableOperation(
                TableOperation.OperationType.DELETE_ROW,
                position,
                null,
                null
            );
            operation.setAffectedCells(rowSnapshot);
            addToUndoStack(operation);
            
            // 删除冻结列中的行
            List<Cell> frozenCells = _frozenColumnCells.getValue();
            if (frozenCells != null) {
                frozenCells.removeIf(cell -> cell.getRowIndex() == position);
                
                // 更新删除位置之后的行索引
                for (Cell cell : frozenCells) {
                    if (cell.getRowIndex() > position) {
                        cell.setRowIndex(cell.getRowIndex() - 1);
                    }
                }
                _frozenColumnCells.postValue(frozenCells);
                
                // 同步维护源数据缓存
                sourceFrozenCells.removeIf(cell -> cell.getRowIndex() == position);
                for (Cell cell : sourceFrozenCells) {
                    if (cell.getRowIndex() > position) {
                        cell.setRowIndex(cell.getRowIndex() - 1);
                    }
                }
            }
            
            // 删除可滚动列中的行
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            if (scrollableCells != null) {
                scrollableCells.removeIf(cell -> cell.getRowIndex() == position);
                
                // 更新删除位置之后的行索引
                for (Cell cell : scrollableCells) {
                    if (cell.getRowIndex() > position) {
                        cell.setRowIndex(cell.getRowIndex() - 1);
                    }
                }
                _scrollableColumnsCells.postValue(scrollableCells);
                
                // 同步维护源数据缓存
                sourceScrollableCells.removeIf(cell -> cell.getRowIndex() == position);
                for (Cell cell : sourceScrollableCells) {
                    if (cell.getRowIndex() > position) {
                        cell.setRowIndex(cell.getRowIndex() - 1);
                    }
                }
            }
            
            _rowCount.postValue(currentRows - 1);
            
            // 维护行顺序数组
            removeKey(position);
            
            markAsModified();
        }
        Log.d(TAG, "Row deleted at position: " + position);
    }
    
    /**
     * 删除指定位置的列
     */
    public void deleteColumnAt(int position) {
        List<Column> currentColumns = _columns.getValue();
        Integer currentRows = _rowCount.getValue();
        
        if (currentColumns != null && currentRows != null && position >= 0 && position < currentColumns.size() && currentColumns.size() > 1) {
            // 快照要删除的列数据用于撤销
            List<Cell> columnSnapshot = snapshotColumn(position, currentRows);
            Column deletedColumn = null;
            for (Column column : currentColumns) {
                if (column.getColumnIndex() == position) {
                    deletedColumn = new Column(column); // 创建副本
                    break;
                }
            }
            
            // 记录撤销重做操作
            TableOperation operation = new TableOperation(
                TableOperation.OperationType.DELETE_COLUMN,
                position,
                null,
                null
            );
            operation.setAffectedCells(columnSnapshot);
            operation.setAffectedColumn(deletedColumn);
            addToUndoStack(operation);
            
            // 删除列定义
            currentColumns.removeIf(column -> column.getColumnIndex() == position);
            
            // 更新删除位置之后的列索引
            for (Column column : currentColumns) {
                if (column.getColumnIndex() > position) {
                    column.setColumnIndex(column.getColumnIndex() - 1);
                }
            }
            _columns.postValue(currentColumns);
            
            // 删除可滚动列中的单元格
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            if (scrollableCells != null) {
                scrollableCells.removeIf(cell -> cell.getColIndex() == position);
                
                // 更新删除位置之后的列索引
                for (Cell cell : scrollableCells) {
                    if (cell.getColIndex() > position) {
                        cell.setColIndex(cell.getColIndex() - 1);
                    }
                }
                _scrollableColumnsCells.postValue(scrollableCells);
            }
            
            // 同步删除源缓存中的单元格
            sourceScrollableCells.removeIf(cell -> cell.getColIndex() == position);
            
            // 同步更新源缓存中删除位置之后的列索引
            for (Cell cell : sourceScrollableCells) {
                if (cell.getColIndex() > position) {
                    cell.setColIndex(cell.getColIndex() - 1);
                }
            }
            
            _columnCount.postValue(currentColumns.size());
            markAsModified();
        }
        Log.d(TAG, "Column deleted at position: " + position);
    }
    
    // ==================== 撤销重做功能 ====================
    
    /**
     * 添加操作到撤销栈
     */
    private void addToUndoStack(TableOperation operation) {
        undoStack.push(operation);
        updateUndoRedoState();
    }
    
    /**
     * 撤销操作
     */
    public void undo() {
        if (!undoStack.isEmpty()) {
            TableOperation operation = undoStack.pop();
            executeUndoOperation(operation);
            updateUndoRedoState();
        }
    }
    

    
    /**
     * 执行撤销操作
     */
    private void executeUndoOperation(TableOperation operation) {
        switch (operation.getType()) {
            case ADD_ROW:
                // 撤销添加行：删除最后一行
                deleteRowAtInternal(operation.getPosition());
                break;
            case DELETE_ROW:
                // 撤销删除行：恢复行数据
                if (operation.getAffectedCells() != null) {
                    restoreRowData(operation.getPosition(), operation.getAffectedCells());
                }
                break;
            case ADD_COLUMN:
                // 撤销添加列：删除最后一列
                deleteColumnAtInternal(operation.getPosition());
                break;
            case DELETE_COLUMN:
                // 撤销删除列：恢复列数据
                if (operation.getAffectedColumn() != null) {
                    restoreColumnData(operation.getPosition(), operation.getAffectedColumn(), operation.getAffectedCells());
                }
                break;
            case UPDATE_CELL:
                // 撤销单元格更新：恢复旧值
                updateCellValueInternal(operation.getPosition() / 1000, operation.getPosition() % 1000, (String) operation.getOldValue());
                break;
        }
    }
    

    
    /**
     * 更新撤销状态
     */
    private void updateUndoRedoState() {
        _canUndo.postValue(!undoStack.isEmpty());
    }
    
    /**
     * 内部更新单元格值方法（不记录操作历史）
     */
    private void updateCellValueInternal(int row, int col, String value) {
        Notebook currentNotebook = _currentNotebook.getValue();
        if (currentNotebook == null) {
            return;
        }
        
        // 更新内存中的数据
        if (isFrozenColumnIndex(col)) {
            List<Cell> frozenCells = _frozenColumnCells.getValue();
            if (frozenCells != null) {
                for (Cell cell : frozenCells) {
                    if (cell.getRowIndex() == row && cell.getColIndex() == col) {
                        cell.setContent(value);
                        break;
                    }
                }
                _frozenColumnCells.postValue(frozenCells);
            }
        } else {
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            if (scrollableCells != null) {
                for (Cell cell : scrollableCells) {
                    if (cell.getRowIndex() == row && cell.getColIndex() == col) {
                        cell.setContent(value);
                        break;
                    }
                }
                _scrollableColumnsCells.postValue(scrollableCells);
            }
        }
        
        markAsModified();
    }
    
    /**
     * 内部添加行方法（不记录操作历史）
     */
    private void addRowInternal() {
        Integer currentRows = _rowCount.getValue();
        Integer currentCols = _columnCount.getValue();
        
        if (currentRows != null && currentCols != null) {
            int newRowIndex = currentRows;
            
            // 为冻结列添加新行
            List<Cell> frozenCells = _frozenColumnCells.getValue();
            if (frozenCells != null) {
                Cell newFrozenCell = new Cell();
                newFrozenCell.setRowIndex(newRowIndex);
                int firstFrozenColIndex = getFirstFrozenColumnIndex();
                newFrozenCell.setColIndex(firstFrozenColIndex >= 0 ? firstFrozenColIndex : 0);
                newFrozenCell.setContent("");
                frozenCells.add(newFrozenCell);
                _frozenColumnCells.postValue(frozenCells);
            }
            
            // 为可滚动列添加新行
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            if (scrollableCells != null) {
                for (int col = 1; col < currentCols; col++) {
                    Cell newCell = new Cell();
                    newCell.setRowIndex(newRowIndex);
                    newCell.setColIndex(col);
                    newCell.setContent("");
                    scrollableCells.add(newCell);
                }
                _scrollableColumnsCells.postValue(scrollableCells);
            }
            
            _rowCount.postValue(newRowIndex + 1);
            markAsModified();
        }
    }
    
    /**
     * 内部添加列方法（不记录操作历史）
     */
    private void addColumnInternal() {
        List<Column> currentColumns = _columns.getValue();
        Integer currentRows = _rowCount.getValue();
        
        if (currentColumns != null && currentRows != null) {
            int newColIndex = currentColumns.size();
            Column newColumn = new Column();
            newColumn.setColumnIndex(newColIndex);
            newColumn.setName("列" + (newColIndex + 1));
            newColumn.setWidth(150);
            newColumn.setType("TEXT");
            newColumn.setVisible(true);
            newColumn.setFrozen(false);
            
            currentColumns.add(newColumn);
            _columns.postValue(currentColumns);
            
            // 为新列添加单元格
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            if (scrollableCells != null) {
                for (int row = 0; row < currentRows; row++) {
                    Cell newCell = new Cell();
                    newCell.setRowIndex(row);
                    newCell.setColIndex(newColIndex);
                    newCell.setContent("");
                    scrollableCells.add(newCell);
                }
                _scrollableColumnsCells.postValue(scrollableCells);
            }
            
            _columnCount.postValue(currentColumns.size());
            markAsModified();
        }
    }
    
    /**
     * 内部删除行方法（不记录操作历史）
     */
    private void deleteRowAtInternal(int position) {
        Integer currentRows = _rowCount.getValue();
        if (currentRows != null && position >= 0 && position < currentRows && currentRows > 1) {
            // 删除冻结列中的行
            List<Cell> frozenCells = _frozenColumnCells.getValue();
            if (frozenCells != null) {
                frozenCells.removeIf(cell -> cell.getRowIndex() == position);
                
                // 更新删除位置之后的行索引
                for (Cell cell : frozenCells) {
                    if (cell.getRowIndex() > position) {
                        cell.setRowIndex(cell.getRowIndex() - 1);
                    }
                }
                _frozenColumnCells.postValue(frozenCells);
            }
            
            // 删除可滚动列中的行
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            if (scrollableCells != null) {
                scrollableCells.removeIf(cell -> cell.getRowIndex() == position);
                
                // 更新删除位置之后的行索引
                for (Cell cell : scrollableCells) {
                    if (cell.getRowIndex() > position) {
                        cell.setRowIndex(cell.getRowIndex() - 1);
                    }
                }
                _scrollableColumnsCells.postValue(scrollableCells);
            }
            
            _rowCount.postValue(currentRows - 1);
        }
    }
    
    /**
     * 内部删除列方法（不记录操作历史）
     */
    private void deleteColumnAtInternal(int position) {
        List<Column> currentColumns = _columns.getValue();
        if (currentColumns != null && position >= 0 && position < currentColumns.size() && currentColumns.size() > 1) {
            // 删除列定义
            currentColumns.removeIf(column -> column.getColumnIndex() == position);
            
            // 更新删除位置之后的列索引
            for (Column column : currentColumns) {
                if (column.getColumnIndex() > position) {
                    column.setColumnIndex(column.getColumnIndex() - 1);
                }
            }
            _columns.postValue(currentColumns);
            
            // 删除可滚动列中的单元格
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            if (scrollableCells != null) {
                scrollableCells.removeIf(cell -> cell.getColIndex() == position);
                
                // 更新删除位置之后的列索引
                for (Cell cell : scrollableCells) {
                    if (cell.getColIndex() > position) {
                        cell.setColIndex(cell.getColIndex() - 1);
                    }
                }
                _scrollableColumnsCells.postValue(scrollableCells);
            }
            
            _columnCount.postValue(currentColumns.size());
        }
    }
    

    /**
     * 快照指定行的所有单元格数据
     * @param rowIndex 行索引（原始行索引）
     * @param totalCols 总列数
     * @return 该行的所有单元格副本
     */
    private List<Cell> snapshotRow(int rowIndex, int totalCols) {
        List<Cell> rowSnapshot = new ArrayList<>();
        
        // 从源数据缓存中获取该行的所有单元格
        for (Cell cell : sourceFrozenCells) {
            if (cell.getRowIndex() == rowIndex) {
                rowSnapshot.add(createCellCopy(cell));
            }
        }
        
        for (Cell cell : sourceScrollableCells) {
            if (cell.getRowIndex() == rowIndex) {
                rowSnapshot.add(createCellCopy(cell));
            }
        }
        
        // 确保所有列都有单元格（为空列创建空单元格）
        for (int col = 0; col < totalCols; col++) {
            boolean found = false;
            for (Cell cell : rowSnapshot) {
                if (cell.getColIndex() == col) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Cell emptyCell = new Cell();
                emptyCell.setRowIndex(rowIndex);
                emptyCell.setColIndex(col);
                emptyCell.setContent("");
                Notebook notebook = _currentNotebook.getValue();
                if (notebook != null) {
                    emptyCell.setNotebookId(notebook.getId());
                }
                rowSnapshot.add(emptyCell);
            }
        }
        
        return rowSnapshot;
    }
    
    /**
     * 快照指定列的所有单元格数据
     * @param colIndex 列索引
     * @param totalRows 总行数
     * @return 该列的所有单元格副本
     */
    private List<Cell> snapshotColumn(int colIndex, int totalRows) {
        List<Cell> columnSnapshot = new ArrayList<>();
        
        // 从源数据缓存中获取该列的所有单元格
        if (isFrozenColumnIndex(colIndex)) {
            for (Cell cell : sourceFrozenCells) {
                if (cell.getColIndex() == colIndex) {
                    columnSnapshot.add(createCellCopy(cell));
                }
            }
        } else {
            for (Cell cell : sourceScrollableCells) {
                if (cell.getColIndex() == colIndex) {
                    columnSnapshot.add(createCellCopy(cell));
                }
            }
        }
        
        // 确保所有行都有单元格（为空行创建空单元格）
        for (int row = 0; row < totalRows; row++) {
            boolean found = false;
            for (Cell cell : columnSnapshot) {
                if (cell.getRowIndex() == row) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Cell emptyCell = new Cell();
                emptyCell.setRowIndex(row);
                emptyCell.setColIndex(colIndex);
                emptyCell.setContent("");
                Notebook notebook = _currentNotebook.getValue();
                if (notebook != null) {
                    emptyCell.setNotebookId(notebook.getId());
                }
                columnSnapshot.add(emptyCell);
            }
        }
        
        return columnSnapshot;
    }
    
    /**
     * 恢复行数据
     * @param position 插入位置
     * @param rowCells 要恢复的行单元格数据
     */
    private void restoreRowData(int position, List<Cell> rowCells) {
        if (rowCells == null || rowCells.isEmpty()) {
            return;
        }
        
        Integer currentRows = _rowCount.getValue();
        Integer currentCols = _columnCount.getValue();
        if (currentRows == null || currentCols == null) {
            return;
        }
        
        // 更新插入位置之后的行索引
        for (Cell cell : sourceFrozenCells) {
            if (cell.getRowIndex() >= position) {
                cell.setRowIndex(cell.getRowIndex() + 1);
            }
        }
        for (Cell cell : sourceScrollableCells) {
            if (cell.getRowIndex() >= position) {
                cell.setRowIndex(cell.getRowIndex() + 1);
            }
        }
        
        // 插入恢复的单元格到源数据缓存
        for (Cell cell : rowCells) {
            Cell restoredCell = createCellCopy(cell);
            restoredCell.setRowIndex(position);
            
            if (isFrozenColumnIndex(restoredCell.getColIndex())) {
                sourceFrozenCells.add(restoredCell);
            } else {
                sourceScrollableCells.add(restoredCell);
            }
        }
        
        // 更新行数
        _rowCount.postValue(currentRows + 1);
        
        // 维护行顺序数组
        insertKey(position);
        
        // 刷新视图
        refreshViewRespectingFilterAndSort();
        markAsModified();
    }
    
    /**
     * 恢复列数据
     * @param position 插入位置
     * @param column 要恢复的列定义
     * @param columnCells 要恢复的列单元格数据
     */
    private void restoreColumnData(int position, Column column, List<Cell> columnCells) {
        if (column == null) {
            return;
        }
        
        Integer currentRows = _rowCount.getValue();
        Integer currentCols = _columnCount.getValue();
        if (currentRows == null || currentCols == null) {
            return;
        }
        
        // 更新插入位置之后的列索引
        List<Column> currentColumns = _columns.getValue();
        if (currentColumns != null) {
            for (Column col : currentColumns) {
                if (col.getColumnIndex() >= position) {
                    col.setColumnIndex(col.getColumnIndex() + 1);
                }
            }
            
            // 插入恢复的列
            Column restoredColumn = new Column();
            restoredColumn.setColumnIndex(position);
            restoredColumn.setName(column.getName());
            restoredColumn.setWidth(column.getWidth());
            restoredColumn.setType(column.getType());
            restoredColumn.setVisible(column.isVisible());
            restoredColumn.setFrozen(column.isFrozen());
            currentColumns.add(position, restoredColumn);
            _columns.postValue(currentColumns);
        }
        
        // 更新所有单元格的列索引
        for (Cell cell : sourceFrozenCells) {
            if (cell.getColIndex() >= position) {
                cell.setColIndex(cell.getColIndex() + 1);
            }
        }
        for (Cell cell : sourceScrollableCells) {
            if (cell.getColIndex() >= position) {
                cell.setColIndex(cell.getColIndex() + 1);
            }
        }
        
        // 插入恢复的单元格到源数据缓存
        if (columnCells != null) {
            for (Cell cell : columnCells) {
                Cell restoredCell = createCellCopy(cell);
                restoredCell.setColIndex(position);
                
                if (isFrozenColumnIndex(position)) {
                    sourceFrozenCells.add(restoredCell);
                } else {
                    sourceScrollableCells.add(restoredCell);
                }
            }
        }
        
        // 更新列数
        _columnCount.postValue(currentCols + 1);
        
        // 刷新视图
        refreshViewRespectingFilterAndSort();
        markAsModified();
    }
    
    /**
     * 保存列数据
     */
    private void saveColumnsData() {
        Notebook notebook = _currentNotebook.getValue();
        List<Column> columns = _columns.getValue();
        if (notebook != null && columns != null && !columns.isEmpty()) {
            for (Column column : columns) {
                column.setNotebookId(notebook.getId());
            }
            
            columnRepository.saveColumns(columns, new ColumnRepository.RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Columns updated successfully");
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Failed to update columns", e);
                    _errorMessage.postValue("更新列数据失败: " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * 推断列的数据类型
     * 基于列内多数值的类型来推断整列的类型
     */
    public CellType inferColumnType(int colIndex) {
        List<Cell> allCells = new ArrayList<>();
        
        // 收集该列的所有单元格
        List<Cell> frozenCells = _frozenColumnCells.getValue();
        List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
        
        if (frozenCells != null) {
            for (Cell cell : frozenCells) {
                if (cell.getColIndex() == colIndex) {
                    allCells.add(cell);
                }
            }
        }
        
        if (scrollableCells != null) {
            for (Cell cell : scrollableCells) {
                if (cell.getColIndex() == colIndex) {
                    allCells.add(cell);
                }
            }
        }
        
        return inferColumnTypeFromCells(allCells);
    }
    
    /**
     * 根据单元格列表推断列类型
     */
    private CellType inferColumnTypeFromCells(List<Cell> cells) {
        if (cells.isEmpty()) {
            return CellType.TEXT;
        }
        
        // 统计各种类型的数量
        int textCount = 0;
        int numberCount = 0;
        int dateCount = 0;
        int booleanCount = 0;
        int imageCount = 0;
        int emptyCount = 0;
        
        for (Cell cell : cells) {
            String content = cell.getContent();
            if (content == null || content.trim().isEmpty()) {
                emptyCount++;
                continue;
            }
            
            CellType detectedType = CellType.detectType(content);
            switch (detectedType) {
                case NUMBER:
                    numberCount++;
                    break;
                case DATE:
                    dateCount++;
                    break;
                case BOOLEAN:
                    booleanCount++;
                    break;
                case IMAGE:
                    imageCount++;
                    break;
                default:
                    textCount++;
                    break;
            }
        }
        
        // 找出数量最多的类型（排除空值）
        int totalNonEmpty = cells.size() - emptyCount;
        if (totalNonEmpty == 0) {
            return CellType.TEXT;
        }
        
        // 如果某种类型占比超过50%，则认为是该类型
        double threshold = 0.5;
        
        if (numberCount > totalNonEmpty * threshold) {
            return CellType.NUMBER;
        }
        if (dateCount > totalNonEmpty * threshold) {
            return CellType.DATE;
        }
        if (booleanCount > totalNonEmpty * threshold) {
            return CellType.BOOLEAN;
        }
        if (imageCount > totalNonEmpty * threshold) {
            return CellType.IMAGE;
        }
        
        // 默认返回文本类型
        return CellType.TEXT;
    }
    
    /**
     * 自动推断并更新单个列的类型
     */
    public void autoInferColumnType(int columnIndex) {
        List<Column> columns = _columns.getValue();
        if (columns == null) return;
        
        for (Column column : columns) {
             if (column.getColumnIndex() == columnIndex) {
                 CellType inferredType = inferColumnType(columnIndex);
                 if (!inferredType.name().equals(column.getType())) {
                     column.setType(inferredType.name());
                     _columns.postValue(columns);
                     saveColumnsData(); // 保存更新后的列信息
                 }
                 break;
             }
         }
    }
    
    /**
     * 自动推断并更新所有列的类型
     */
    public void autoInferAllColumnTypes() {
        List<Column> columns = _columns.getValue();
        if (columns == null) return;
        
        boolean hasChanges = false;
         for (Column column : columns) {
             CellType inferredType = inferColumnType(column.getColumnIndex());
             if (!inferredType.name().equals(column.getType())) {
                 column.setType(inferredType.name());
                 hasChanges = true;
             }
         }
         
         if (hasChanges) {
             _columns.postValue(columns);
             saveColumnsData(); // 保存更新后的列信息
         }
    }
    
    /**
     * 更新单元格样式
     */
    public void updateCellStyle(int row, int col, String textColor, String backgroundColor, 
                               boolean isBold, boolean isItalic, int textSize, String textAlignment) {
        Notebook notebook = _currentNotebook.getValue();
        if (notebook == null) {
            _errorMessage.setValue("当前没有打开的笔记本");
            return;
        }
        
        // 行号映射：将显示行号转换为原始行号
        final int originalRow;
        if (currentRowOrder != null && row < currentRowOrder.length) {
            originalRow = currentRowOrder[row];
            Log.d(TAG, "Row mapping for style update: display row " + row + " -> original row " + originalRow);
        } else {
            originalRow = row;
        }
        
        // 使用原始行号进行数据库操作
        cellRepository.updateCellFormat(notebook.getId(), originalRow, col, textColor, backgroundColor, 
                                      isBold, isItalic, textSize, textAlignment, new CellRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // 更新本地数据（使用显示行号）
                updateLocalCellStyle(row, col, textColor, backgroundColor, isBold, isItalic, textSize, textAlignment);
                markAsModified();
            }
            
            @Override
            public void onError(Exception error) {
                _errorMessage.setValue("更新单元格样式失败: " + error.getMessage());
            }
        });
    }
    
    /**
     * 更新本地单元格样式数据
     */
    private void updateLocalCellStyle(int row, int col, String textColor, String backgroundColor, 
                                     boolean isBold, boolean isItalic, int textSize, String textAlignment) {
        // 更新冻结列单元格
        List<Cell> frozenCells = _frozenColumnCells.getValue();
        if (frozenCells != null) {
            for (Cell cell : frozenCells) {
                if (cell.getRowIndex() == row && cell.getColIndex() == col) {
                    cell.setTextColor(textColor);
                    cell.setBackgroundColor(backgroundColor);
                    cell.setBold(isBold);
                    cell.setItalic(isItalic);
                    cell.setTextSize(textSize);
                    cell.setTextAlignment(textAlignment);
                    break;
                }
            }
            _frozenColumnCells.setValue(frozenCells);
        }
        
        // 更新可滚动列单元格
        List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
        if (scrollableCells != null) {
            for (Cell cell : scrollableCells) {
                if (cell.getRowIndex() == row && cell.getColIndex() == col) {
                    cell.setTextColor(textColor);
                    cell.setBackgroundColor(backgroundColor);
                    cell.setBold(isBold);
                    cell.setItalic(isItalic);
                    cell.setTextSize(textSize);
                    cell.setTextAlignment(textAlignment);
                    break;
                }
            }
            _scrollableColumnsCells.setValue(scrollableCells);
        }
    }
    
    /**
     * 更新列名
     */
    public void updateColumnName(int columnIndex, String newColumnName) {
        List<Column> currentColumns = _columns.getValue();
        if (currentColumns != null && columnIndex >= 0 && columnIndex < currentColumns.size()) {
            Column column = currentColumns.get(columnIndex);
            column.setName(newColumnName);
            
            // 更新数据库
            columnRepository.updateColumnName(column.getId(), newColumnName, new ColumnRepository.RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // 更新本地数据
                    _columns.postValue(currentColumns);
                    markAsModified();
                }
                
                @Override
                public void onError(Exception error) {
                    _errorMessage.setValue("更新列名失败: " + error.getMessage());
                }
            });
        }
    }
    
    /**
     * 按列索引排序
     */

    

    
    /**
     * 按列筛选
     */
    public void filterByColumn(int columnIndex, String filterType, String filterValue) {
        filterByColumn(columnIndex, filterType, filterValue, 0.0, Double.MAX_VALUE);
    }
    
    /**
     * 按列值集合筛选（多选筛选）- DataGrip风格
     * 当selectedValues为空或包含所有值时，显示全部数据
     * 否则只显示选中值对应的行
     */
    public void filterByColumnValues(int columnIndex, Set<String> selectedValues) {
        // 1) 空集合或"全选" => 清除筛选
        if (selectedValues == null || selectedValues.isEmpty()) {
            activeVisibleRows = null;
            refreshViewRespectingFilterAndSort();
            return;
        }

        // 2) 用源缓存计算该列所有值
        List<Cell> allSourceCells = getSourceCells();
        if (allSourceCells.isEmpty()) return;

        Set<String> allColumnValues = new HashSet<>();
        for (Cell cell : allSourceCells) {
            if (cell.getColIndex() == columnIndex) {
                allColumnValues.add(cell.getContent() != null ? cell.getContent() : "");
            }
        }
        if (selectedValues.containsAll(allColumnValues)) {
            activeVisibleRows = null;
            refreshViewRespectingFilterAndSort();
            return;
        }

        // 3) 求可见的"原始行索引"
        Set<Integer> valid = new HashSet<>();
        for (Cell cell : allSourceCells) {
            if (cell.getColIndex() == columnIndex) {
                String v = cell.getContent() != null ? cell.getContent() : "";
                if (selectedValues.contains(v)) valid.add(cell.getRowIndex());
            }
        }

        // 4) 记录为状态 + 渲染（排序会在渲染阶段应用）
        activeVisibleRows = new ArrayList<>(valid);
        refreshViewRespectingFilterAndSort();
    }

    /**
     * 获取源数据单元格列表
     */
    public List<Cell> getSourceCells() {
        List<Cell> allSourceCells = new ArrayList<>();
        if (sourceFrozenCells != null) {
            allSourceCells.addAll(sourceFrozenCells);
        }
        if (sourceScrollableCells != null) {
            allSourceCells.addAll(sourceScrollableCells);
        }
        return allSourceCells;
    }
    
    /**
     * 按列筛选（带数值范围）
     */
    public void filterByColumn(int columnIndex, String filterType, String filterValue, double minValue, double maxValue) {
        Notebook notebook = _currentNotebook.getValue();
        if (notebook == null) {
            return;
        }
        
        // 使用Repository中的筛选方法，返回LiveData
        LiveData<List<Cell>> filteredCells = cellRepository.getCellsByColumnWithFilter(
            notebook.getId(), columnIndex, filterType, filterValue, minValue, maxValue, "ASC");
            
        // 观察筛选结果
        filteredCells.observeForever(new Observer<List<Cell>>() {
            @Override
            public void onChanged(List<Cell> result) {
                if (result != null) {
                    // 分离冻结列和可滚动列
                    List<Cell> newFrozenCells = new ArrayList<>();
                    List<Cell> newScrollableCells = new ArrayList<>();
                    
                    for (Cell cell : result) {
                        if (isFrozenColumnIndex(cell.getColIndex())) {
                            newFrozenCells.add(cell);
                        } else {
                            newScrollableCells.add(cell);
                        }
                    }
                    
                    _frozenColumnCells.postValue(newFrozenCells);
                    _scrollableColumnsCells.postValue(newScrollableCells);
                    
                    // 更新行数
                    int maxRow = 0;
                    for (Cell cell : result) {
                        maxRow = Math.max(maxRow, cell.getRowIndex());
                    }
                    _rowCount.postValue(maxRow + 1);
                }
                
                // 移除观察者避免内存泄漏
                filteredCells.removeObserver(this);
            }
        });
    }
    
    /**
     * 重新加载数据（清除筛选和排序）
     */
    public void reloadData() {
        Notebook notebook = _currentNotebook.getValue();
        if (notebook != null) {
            loadCellData(notebook.getId());
        }
    }
    
    // ========== 行维护辅助方法 ==========
    
    /**
     * 获取下一个原始行键值
     */
    private int nextOriginalKey() {
        if (originalRowOrder == null || originalRowOrder.length == 0) {
            return 0;
        }
        int max = originalRowOrder[0];
        for (int key : originalRowOrder) {
            max = Math.max(max, key);
        }
        return max + 1;
    }
    
    /**
     * 在指定位置插入新行，维护行顺序数组
     * @param insertPosition 插入位置（显示位置）
     */
    private void insertKey(int insertPosition) {
        if (originalRowOrder == null || currentRowOrder == null) {
            return;
        }
        
        int newKey = nextOriginalKey();
        int[] newOriginal = new int[originalRowOrder.length + 1];
        int[] newCurrent = new int[currentRowOrder.length + 1];
        
        // 复制原始数组
        System.arraycopy(originalRowOrder, 0, newOriginal, 0, originalRowOrder.length);
        newOriginal[originalRowOrder.length] = newKey;
        
        // 在当前顺序中插入新键
        for (int i = 0; i < insertPosition; i++) {
            newCurrent[i] = currentRowOrder[i];
        }
        newCurrent[insertPosition] = newKey;
        for (int i = insertPosition; i < currentRowOrder.length; i++) {
            newCurrent[i + 1] = currentRowOrder[i];
        }
        
        originalRowOrder = newOriginal;
        currentRowOrder = newCurrent;
    }
    
    /**
     * 删除指定位置的行，维护行顺序数组
     * @param removePosition 删除位置（显示位置）
     */
    private void removeKey(int removePosition) {
        if (currentRowOrder == null || removePosition < 0 || removePosition >= currentRowOrder.length) {
            return;
        }
        
        int removedKey = currentRowOrder[removePosition];
        
        // 从originalRowOrder中移除
        int[] newOriginal = new int[originalRowOrder.length - 1];
        int originalIndex = 0;
        for (int key : originalRowOrder) {
            if (key != removedKey) {
                newOriginal[originalIndex++] = key;
            }
        }
        
        // 从currentRowOrder中移除
        int[] newCurrent = new int[currentRowOrder.length - 1];
        int currentIndex = 0;
        for (int i = 0; i < currentRowOrder.length; i++) {
            if (i != removePosition) {
                newCurrent[currentIndex++] = currentRowOrder[i];
            }
        }
        
        originalRowOrder = newOriginal;
        currentRowOrder = newCurrent;
    }
    
    /**
     * 深拷贝单元格列表
     * @param source 源单元格列表
     * @return 深拷贝后的单元格列表
     */
    private List<Cell> deepCopyCells(List<Cell> source) {
        List<Cell> result = new ArrayList<>();
        for (Cell cell : source) {
            result.add(createCellCopy(cell));
        }
        return result;
    }
    


    /**
     * 获取源数据的总行数
     * @return 源数据的总行数
     */
    private int totalRowsFromSource() {
        int max = -1;
        if (sourceFrozenCells != null) {
            for (Cell c : sourceFrozenCells) {
                max = Math.max(max, c.getRowIndex());
            }
        }
        if (sourceScrollableCells != null) {
            for (Cell c : sourceScrollableCells) {
                max = Math.max(max, c.getRowIndex());
            }
        }
        return max + 1;
    }

    /**
     * 获取总列数（基于列定义）
     * @return 总列数
     */
    private int totalColsFromColumns() {
        List<Column> cs = _columns.getValue();
        return (cs == null) ? 0 : cs.size();
    }
    
    /**
     * 构建指定列的值计数列表（根据修改建议.md要求）
     * @param columnIndex 列索引
     * @return FilterOption列表，包含值、显示文本、计数和选中状态
     */
    public List<FilterOption> buildValueCountsForColumn(int columnIndex) {
        return buildValueCountsForColumn(columnIndex, false);
    }
    
    /**
     * 构建指定列的值计数列表（支持多列联动筛选）
     * @param columnIndex 列索引
     * @param respectCurrentFilter 是否基于当前筛选结果统计（true=多列联动，false=基于源数据）
     * @return FilterOption列表，包含值、显示文本、计数和选中状态
     */
    public List<FilterOption> buildValueCountsForColumn(int columnIndex, boolean respectCurrentFilter) {
        List<Cell> sourceCells;
        
        if (respectCurrentFilter) {
            // 基于当前筛选结果统计（多列联动模式）
            sourceCells = new ArrayList<>();
            List<Cell> frozenCells = _frozenColumnCells.getValue();
            List<Cell> scrollableCells = _scrollableColumnsCells.getValue();
            
            if (frozenCells != null) {
                sourceCells.addAll(frozenCells);
            }
            if (scrollableCells != null) {
                sourceCells.addAll(scrollableCells);
            }
        } else {
            // 基于源数据统计（独立筛选模式）
            sourceCells = getSourceCells();
        }
        
        Map<String, Integer> valueCountMap = new HashMap<>();
        
        // 如果源数据为空，直接返回空列表，不回退到当前显示数据
        if (sourceCells.isEmpty()) {
            Log.w(TAG, "Source cells is empty, returning empty filter options");
            return new ArrayList<>();
        }
        
        // 统计指定列的值出现次数（包含所有行）
        Log.d(TAG, "Building value counts for column " + columnIndex + ", source cells count: " + sourceCells.size());
        for (Cell cell : sourceCells) {
            if (cell.getColIndex() == columnIndex) {
                String value = cell.getContent() != null ? cell.getContent() : "";
                int currentCount = valueCountMap.getOrDefault(value, 0);
                valueCountMap.put(value, currentCount + 1);
                Log.d(TAG, "Column " + columnIndex + ", row " + cell.getRowIndex() + ", value '" + value + "' count: " + (currentCount + 1));
            }
        }
        
        // 转换为FilterOption列表
        List<FilterOption> filterOptions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : valueCountMap.entrySet()) {
            filterOptions.add(new FilterOption(entry.getKey(), entry.getValue(), true));
        }
        
        // 按计数降序排序，计数相同时按值升序，空值排在最后
        filterOptions.sort((a, b) -> {
            // 空值处理：空值排在最后
            if (a.value.isEmpty() && b.value.isEmpty()) return 0;
            if (a.value.isEmpty()) return 1;
            if (b.value.isEmpty()) return -1;
            
            // 按计数降序排序
            int countCompare = Integer.compare(b.count, a.count);
            if (countCompare != 0) {
                return countCompare;
            }
            
            // 计数相同时按值升序排序
            return a.value.compareToIgnoreCase(b.value);
        });
        
        Log.d(TAG, "Built " + filterOptions.size() + " filter options for column " + columnIndex);
        return filterOptions;
    }
    
    /**
     * 应用值筛选（根据修改建议.md要求）
     * @param columnIndex 列索引
     * @param filterOptions 筛选选项列表
     */
    public void applyValueFilter(int columnIndex, List<FilterOption> filterOptions) {
        if (filterOptions == null || filterOptions.isEmpty()) {
            reloadData();
            return;
        }
        
        // 收集选中的值
        Set<String> selectedValues = new HashSet<>();
        boolean hasSelection = false;
        boolean allSelected = true;
        
        for (FilterOption option : filterOptions) {
            if (option.checked) {
                selectedValues.add(option.value);
                hasSelection = true;
            } else {
                allSelected = false;
            }
        }
        
        // DataGrip风格逻辑：未选中任何值或全选时显示全部数据
        if (!hasSelection || allSelected) {
            reloadData();
            return;
        }
        
        // 应用筛选
        filterByColumnValues(columnIndex, selectedValues);
    }
    
    /**
     * 重新应用当前的排序状态
     * 用于在筛选后保持排序顺序
     */
    private void reapplyCurrentSort() {
        refreshViewRespectingFilterAndSort();
    }

    /**
     * 清除筛选
     */
    public void clearFilter() {
        activeVisibleRows = null;
        refreshViewRespectingFilterAndSort();
    }
    
    // ==================== 视口状态管理方法 ====================
    
    /**
     * 获取当前缩放比例
     */
    public LiveData<Float> getScale() {
        return scale;
    }
    
    /**
     * 获取当前X偏移量
     */
    public LiveData<Float> getOffsetX() {
        return offsetX;
    }
    
    /**
     * 获取当前Y偏移量
     */
    public LiveData<Float> getOffsetY() {
        return offsetY;
    }
    
    /**
     * 更新视口状态
     */
    public void updateViewport(float scale, float offsetX, float offsetY) {
        _scale.setValue(scale);
        _offsetX.setValue(offsetX);
        _offsetY.setValue(offsetY);
    }
    
    /**
     * 重置视口到初始状态
     */
    public void resetViewport() {
        _scale.setValue(1.0f);
        _offsetX.setValue(0f);
        _offsetY.setValue(0f);
    }
    
    /**
     * 获取当前缩放比例值
     */
    public float getCurrentScale() {
        Float currentScale = _scale.getValue();
        return currentScale != null ? currentScale : 1.0f;
    }
    
    /**
     * 获取当前X偏移量值
     */
    public float getCurrentOffsetX() {
        Float currentOffsetX = _offsetX.getValue();
        return currentOffsetX != null ? currentOffsetX : 0f;
    }
    
    /**
     * 获取当前Y偏移量值
     */
    public float getCurrentOffsetY() {
        Float currentOffsetY = _offsetY.getValue();
        return currentOffsetY != null ? currentOffsetY : 0f;
    }

}