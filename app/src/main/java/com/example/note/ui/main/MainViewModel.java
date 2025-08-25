package com.example.note.ui.main;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
        _notebooks.addSource(_sortType, sortType -> updateNotebooks(allNotebooks.getValue()));
    }
    
    /**
     * 更新笔记本列表
     */
    private void updateNotebooks(List<Notebook> source) {
        if (source == null) {
            _notebooks.setValue(null);
            setEmpty(true);
            return;
        }
        
        // 重要：永远返回"新实例"
        List<Notebook> result = filterAndSortNotebooks(source);
        _notebooks.setValue(result); // result 已是新实例
        setEmpty(result.isEmpty());
    }
    
    /**
     * 筛选和排序笔记本
     */
    private List<Notebook> filterAndSortNotebooks(List<Notebook> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 重要：先复制，后续所有操作都基于副本
        List<Notebook> list = new ArrayList<>(source);
        
        // 应用搜索筛选（仅支持笔记名模糊匹配）
        String query = _searchQuery.getValue();
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            List<Notebook> filtered = new ArrayList<>();
            for (Notebook notebook : list) {
                if (notebook.getTitle() != null && notebook.getTitle().toLowerCase().contains(lowerQuery)) {
                    filtered.add(notebook);
                }
            }
            list = filtered; // 新实例
        }
        
        // 应用排序（置顶的笔记本始终在前面）
        SortType sort = _sortType.getValue();
        if (sort == null) {
            sort = SortType.UPDATED_DESC;
        }
        
        // 如果是默认的更新时间降序排序，直接使用数据库的排序结果
        // 数据库已经按照 ORDER BY is_pinned DESC, updated_at DESC, id ASC 排序
        if (sort == SortType.UPDATED_DESC) {
            return new ArrayList<>(list);
        }
        
        // 如果是更新时间升序排序，反转数据库的降序结果
        if (sort == SortType.UPDATED_ASC) {
            List<Notebook> result = new ArrayList<>(list);
            // 分别反转置顶和非置顶的部分
            List<Notebook> pinnedList = new ArrayList<>();
            List<Notebook> unpinnedList = new ArrayList<>();
            
            for (Notebook notebook : result) {
                if (notebook.isPinned()) {
                    pinnedList.add(notebook);
                } else {
                    unpinnedList.add(notebook);
                }
            }
            
            // 反转各自的顺序
            Collections.reverse(pinnedList);
            Collections.reverse(unpinnedList);
            
            // 重新组合：置顶在前，非置顶在后
            List<Notebook> finalResult = new ArrayList<>();
            finalResult.addAll(pinnedList);
            finalResult.addAll(unpinnedList);
            
            return finalResult;
        }
        
        // 创建基础比较器：置顶优先
        Comparator<Notebook> baseComparator = (a, b) -> {
            if (a.isPinned() != b.isPinned()) {
                return Boolean.compare(b.isPinned(), a.isPinned()); // 置顶在前
            }
            return 0;
        };
        
        switch (sort) {
            case TITLE_ASC:
                list.sort(baseComparator
                    .thenComparing(n -> n.getTitle() == null ? "" : n.getTitle(),
                                   String.CASE_INSENSITIVE_ORDER)
                    .thenComparingLong(Notebook::getId)); // 稳定兜底
                break;
            case TITLE_DESC:
                list.sort(baseComparator
                    .thenComparing((Notebook n) -> n.getTitle() == null ? "" : n.getTitle(),
                                   String.CASE_INSENSITIVE_ORDER.reversed())
                    .thenComparingLong(Notebook::getId));
                break;
            case CREATED_ASC:
                list.sort(baseComparator
                    .thenComparingLong(Notebook::getCreatedAt)
                    .thenComparingLong(Notebook::getId));
                break;
            case CREATED_DESC:
                list.sort(baseComparator
                    .thenComparing(Comparator.comparingLong(Notebook::getCreatedAt).reversed())
                    .thenComparingLong(Notebook::getId));
                break;
            case UPDATED_DESC:
                // 直接使用数据库的默认排序 (ORDER BY is_pinned DESC, updated_at DESC, id ASC)
                // 不需要额外排序
                break;
        }
        
        // 再次返回"新实例"，避免外部误改
        return new ArrayList<>(list);
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
     * 清除筛选
     */
    public void clearFilters() {
        _searchQuery.setValue("");
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
        setRefreshing(true);
        
        notebookRepository.deleteNotebook(id, new NotebookRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                handleRepositoryCallback(result, null, "笔记本已删除");
                setRefreshing(false);   // ✅ 必须显式关
                refresh();              // （可选）触发一次 UI 刷新
            }
            
            @Override
            public void onError(Exception error) {
                handleRepositoryCallback(null, error);
                setRefreshing(false);   // ✅ 失败也要关
            }
        });
    }
    
    /**
     * 置顶笔记本
     */
    public void pinNotebook(long id) {
        setRefreshing(true);
        
        notebookRepository.pinNotebook(id, new NotebookRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                handleRepositoryCallback(result, null, "笔记本已置顶");
                setRefreshing(false);   // ✅
                refresh();              // ✅
            }
            
            @Override
            public void onError(Exception error) {
                handleRepositoryCallback(null, error);
                setRefreshing(false);   // ✅
            }
        });
    }
    
    /**
     * 取消置顶笔记本
     */
    public void unpinNotebook(long id) {
        setRefreshing(true);
        
        notebookRepository.unpinNotebook(id, new NotebookRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                handleRepositoryCallback(result, null, "已取消置顶");
                setRefreshing(false);   // ✅
                refresh();              // ✅
            }
            
            @Override
            public void onError(Exception error) {
                handleRepositoryCallback(null, error);
                setRefreshing(false);   // ✅
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
        return (query != null && !query.trim().isEmpty());
    }
    
    @Override
    protected void onInitialize() {
        Log.d(TAG, "MainViewModel initialized");
        // 初始化时不需要特殊操作，LiveData会自动加载数据
    }
    
    @Override
    protected void onRefresh() {
        Log.d(TAG, "Refreshing main data");
        
        // 由于getAllNotebooks返回LiveData，数据会自动更新
        // 这里只需要模拟刷新延迟然后停止刷新状态
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setRefreshing(false);
            Log.d(TAG, "Refresh completed");
        }, 1000); // 1秒后停止刷新动画
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