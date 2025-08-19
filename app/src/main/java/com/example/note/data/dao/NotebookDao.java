package com.example.note.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.note.data.entity.Notebook;

import java.util.List;

/**
 * 笔记本数据访问对象
 */
@Dao
public interface NotebookDao {
    
    /**
     * 插入笔记本
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Notebook notebook);
    
    /**
     * 批量插入笔记本
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Notebook> notebooks);
    
    /**
     * 更新笔记本
     */
    @Update
    int update(Notebook notebook);
    
    /**
     * 删除笔记本（物理删除）
     */
    @Delete
    int delete(Notebook notebook);
    
    /**
     * 根据ID获取笔记本
     */
    @Query("SELECT * FROM notebooks WHERE id = :id")
    LiveData<Notebook> getById(long id);
    
    /**
     * 根据ID获取笔记本（同步）
     */
    @Query("SELECT * FROM notebooks WHERE id = :id")
    Notebook getByIdSync(long id);
    
    /**
     * 获取所有未删除的笔记本（按更新时间倒序）
     */
    @Query("SELECT * FROM notebooks WHERE is_deleted = 0 ORDER BY updated_at DESC")
    LiveData<List<Notebook>> getAllNotebooks();
    
    /**
     * 获取所有未删除的笔记本（同步，按更新时间倒序）
     */
    @Query("SELECT * FROM notebooks WHERE is_deleted = 0 ORDER BY updated_at DESC")
    List<Notebook> getAllNotebooksSync();
    
    /**
     * 获取回收站中的笔记本（按删除时间倒序）
     */
    @Query("SELECT * FROM notebooks WHERE is_deleted = 1 ORDER BY deleted_at DESC")
    LiveData<List<Notebook>> getDeletedNotebooks();
    
    /**
     * 搜索笔记本（按标题）
     */
    @Query("SELECT * FROM notebooks WHERE is_deleted = 0 AND title LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    LiveData<List<Notebook>> searchNotebooks(String query);
    
    /**
     * 根据颜色筛选笔记本
     */
    @Query("SELECT * FROM notebooks WHERE is_deleted = 0 AND color = :color ORDER BY updated_at DESC")
    LiveData<List<Notebook>> getNotebooksByColor(String color);
    
    /**
     * 获取最近创建的笔记本
     */
    @Query("SELECT * FROM notebooks WHERE is_deleted = 0 ORDER BY created_at DESC LIMIT :limit")
    LiveData<List<Notebook>> getRecentNotebooks(int limit);
    
    /**
     * 获取最近更新的笔记本
     */
    @Query("SELECT * FROM notebooks WHERE is_deleted = 0 ORDER BY updated_at DESC LIMIT :limit")
    LiveData<List<Notebook>> getRecentlyUpdatedNotebooks(int limit);
    
    /**
     * 软删除笔记本
     */
    @Query("UPDATE notebooks SET is_deleted = 1, deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id")
    int softDelete(long id, long deletedAt, long updatedAt);
    
    /**
     * 恢复删除的笔记本
     */
    @Query("UPDATE notebooks SET is_deleted = 0, deleted_at = NULL, updated_at = :updatedAt WHERE id = :id")
    int restore(long id, long updatedAt);
    
    /**
     * 物理删除回收站中的笔记本
     */
    @Query("DELETE FROM notebooks WHERE is_deleted = 1 AND deleted_at < :beforeTime")
    int deleteExpiredNotebooks(long beforeTime);
    
    /**
     * 更新笔记本标题
     */
    @Query("UPDATE notebooks SET title = :title, updated_at = :updatedAt WHERE id = :id")
    int updateTitle(long id, String title, long updatedAt);
    
    /**
     * 更新笔记本颜色
     */
    @Query("UPDATE notebooks SET color = :color, updated_at = :updatedAt WHERE id = :id")
    int updateColor(long id, String color, long updatedAt);
    
    /**
     * 更新笔记本的更新时间
     */
    @Query("UPDATE notebooks SET updated_at = :updatedAt WHERE id = :id")
    int touch(long id, long updatedAt);
    
    /**
     * 获取笔记本总数
     */
    @Query("SELECT COUNT(*) FROM notebooks WHERE is_deleted = 0")
    LiveData<Integer> getNotebookCount();
    
    /**
     * 获取回收站笔记本总数
     */
    @Query("SELECT COUNT(*) FROM notebooks WHERE is_deleted = 1")
    LiveData<Integer> getDeletedNotebookCount();
    
    /**
     * 检查标题是否已存在
     */
    @Query("SELECT COUNT(*) FROM notebooks WHERE is_deleted = 0 AND title = :title AND id != :excludeId")
    int countByTitle(String title, long excludeId);
    
    /**
     * 获取所有使用的颜色
     */
    @Query("SELECT DISTINCT color FROM notebooks WHERE is_deleted = 0 ORDER BY color")
    LiveData<List<String>> getUsedColors();
    
    /**
     * 按时间范围获取笔记本
     */
    @Query("SELECT * FROM notebooks WHERE is_deleted = 0 AND created_at BETWEEN :startTime AND :endTime ORDER BY created_at DESC")
    LiveData<List<Notebook>> getNotebooksByTimeRange(long startTime, long endTime);
    
    /**
     * 根据名称获取笔记本
     */
    @Query("SELECT * FROM notebooks WHERE is_deleted = 0 AND title = :name LIMIT 1")
    Notebook getNotebookByName(String name);
}