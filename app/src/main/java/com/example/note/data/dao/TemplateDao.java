package com.example.note.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.note.data.entity.Template;

import java.util.List;

/**
 * 模板数据访问对象
 */
@Dao
public interface TemplateDao {
    
    /**
     * 插入模板
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Template template);
    
    /**
     * 批量插入模板
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Template> templates);
    
    /**
     * 更新模板
     */
    @Update
    int update(Template template);
    
    /**
     * 删除模板
     */
    @Delete
    int delete(Template template);
    
    /**
     * 根据ID获取模板
     */
    @Query("SELECT * FROM templates WHERE id = :id")
    LiveData<Template> getById(long id);
    
    /**
     * 根据ID获取模板（同步）
     */
    @Query("SELECT * FROM templates WHERE id = :id")
    Template getByIdSync(long id);
    
    /**
     * 获取所有模板（按创建时间倒序）
     */
    @Query("SELECT * FROM templates ORDER BY is_system DESC, created_at DESC")
    LiveData<List<Template>> getAllTemplates();
    
    /**
     * 获取所有模板（同步，按创建时间倒序）
     */
    @Query("SELECT * FROM templates ORDER BY is_system DESC, created_at DESC")
    List<Template> getAllTemplatesSync();
    
    /**
     * 获取系统预置模板
     */
    @Query("SELECT * FROM templates WHERE is_system = 1 ORDER BY created_at DESC")
    LiveData<List<Template>> getSystemTemplates();
    
    /**
     * 获取用户自定义模板
     */
    @Query("SELECT * FROM templates WHERE is_system = 0 ORDER BY created_at DESC")
    LiveData<List<Template>> getUserTemplates();
    
    /**
     * 搜索模板（按名称和描述）
     */
    @Query("SELECT * FROM templates WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY is_system DESC, created_at DESC")
    LiveData<List<Template>> searchTemplates(String query);
    
    /**
     * 根据尺寸筛选模板
     */
    @Query("SELECT * FROM templates WHERE rows = :rows AND cols = :cols ORDER BY is_system DESC, created_at DESC")
    LiveData<List<Template>> getTemplatesBySize(int rows, int cols);
    
    /**
     * 获取最近使用的模板
     */
    @Query("SELECT * FROM templates ORDER BY created_at DESC LIMIT :limit")
    LiveData<List<Template>> getRecentTemplates(int limit);
    
    /**
     * 根据名称获取模板
     */
    @Query("SELECT * FROM templates WHERE name = :name")
    Template getByName(String name);
    
    /**
     * 检查模板名称是否已存在
     */
    @Query("SELECT COUNT(*) FROM templates WHERE name = :name AND id != :excludeId")
    int countByName(String name, long excludeId);
    
    /**
     * 删除用户自定义模板
     */
    @Query("DELETE FROM templates WHERE is_system = 0")
    int deleteUserTemplates();
    
    /**
     * 获取模板总数
     */
    @Query("SELECT COUNT(*) FROM templates")
    LiveData<Integer> getTemplateCount();
    
    /**
     * 获取系统模板总数
     */
    @Query("SELECT COUNT(*) FROM templates WHERE is_system = 1")
    LiveData<Integer> getSystemTemplateCount();
    
    /**
     * 获取用户模板总数
     */
    @Query("SELECT COUNT(*) FROM templates WHERE is_system = 0")
    LiveData<Integer> getUserTemplateCount();
    
    /**
     * 获取所有使用的尺寸组合
     */
    @Query("SELECT DISTINCT (rows || 'x' || cols) as size FROM templates ORDER BY rows, cols")
    List<String> getUsedSizes();
}