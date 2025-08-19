package com.example.note.ui.main;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.note.R;
import com.example.note.data.entity.Notebook;
import com.example.note.data.repository.NotebookRepository;
import com.example.note.ui.dialog.CreateNotebookDialog;
import com.example.note.ui.dialog.SortFilterDialog;
import com.example.note.ui.template.TemplateSelectionActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

/**
 * 主页Activity
 * 显示笔记本的瀑布流列表，支持搜索、排序、筛选和创建等功能
 */
public class MainActivity extends AppCompatActivity implements CreateNotebookDialog.OnNotebookNameListener {
    
    private static final String TAG = "MainActivity";
    private static final int SEARCH_DELAY_MS = 300; // 搜索节流延迟
    
    // UI组件
    private Toolbar toolbar;
    private EditText searchEditText;
    private ImageView searchClearButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private View emptyView;
    private View progressBar;
    private FloatingActionButton fab;
    
    // ViewModel和适配器
    private MainViewModel viewModel;
    private NotebookAdapter adapter;
    
    // 搜索节流处理
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initViewModel();
        initRecyclerView();
        initSearchView();
        setupObservers();
        setupListeners();
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        searchEditText = findViewById(R.id.search_edit_text);
        searchClearButton = findViewById(R.id.search_clear_button);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        recyclerView = findViewById(R.id.recycler_view);
        // emptyView已在上面定义
        emptyView = findViewById(R.id.empty_view);
        progressBar = findViewById(R.id.progress_bar);
        fab = findViewById(R.id.fab_add);
        
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }
    
    /**
     * 初始化ViewModel
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
    }
    
    /**
     * 初始化RecyclerView
     */
    private void initRecyclerView() {
        // 创建适配器
        adapter = new NotebookAdapter();
        
        // 设置点击监听器
        adapter.setOnItemClickListener(this::openNotebook);
        adapter.setOnItemLongClickListener(this::showNotebookMenu);
        
        // 设置布局管理器（2列瀑布流）
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(
            2, // 固定2列
            StaggeredGridLayoutManager.VERTICAL
        );
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false); // 瀑布流需要动态高度
    }
    
    /**
     * 初始化搜索视图
     */
    private void initSearchView() {
        searchEditText.setHint(R.string.search_hint);
        
        // 搜索文本变化监听（带节流）
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 取消之前的搜索任务
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                // 创建新的搜索任务
                searchRunnable = () -> viewModel.searchNotebooks(s.toString());
                
                // 延迟执行搜索
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                
                // 显示/隐藏清除按钮
                searchClearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // 清除按钮点击监听
        searchClearButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchClearButton.setVisibility(View.GONE);
        });
    }
    
    /**
     * 设置数据观察者
     */
    private void setupObservers() {
        // 观察笔记本列表
        viewModel.notebooks.observe(this, notebooks -> {
            if (notebooks != null) {
                adapter.submitList(notebooks);
                updateEmptyState(notebooks.isEmpty());
            }
        });
        
        // 观察加载状态
        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
        
        // 观察刷新状态
        viewModel.isRefreshing.observe(this, isRefreshing -> {
            if (isRefreshing != null) {
                swipeRefreshLayout.setRefreshing(isRefreshing);
            }
        });
        
        // 观察布局模式变化
        viewModel.layoutMode.observe(this, layoutMode -> {
            if (layoutMode != null) {
                updateLayoutMode(layoutMode);
            }
        });
    }
    
    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.onRefresh());
        
        // 搜索清除按钮
        searchClearButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchEditText.clearFocus();
        });
        
        // 浮动操作按钮
        fab.setOnClickListener(v -> showCreateNotebookDialog());
    }
    
    /**
     * 更新空状态显示
     */
    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 更新布局模式
     */
    private void updateLayoutMode(MainViewModel.LayoutMode layoutMode) {
        StaggeredGridLayoutManager layoutManager;
        
        switch (layoutMode) {
            case GRID:
                layoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
                break;
            case LIST:
                layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
                break;
            case WATERFALL:
            default:
                layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
                break;
        }
        
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(layoutManager);
    }
    
    /**
     * 打开笔记本
     */
    private void openNotebook(long notebookId) {
        Intent intent = new Intent(this, com.example.note.ui.note.NoteActivity.class);
        intent.putExtra(com.example.note.ui.note.NoteActivity.EXTRA_NOTEBOOK_ID, notebookId);
        intent.putExtra(com.example.note.ui.note.NoteActivity.EXTRA_IS_NEW, false);
        startActivity(intent);
    }
    
    /**
     * 显示笔记本菜单
     */
    private void showNotebookMenu(Notebook notebook) {
        // TODO: 实现笔记本长按菜单
        Snackbar.make(recyclerView, "长按笔记本: " + notebook.getTitle(), Snackbar.LENGTH_SHORT).show();
    }
    
    /**
     * 显示创建笔记本对话框
     */
    private void showCreateNotebookDialog() {
        CreateNotebookDialog dialog = CreateNotebookDialog.newInstance();
        dialog.setOnNotebookNameListener(this);
        dialog.show(getSupportFragmentManager(), "CreateNotebookDialog");
    }
    
    @Override
    public void onNotebookNameConfirmed(String name) {
        // 检查名称是否已存在
        viewModel.checkNotebookNameExists(name, new NotebookRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean exists) {
                if (exists) {
                    Snackbar.make(recyclerView, "笔记名称已存在，请使用其他名称", Snackbar.LENGTH_LONG).show();
                } else {
                    // 跳转到模板选择界面
                    Intent intent = new Intent(MainActivity.this, TemplateSelectionActivity.class);
                    intent.putExtra(TemplateSelectionActivity.EXTRA_NOTEBOOK_NAME, name);
                    startActivity(intent);
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Failed to check notebook name", error);
                Snackbar.make(recyclerView, "检查笔记名称时出错", Snackbar.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * 显示排序/筛选对话框
     */
    private void showSortFilterDialog() {
        SortFilterDialog dialog = SortFilterDialog.newInstance(
                viewModel.getCurrentSortType(),
                viewModel.getCurrentFilterColor()
        );
        
        dialog.setOnSortFilterListener(new SortFilterDialog.OnSortFilterListener() {
            @Override
            public void onSortFilterApplied(MainViewModel.SortType sortType, String filterColor) {
                viewModel.setSortType(sortType);
                viewModel.setFilterColor(filterColor);
                
                String message = "已应用排序: " + sortType.getDisplayName();
                if (filterColor != null) {
                    message += "，颜色筛选已启用";
                }
                Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show();
            }
            
            @Override
            public void onFiltersCleared() {
                viewModel.clearFilters();
                Snackbar.make(recyclerView, "已清除所有筛选条件", Snackbar.LENGTH_SHORT).show();
            }
        });
        
        dialog.show(getSupportFragmentManager(), "SortFilterDialog");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_sort) {
            showSortFilterDialog();
            return true;
        } else if (id == R.id.action_filter) {
            showSortFilterDialog();
            return true;
        } else if (id == R.id.action_layout) {
            viewModel.setLayoutMode(getNextLayoutMode());
            return true;
        } else if (id == R.id.action_settings) {
            // TODO: 打开设置页面
            Snackbar.make(recyclerView, "打开设置", Snackbar.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 获取下一个布局模式
     */
    private MainViewModel.LayoutMode getNextLayoutMode() {
        MainViewModel.LayoutMode current = viewModel.getCurrentLayoutMode();
        switch (current) {
            case WATERFALL:
                return MainViewModel.LayoutMode.GRID;
            case GRID:
                return MainViewModel.LayoutMode.LIST;
            case LIST:
            default:
                return MainViewModel.LayoutMode.WATERFALL;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理搜索处理器
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}