package com.example.note.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.note.data.dao.TemplateDao;
import com.example.note.data.database.AppDatabase;
import com.example.note.data.entity.Template;
import com.example.note.util.DateUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 模板仓库类
 * 提供模板数据的统一访问接口
 */
public class TemplateRepository {
    
    private static final String TAG = "TemplateRepository";
    private static volatile TemplateRepository INSTANCE;
    
    private final TemplateDao templateDao;
    private final Executor executor;
    
    // LiveData缓存
    private final LiveData<List<Template>> allTemplates;
    private final LiveData<List<Template>> systemTemplates;
    private final LiveData<List<Template>> userTemplates;
    private final LiveData<Integer> templateCount;
    
    private TemplateRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        templateDao = database.templateDao();
        executor = Executors.newFixedThreadPool(4);
        
        // 初始化LiveData
        allTemplates = templateDao.getAllTemplates();
        systemTemplates = templateDao.getSystemTemplates();
        userTemplates = templateDao.getUserTemplates();
        templateCount = templateDao.getTemplateCount();
    }
    
    /**
     * 获取Repository实例（单例模式）
     */
    public static TemplateRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TemplateRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TemplateRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 获取所有模板
     */
    public LiveData<List<Template>> getAllTemplates() {
        return allTemplates;
    }
    
    /**
     * 获取系统模板
     */
    public LiveData<List<Template>> getSystemTemplates() {
        return systemTemplates;
    }
    
    /**
     * 获取用户自定义模板
     */
    public LiveData<List<Template>> getUserTemplates() {
        return userTemplates;
    }
    
    /**
     * 获取模板总数
     */
    public LiveData<Integer> getTemplateCount() {
        return templateCount;
    }
    
    /**
     * 根据ID获取模板
     */
    public LiveData<Template> getTemplateById(long id) {
        return templateDao.getById(id);
    }
    
    /**
     * 搜索模板
     */
    public LiveData<List<Template>> searchTemplates(String query) {
        if (query == null || query.trim().isEmpty()) {
            return allTemplates;
        }
        return templateDao.searchTemplates(query.trim());
    }
    
    /**
     * 根据尺寸获取模板
     */
    public LiveData<List<Template>> getTemplatesBySize(int rows, int cols) {
        return templateDao.getTemplatesBySize(rows, cols);
    }
    
    /**
     * 获取最近使用的模板
     */
    public LiveData<List<Template>> getRecentTemplates(int limit) {
        return templateDao.getRecentTemplates(limit);
    }
    
    /**
     * 获取系统模板总数
     */
    public LiveData<Integer> getSystemTemplateCount() {
        return templateDao.getSystemTemplateCount();
    }
    
    /**
     * 获取用户模板总数
     */
    public LiveData<Integer> getUserTemplateCount() {
        return templateDao.getUserTemplateCount();
    }
    
    /**
     * 创建新模板
     */
    public void createTemplate(String name, String description, int rows, int cols, String data, RepositoryCallback<Long> callback) {
        executor.execute(() -> {
            try {
                // 验证参数
                if (name == null || name.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("模板名称不能为空"));
                    }
                    return;
                }
                
                if (rows <= 0 || cols <= 0) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("行数和列数必须大于0"));
                    }
                    return;
                }
                
                // 检查名称是否已存在
                if (templateDao.countByName(name.trim(), 0) > 0) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("模板名称已存在"));
                    }
                    return;
                }
                
                // 创建模板
                Template template = new Template();
                template.setName(name.trim());
                template.setDescription(description != null ? description.trim() : "");
                template.setRows(rows);
                template.setCols(cols);
                template.setData(data != null ? data : "");
                template.setSystem(false); // 用户创建的模板
                template.setCreatedAt(DateUtils.now());
                
                long id = templateDao.insert(template);
                
                if (callback != null) {
                    callback.onSuccess(id);
                }
                
                Log.d(TAG, "Template created: " + name + ", id: " + id);
            } catch (Exception e) {
                Log.e(TAG, "Failed to create template: " + name, e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新模板
     */
    public void updateTemplate(Template template, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                // 系统模板不允许修改
                if (template.isSystem()) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("系统模板不允许修改"));
                    }
                    return;
                }
                
                int result = templateDao.update(template);
                
                if (result > 0) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    Log.d(TAG, "Template updated: " + template.getId());
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("更新失败"));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update template: " + template.getId(), e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新模板名称
     */
    public void updateTemplateName(long id, String name, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                // 验证参数
                if (name == null || name.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("模板名称不能为空"));
                    }
                    return;
                }
                
                // 检查是否为系统模板
                Template template = templateDao.getByIdSync(id);
                if (template == null) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("模板不存在"));
                    }
                    return;
                }
                
                if (template.isSystem()) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("系统模板不允许修改"));
                    }
                    return;
                }
                
                // 检查名称是否已存在
                if (templateDao.countByName(name.trim(), id) > 0) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("模板名称已存在"));
                    }
                    return;
                }
                
                template.setName(name.trim());
                int result = templateDao.update(template);
                
                if (result > 0) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    Log.d(TAG, "Template name updated: " + id);
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("更新失败"));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update template name: " + id, e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 删除模板
     */
    public void deleteTemplate(long id, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                Template template = templateDao.getByIdSync(id);
                if (template == null) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("模板不存在"));
                    }
                    return;
                }
                
                // 系统模板不允许删除
                if (template.isSystem()) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("系统模板不允许删除"));
                    }
                    return;
                }
                
                int result = templateDao.delete(template);
                
                if (result > 0) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    Log.d(TAG, "Template deleted: " + id);
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("删除失败"));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete template: " + id, e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 删除所有用户自定义模板
     */
    public void deleteAllUserTemplates(RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                int result = templateDao.deleteUserTemplates();
                
                if (callback != null) {
                    callback.onSuccess(result);
                }
                
                Log.d(TAG, "Deleted " + result + " user templates");
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete user templates", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 批量插入模板（主要用于系统模板）
     */
    public void insertTemplates(List<Template> templates, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                templateDao.insertAll(templates);
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Batch inserted " + templates.size() + " templates");
            } catch (Exception e) {
                Log.e(TAG, "Failed to batch insert templates", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 复制模板（基于现有模板创建新模板）
     */
    public void copyTemplate(long sourceId, String newName, String newDescription, RepositoryCallback<Long> callback) {
        executor.execute(() -> {
            try {
                Template sourceTemplate = templateDao.getByIdSync(sourceId);
                if (sourceTemplate == null) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("源模板不存在"));
                    }
                    return;
                }
                
                // 验证新名称
                if (newName == null || newName.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("模板名称不能为空"));
                    }
                    return;
                }
                
                // 检查名称是否已存在
                if (templateDao.countByName(newName.trim(), 0) > 0) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("模板名称已存在"));
                    }
                    return;
                }
                
                // 创建新模板
                Template newTemplate = new Template();
                newTemplate.setName(newName.trim());
                newTemplate.setDescription(newDescription != null ? newDescription.trim() : sourceTemplate.getDescription());
                newTemplate.setRows(sourceTemplate.getRows());
                newTemplate.setCols(sourceTemplate.getCols());
                newTemplate.setData(sourceTemplate.getData());
                newTemplate.setSystem(false); // 复制的模板都是用户模板
                newTemplate.setCreatedAt(DateUtils.now());
                
                long id = templateDao.insert(newTemplate);
                
                if (callback != null) {
                    callback.onSuccess(id);
                }
                
                Log.d(TAG, "Template copied: " + sourceId + " -> " + id);
            } catch (Exception e) {
                Log.e(TAG, "Failed to copy template: " + sourceId, e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 获取所有使用的尺寸组合
     */
    public void getAllUsedSizes(RepositoryCallback<List<String>> callback) {
        executor.execute(() -> {
            try {
                List<String> sizes = templateDao.getUsedSizes();
                
                if (callback != null) {
                    callback.onSuccess(sizes);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get all used sizes", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 检查模板名称是否存在
     */
    public void isTemplateNameExists(String name, long excludeId, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean exists = templateDao.countByName(name.trim(), excludeId) > 0;
                
                if (callback != null) {
                    callback.onSuccess(exists);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to check template name existence", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Repository回调接口
     */
    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
}