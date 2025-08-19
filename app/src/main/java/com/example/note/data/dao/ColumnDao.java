package com.example.note.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.note.data.entity.Column;

import java.util.List;

/**
 * 列数据访问对象
 */
@Dao
public interface ColumnDao {
    
    /**
     * 插入列
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Column column);
    
    /**
     * 批量插入列
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Column> columns);
    
    /**
     * 更新列
     */
    @Update
    int update(Column column);
    
    /**
     * 删除列
     */
    @Delete
    int delete(Column column);
    
    /**
     * 根据ID获取列
     */
    @Query("SELECT * FROM columns WHERE id = :id")
    LiveData<Column> getById(long id);
    
    /**
     * 根据ID获取列（同步）
     */
    @Query("SELECT * FROM columns WHERE id = :id")
    Column getByIdSync(long id);
    
    /**
     * 根据笔记本ID获取所有列
     */
    @Query("SELECT * FROM columns WHERE notebook_id = :notebookId ORDER BY column_index")
    LiveData<List<Column>> getColumnsByNotebookId(long notebookId);
    
    /**
     * 根据笔记本ID获取所有列（同步）
     */
    @Query("SELECT * FROM columns WHERE notebook_id = :notebookId ORDER BY column_index")
    List<Column> getColumnsByNotebookIdSync(long notebookId);
    
    /**
     * 根据位置获取列
     */
    @Query("SELECT * FROM columns WHERE notebook_id = :notebookId AND column_index = :columnIndex")
    LiveData<Column> getColumnByPosition(long notebookId, int columnIndex);
    
    /**
     * 根据位置获取列（同步）
     */
    @Query("SELECT * FROM columns WHERE notebook_id = :notebookId AND column_index = :columnIndex")
    Column getColumnByPositionSync(long notebookId, int columnIndex);
    
    /**
     * 获取可见列
     */
    @Query("SELECT * FROM columns WHERE notebook_id = :notebookId AND is_visible = 1 ORDER BY column_index")
    LiveData<List<Column>> getVisibleColumns(long notebookId);
    
    /**
     * 获取冻结列
     */
    @Query("SELECT * FROM columns WHERE notebook_id = :notebookId AND is_frozen = 1 ORDER BY column_index")
    LiveData<List<Column>> getFrozenColumns(long notebookId);
    
    /**
     * 删除笔记本的所有列
     */
    @Query("DELETE FROM columns WHERE notebook_id = :notebookId")
    int deleteColumnsByNotebookId(long notebookId);
    
    /**
     * 删除指定列
     */
    @Query("DELETE FROM columns WHERE notebook_id = :notebookId AND column_index = :columnIndex")
    int deleteColumnByPosition(long notebookId, int columnIndex);
    
    /**
     * 获取笔记本的最大列索引
     */
    @Query("SELECT COALESCE(MAX(column_index), -1) FROM columns WHERE notebook_id = :notebookId")
    int getMaxColumnIndex(long notebookId);
    
    /**
     * 获取列总数
     */
    @Query("SELECT COUNT(*) FROM columns WHERE notebook_id = :notebookId")
    LiveData<Integer> getColumnCount(long notebookId);
    
    /**
     * 插入列后调整索引（将指定列之后的列索引+1）
     */
    @Query("UPDATE columns SET column_index = column_index + 1, updated_at = :updatedAt WHERE notebook_id = :notebookId AND column_index >= :insertColumnIndex")
    int insertColumn(long notebookId, int insertColumnIndex, long updatedAt);
    
    /**
     * 删除列后调整索引（将指定列之后的列索引-1）
     */
    @Query("UPDATE columns SET column_index = column_index - 1, updated_at = :updatedAt WHERE notebook_id = :notebookId AND column_index > :deletedColumnIndex")
    int adjustColumnIndexAfterDelete(long notebookId, int deletedColumnIndex, long updatedAt);
    
    /**
     * 移动列
     */
    @Query("UPDATE columns SET column_index = CASE " +
           "WHEN column_index = :fromIndex THEN :toIndex " +
           "WHEN :fromIndex < :toIndex AND column_index > :fromIndex AND column_index <= :toIndex THEN column_index - 1 " +
           "WHEN :fromIndex > :toIndex AND column_index >= :toIndex AND column_index < :fromIndex THEN column_index + 1 " +
           "ELSE column_index END, " +
           "updated_at = :updatedAt " +
           "WHERE notebook_id = :notebookId")
    int moveColumn(long notebookId, int fromIndex, int toIndex, long updatedAt);
    
    /**
     * 更新列宽度
     */
    @Query("UPDATE columns SET width = :width, updated_at = :updatedAt WHERE id = :id")
    int updateWidth(long id, float width, long updatedAt);
    
    /**
     * 更新列名称
     */
    @Query("UPDATE columns SET name = :name, updated_at = :updatedAt WHERE id = :id")
    int updateName(long id, String name, long updatedAt);
    
    /**
     * 更新列可见性
     */
    @Query("UPDATE columns SET is_visible = :isVisible, updated_at = :updatedAt WHERE id = :id")
    int updateVisibility(long id, boolean isVisible, long updatedAt);
    
    /**
     * 更新列排序
     */
    @Query("UPDATE columns SET sort_order = :sortOrder, updated_at = :updatedAt WHERE id = :id")
    int updateSortOrder(long id, String sortOrder, long updatedAt);
    
    /**
     * 更新列筛选
     */
    @Query("UPDATE columns SET filter_value = :filterValue, updated_at = :updatedAt WHERE id = :id")
    int updateFilterValue(long id, String filterValue, long updatedAt);
}