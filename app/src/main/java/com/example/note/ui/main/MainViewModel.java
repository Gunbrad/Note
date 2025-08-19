package com.example.note.ui.main;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.note.data.entity.Notebook;
import com.example.note.data.repository.NotebookRepository;
import com.example.note.ui.base.BaseViewModel;
import com.example.note.util.ColorUtils;

import java.util.List;

/**
 * 主页ViewModel
 * 管理笔记本列表的显示和操作
 */
public class MainViewModel extends BaseViewModel {
    
    private static final String TAG = "MainViewModel";
    
    private final NotebookRepository notebookRepository;
    
    // 搜索查询
    private final MutableLiveData<String> _searchQuery = new MutableLiveData<>("");
    public final LiveData<String> searchQuery = _searchQuery;
    
    // 排序方式
    private final MutableLiveData<SortType> _sortType = new MutableLiveData<>(SortType.UPDATED_DESC);
    public final LiveData<SortType> sortType = _sortType;
    
    // 筛选颜色
    private final MutableLiveData<String> _filterColor = new MutableLiveData<>(null);
    public final LiveData<String> filterColor = _filterColor;
    
    // 笔记本列表（根据搜索和筛选条件动态变化）
    private final MediatorLiveData<List<Notebook>> _notebooks = new MediatorLiveData<>();
    public final LiveData<List<Notebook>> notebooks = _notebooks;
    
    // 原始笔记本列表
    private final LiveData<List<Notebook>> allNotebooks;
    
    // 笔记本总数
    public final LiveData<Integer> notebookCount;
    
    // 最近的笔记本
    private final LiveData<List<Notebook>> recentNotebooks;
    
    // 选中的笔记本
    private final MutableLiveData<Notebook> _selectedNotebook = new MutableLiveData<>();
    public final LiveData<Notebook> selectedNotebook = _selectedNotebook;
    
    // 布局模式
    private final MutableLiveData<LayoutMode> _layoutMode = new MutableLiveData<>(LayoutMode.WATERFALL);
    public final LiveData<LayoutMode> layoutMode = _layoutMode;
    
    public MainViewModel(@NonNull Application application) {
        super(application);
        
        notebookRepository = NotebookRepository.getInstance(application);
        
        // 初始化LiveData
        allNotebooks = notebookRepository.getAllNotebooks();
        notebookCount = notebookRepository.getNotebookCount();
        recentNotebooks = notebookRepository.getRecentNotebooks(10);
        
        // 设置笔记本列表的数据源
        setupNotebooksLiveData();
    }
    
    /**
     * 设置笔记本列表的数据源
     */
    private void setupNotebooksLiveData() {
        // 添加数据源
        _notebooks.addSource(allNotebooks, this::updateNotebooks);
        _notebooks.addSource(_searchQuery, query -> updateNotebooks(allNotebooks.getValue()));
        _notebooks.addSource(_filterColor, color -> updateNotebooks(allNotebooks.getValue()));
        _notebooks.addSource(_sortType, sortType -> updateNotebooks(allNotebooks.getValue()));
    }
    
    /**
     * 更新笔记本列表
     */
    private void updateNotebooks(List<Notebook> notebooks) {
        if (notebooks == null) {
            _notebooks.setValue(null);
            setEmpty(true);
            return;
        }
        
        List<Notebook> filteredNotebooks = filterAndSortNotebooks(notebooks);
        _notebooks.setValue(filteredNotebooks);
        setEmpty(filteredNotebooks.isEmpty());
    }
    
