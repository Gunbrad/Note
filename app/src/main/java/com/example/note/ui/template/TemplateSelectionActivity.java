package com.example.note.ui.template;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;
import com.example.note.data.entity.Template;
import com.example.note.ui.note.NoteActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * 模板选择Activity
 * 用于在创建新笔记时选择模板
 */
public class TemplateSelectionActivity extends AppCompatActivity implements TemplateAdapter.OnTemplateClickListener {
    
    public static final String EXTRA_NOTEBOOK_NAME = "notebook_name";
    public static final String EXTRA_SELECTED_TEMPLATE_ID = "selected_template_id";
    
    private MaterialToolbar toolbar;
    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private ChipGroup filterChipGroup;
    private RecyclerView templatesRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FloatingActionButton createBlankFab;
    
    private TemplateSelectionViewModel viewModel;
    private TemplateAdapter templateAdapter;
    
    private String notebookName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_selection);
        
        // 获取传入的笔记名称
        notebookName = getIntent().getStringExtra(EXTRA_NOTEBOOK_NAME);
        if (notebookName == null || notebookName.trim().isEmpty()) {
            finish();
            return;
        }
        
        initViews();
        initViewModel();
        setupRecyclerView();
        setupSearchView();
        setupFilterChips();
        setupObservers();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        searchEditText = findViewById(R.id.search_edit_text);
        clearSearchButton = findViewById(R.id.clear_search_button);
        filterChipGroup = findViewById(R.id.filter_chip_group);
        templatesRecyclerView = findViewById(R.id.templates_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.empty_view);
        createBlankFab = findViewById(R.id.create_blank_fab);
        
        // 设置工具栏
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("选择模板");
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // 设置创建空白笔记按钮
        createBlankFab.setOnClickListener(v -> createBlankNotebook());
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(TemplateSelectionViewModel.class);
    }
    
    private void setupRecyclerView() {
        templateAdapter = new TemplateAdapter(this);
        templatesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        templatesRecyclerView.setAdapter(templateAdapter);
    }
    
    private void setupSearchView() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                viewModel.searchTemplates(query);
                clearSearchButton.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSearchButton.setVisibility(View.GONE);
        });
    }
    
    private void setupFilterChips() {
        // 添加筛选芯片
        addFilterChip("全部", "");
        addFilterChip("系统模板", "system");
        addFilterChip("我的模板", "user");
        
        // 默认选中"全部"
        ((Chip) filterChipGroup.getChildAt(0)).setChecked(true);
    }
    
    private void addFilterChip(String text, String filter) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 取消其他芯片的选中状态
                for (int i = 0; i < filterChipGroup.getChildCount(); i++) {
                    Chip otherChip = (Chip) filterChipGroup.getChildAt(i);
                    if (otherChip != chip) {
                        otherChip.setChecked(false);
                    }
                }
                viewModel.setFilter(filter);
            }
        });
        filterChipGroup.addView(chip);
    }
    
    private void setupObservers() {
        // 观察模板列表
        viewModel.getTemplates().observe(this, templates -> {
            templateAdapter.submitList(templates);
            updateEmptyView(templates);
        });
        
        // 观察加载状态
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }
    
    private void updateEmptyView(List<Template> templates) {
        if (templates == null || templates.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            templatesRecyclerView.setVisibility(View.GONE);
            emptyView.setText("暂无模板\n点击右下角按钮创建空白笔记");
        } else {
            emptyView.setVisibility(View.GONE);
            templatesRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onTemplateClick(Template template) {
        // 使用选中的模板创建笔记
        createNotebookWithTemplate(template);
    }
    
    private void createBlankNotebook() {
        // 创建空白笔记（5x5表格）
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra(NoteActivity.EXTRA_NOTEBOOK_NAME, notebookName);
        intent.putExtra(NoteActivity.EXTRA_ROWS, 5);
        intent.putExtra(NoteActivity.EXTRA_COLS, 5);
        intent.putExtra(NoteActivity.EXTRA_IS_NEW, true);
        startActivity(intent);
        finish();
    }
    
    private void createNotebookWithTemplate(Template template) {
        // 使用模板创建笔记
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra(NoteActivity.EXTRA_NOTEBOOK_NAME, notebookName);
        intent.putExtra(NoteActivity.EXTRA_TEMPLATE_ID, template.getId());
        intent.putExtra(NoteActivity.EXTRA_IS_NEW, true);
        startActivity(intent);
        finish();
    }
}