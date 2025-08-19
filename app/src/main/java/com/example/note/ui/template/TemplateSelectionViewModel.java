package com.example.note.ui.template;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.note.data.entity.Template;
import com.example.note.data.repository.TemplateRepository;

import java.util.List;

/**
 * 模板选择ViewModel
 * 处理模板选择界面的业务逻辑
 */
public class TemplateSelectionViewModel extends AndroidViewModel {
    
    private final TemplateRepository templateRepository;
    
    // 搜索查询
    private final MutableLiveData<String> _searchQuery = new MutableLiveData<>("");
    
    // 筛选类型（""=全部, "system"=系统模板, "user"=用户模板）
    private final MutableLiveData<String> _filter = new MutableLiveData<>("");
    
    // 加载状态
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    
    // 模板列表
    private final MediatorLiveData<List<Template>> _templates = new MediatorLiveData<>();
    
    // 原始数据源
    private LiveData<List<Template>> allTemplates;
    private LiveData<List<Template>> systemTemplates;
    private LiveData<List<Template>> userTemplates;
    
    public TemplateSelectionViewModel(@NonNull Application application) {
        super(application);
        templateRepository = TemplateRepository.getInstance(application);
        
        // 初始化数据源
        allTemplates = templateRepository.getAllTemplates();
        systemTemplates = templateRepository.getSystemTemplates();
        userTemplates = templateRepository.getUserTemplates();
        
        setupTemplatesLiveData();
    }
    
    private void setupTemplatesLiveData() {
        // 添加数据源
        _templates.addSource(allTemplates, templates -> updateTemplates());
        _templates.addSource(systemTemplates, templates -> updateTemplates());
        _templates.addSource(userTemplates, templates -> updateTemplates());
        _templates.addSource(_searchQuery, query -> updateTemplates());
        _templates.addSource(_filter, filter -> updateTemplates());
    }
    
    private void updateTemplates() {
        String query = _searchQuery.getValue();
        String filter = _filter.getValue();
        
        List<Template> sourceTemplates;
        
        // 根据筛选条件选择数据源
        if ("system".equals(filter)) {
            sourceTemplates = systemTemplates.getValue();
        } else if ("user".equals(filter)) {
            sourceTemplates = userTemplates.getValue();
        } else {
            sourceTemplates = allTemplates.getValue();
        }
        
        if (sourceTemplates == null) {
            _templates.setValue(null);
            return;
        }
        
        // 如果有搜索查询，进行筛选
        if (query != null && !query.trim().isEmpty()) {
            List<Template> filteredTemplates = sourceTemplates.stream()
                    .filter(template -> {
                        String lowerQuery = query.toLowerCase();
                        return template.getName().toLowerCase().contains(lowerQuery) ||
                               (template.getDescription() != null && 
                                template.getDescription().toLowerCase().contains(lowerQuery));
                    })
                    .collect(java.util.stream.Collectors.toList());
            _templates.setValue(filteredTemplates);
        } else {
            _templates.setValue(sourceTemplates);
        }
    }
    
    // Public getters
    public LiveData<List<Template>> getTemplates() {
        return _templates;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return _isLoading;
    }
    
    // Public methods
    public void searchTemplates(String query) {
        _searchQuery.setValue(query);
    }
    
    public void setFilter(String filter) {
        _filter.setValue(filter);
    }
    
    public void refresh() {
        _isLoading.setValue(true);
        // 刷新数据（重新触发LiveData更新）
        updateTemplates();
        _isLoading.setValue(false);
    }
}