    /**
     * 筛选和排序笔记本
     */
    private List<Notebook> filterAndSortNotebooks(List<Notebook> notebooks) {
        if (notebooks == null || notebooks.isEmpty()) {
            return notebooks;
        }
        
        // 应用搜索筛选
        String query = _searchQuery.getValue();
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            notebooks = notebooks.stream()
                    .filter(notebook -> notebook.getTitle().toLowerCase().contains(lowerQuery))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // 应用颜色筛选
        String color = _filterColor.getValue();
        if (color != null && !color.trim().isEmpty()) {
            notebooks = notebooks.stream()
                    .filter(notebook -> color.equals(notebook.getColor()))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // 应用排序
        SortType sort = _sortType.getValue();
        if (sort != null) {
            switch (sort) {
                case TITLE_ASC:
                    notebooks.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
                    break;
                case TITLE_DESC:
                    notebooks.sort((a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()));
                    break;
                case CREATED_ASC:
                    notebooks.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                    break;
                case CREATED_DESC:
                    notebooks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    break;
                case UPDATED_ASC:
                    notebooks.sort((a, b) -> Long.compare(a.getUpdatedAt(), b.getUpdatedAt()));
                    break;
                case UPDATED_DESC:
                default:
                    notebooks.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
                    break;
            }
        }
        
        return notebooks;
    }
    
    /**
     * 搜索笔记本
     */
    public void searchNotebooks(String query) {
        String trimmedQuery = query != null ? query.trim() : "";
        if (!trimmedQuery.equals(_searchQuery.getValue())) {
            _searchQuery.setValue(trimmedQuery);
            Log.d(TAG, "Search query changed: " + trimmedQuery);
        }
    }
    
    /**
     * 设置排序方式
     */
    public void setSortType(SortType sortType) {
        if (sortType != _sortType.getValue()) {
            _sortType.setValue(sortType);
            Log.d(TAG, "Sort type changed: " + sortType);
        }
    }
    
    /**
     * 设置筛选颜色
     */
    public void setFilterColor(String color) {
        if ((color == null && _filterColor.getValue() != null) || 
            (color != null && !color.equals(_filterColor.getValue()))) {
            _filterColor.setValue(color);
            Log.d(TAG, "Filter color changed: " + color);
        }
    }
    
    /**
     * 清除筛选
     */
    public void clearFilters() {
        _searchQuery.setValue("");
        _filterColor.setValue(null);
        Log.d(TAG, "Filters cleared");
    }
    
    /**
     * 设置布局模式
     */
    public void setLayoutMode(LayoutMode mode) {
        if (mode != _layoutMode.getValue()) {
            _layoutMode.setValue(mode);
            Log.d(TAG, "Layout mode changed: " + mode);
        }
    }
    
    /**
     * 创建新笔记本
     */
    public void createNotebook(String title, String color) {
        setLoading(true);
        
        String finalColor = color != null ? color : ColorUtils.getRandomNotebookColor();
        
        notebookRepository.createNotebook(title, finalColor, new NotebookRepository.RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                handleRepositoryCallback(result, null, "笔记本创建成功");
                Log.d(TAG, "Notebook created successfully: " + result);
            }
            
            @Override
            public void onError(Exception error) {
                handleRepositoryCallback(null, error);
            }
        });
    }
    
    /**
     * 更新笔记本标题
     */
    public void updateNotebookTitle(long id, String title) {
        setLoading(true);
        
        notebookRepository.updateNotebookTitle(id, title, new NotebookRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                handleRepositoryCallback(result, null, "标题更新成功");
            }
            
            @Override
            public void onError(Exception error) {
                handleRepositoryCallback(null, error);
            }
        });
    }
    
    /**
     * 更新笔记本颜色
     */
    public void updateNotebookColor(long id, String color) {
        setLoading(true);
        
        notebookRepository.updateNotebookColor(id, color, new NotebookRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                handleRepositoryCallback(result, null, "颜色更新成功");
            }
            
            @Override
            public void onError(Exception error) {
                handleRepositoryCallback(null, error);
            }
        });
    }
    
    /**
     * 删除笔记本
     */
    public void deleteNotebook(long id) {
        setLoading(true);
        
        notebookRepository.deleteNotebook(id, new NotebookRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                handleRepositoryCallback(result, null, "笔记本已删除");
            }
            
            @Override
            public void onError(Exception error) {
                handleRepositoryCallback(null, error);
            }
        });
    }
    
    /**
     * 选择笔记本
     */
    public void selectNotebook(Notebook notebook) {
        _selectedNotebook.setValue(notebook);
        Log.d(TAG, "Notebook selected: " + (notebook != null ? notebook.getId() : "null"));
    }
    
    /**
     * 清除选择
     */
    public void clearSelection() {
        _selectedNotebook.setValue(null);
        Log.d(TAG, "Selection cleared");
    }
    
    /**
     * 获取最近的笔记本
     */
    public LiveData<List<Notebook>> getRecentNotebooks() {
        return recentNotebooks;
    }
    
    /**
     * 获取当前搜索查询
     */
    public String getCurrentSearchQuery() {
        return _searchQuery.getValue();
    }
    
    /**
     * 获取当前排序方式
     */
    public SortType getCurrentSortType() {
        return _sortType.getValue();
    }
    
    /**
     * 获取当前筛选颜色
     */
    public String getCurrentFilterColor() {
        return _filterColor.getValue();
    }
    
    /**
     * 获取当前布局模式
     */
    public LayoutMode getCurrentLayoutMode() {
        return _layoutMode.getValue();
    }
    
    /**
     * 检查是否有筛选条件
     */
    public boolean hasFilters() {
        String query = _searchQuery.getValue();
        String color = _filterColor.getValue();
        return (query != null && !query.trim().isEmpty()) || 
               (color != null && !color.trim().isEmpty());
    }
    
    @Override
    protected void onInitialize() {
        Log.d(TAG, "MainViewModel initialized");
        // 初始化时不需要特殊操作，LiveData会自动加载数据
    }
    
    @Override
    protected void onRefresh() {
        Log.d(TAG, "Refreshing main data");
        // 主页数据通过LiveData自动刷新，这里只需要结束刷新状态
        setRefreshing(false);
    }
    
    /**
     * 检查笔记名称是否已存在
     */
    public void checkNotebookNameExists(String name, NotebookRepository.RepositoryCallback<Boolean> callback) {
        notebookRepository.checkNotebookNameExists(name, callback);
    }
    
    /**
     * 排序类型枚举
     */
    public enum SortType {
        TITLE_ASC("标题升序"),
        TITLE_DESC("标题降序"),
        CREATED_ASC("创建时间升序"),
        CREATED_DESC("创建时间降序"),
        UPDATED_ASC("更新时间升序"),
        UPDATED_DESC("更新时间降序");
        
        private final String displayName;
        
        SortType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 布局模式枚举
     */
    public enum LayoutMode {
        WATERFALL("瀑布流"),
        GRID("网格"),
        LIST("列表");
        
        private final String displayName;
        
        LayoutMode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}