package com.example.note.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.note.data.entity.Row;

import java.util.List;

/**
 * 行数据访问对象
 * 提供行数据的CRUD操作
 */
@Dao
public interface RowDao {
    
    /**
     * 插入行
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertRow(Row row);
    
    /**
     * 批量插入行
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertRows(List<Row> rows);
    
    /**
     * 更新行
     */
    @Update
    int updateRow(Row row);
    
    /**
     * 批量更新行
     */
    @Update
    int updateRows(List<Row> rows);
    
    /**
     * 删除行
     */
    @Delete
    int deleteRow(Row row);
    
    /**
     * 根据ID删除行
     */
    @Query("DELETE FROM rows WHERE id = :rowId")
    int deleteRowById(long rowId);
    
    /**
     * 删除指定笔记本的所有行
     */
    @Query("DELETE FROM rows WHERE notebook_id = :notebookId")
    int deleteRowsByNotebookId(long notebookId);
    
    /**
     * 删除指定笔记本中指定行索引及之后的所有行
     */
    @Query("DELETE FROM rows WHERE notebook_id = :notebookId AND row_index >= :fromRowIndex")
    int deleteRowsFromIndex(long notebookId, int fromRowIndex);
    
    /**
     * 获取指定笔记本的所有行
     */
    @Query("SELECT * FROM rows WHERE notebook_id = :notebookId ORDER BY row_index ASC")
    LiveData<List<Row>> getRowsByNotebookId(long notebookId);
    
    /**
     * 同步获取指定笔记本的所有行
     */
    @Query("SELECT * FROM rows WHERE notebook_id = :notebookId ORDER BY row_index ASC")
    List<Row> getRowsByNotebookIdSync(long notebookId);
    
    /**
     * 获取指定笔记本中指定行索引的行
     */
    @Query("SELECT * FROM rows WHERE notebook_id = :notebookId AND row_index = :rowIndex LIMIT 1")
    Row getRowByIndex(long notebookId, int rowIndex);
    
    /**
     * 获取指定笔记本中指定行索引的行高
     */
    @Query("SELECT height_dp FROM rows WHERE notebook_id = :notebookId AND row_index = :rowIndex LIMIT 1")
    Float getRowHeight(long notebookId, int rowIndex);
    
    /**
     * 更新指定行的高度
     */
    @Query("UPDATE rows SET height_dp = :height, updated_at = :updatedAt WHERE notebook_id = :notebookId AND row_index = :rowIndex")
    int updateRowHeight(long notebookId, int rowIndex, float height, long updatedAt);
    
    /**
     * 获取指定笔记本的行数
     */
    @Query("SELECT COUNT(*) FROM rows WHERE notebook_id = :notebookId")
    int getRowCount(long notebookId);
    
    /**
     * 更新指定行索引之后的所有行的索引（用于插入行时调整索引）
     */
    @Query("UPDATE rows SET row_index = row_index + 1, updated_at = :updatedAt WHERE notebook_id = :notebookId AND row_index >= :fromIndex")
    int incrementRowIndicesFrom(long notebookId, int fromIndex, long updatedAt);
    
    /**
     * 更新指定行索引之后的所有行的索引（用于删除行时调整索引）
     */
    @Query("UPDATE rows SET row_index = row_index - 1, updated_at = :updatedAt WHERE notebook_id = :notebookId AND row_index > :deletedIndex")
    int decrementRowIndicesAfter(long notebookId, int deletedIndex, long updatedAt);
}