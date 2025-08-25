package com.example.note.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.note.data.entity.Cell;

import java.util.List;

/**
 * 单元格数据访问对象
 */
@Dao
public interface CellDao {
    
    /**
     * 插入单元格
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Cell cell);
    
    /**
     * 批量插入单元格
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Cell> cells);
    
    /**
     * 使用复合键UPSERT单个单元格（兼容版本）
     */
    @Query("INSERT OR REPLACE INTO cells (" +
           "  id, notebook_id, row_index, col_index, " +
           "  content, text_color, background_color, " +
           "  is_bold, is_italic, text_size, text_alignment, image_id, " +
           "  updated_at, created_at" +
           ") VALUES (" +
           "  (SELECT id FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex)," +
           "  :notebookId, :rowIndex, :colIndex," +
           "  :content, :textColor, :backgroundColor, :isBold, :isItalic, :textSize, :textAlignment, :imageId," +
           "  :updatedAt," +
           "  COALESCE((SELECT created_at FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex), :createdAt)" +
           ")")
    void upsertCell(long notebookId, int rowIndex, int colIndex, String content, String textColor, String backgroundColor, boolean isBold, boolean isItalic, float textSize, String textAlignment, String imageId, long updatedAt, long createdAt);
    
    /**
     * 使用复合键UPSERT单元格内容（兼容版本）
     */
    @Query("INSERT OR REPLACE INTO cells (" +
           "  id, notebook_id, row_index, col_index, " +
           "  content, text_color, background_color, " +
           "  is_bold, is_italic, text_size, text_alignment, image_id, " +
           "  updated_at, created_at" +
           ") VALUES (" +
           "  (SELECT id FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex)," +
           "  :notebookId, :rowIndex, :colIndex," +
           "  :content," +
           "  COALESCE((SELECT text_color       FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex), '')," +
           "  COALESCE((SELECT background_color FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex), '')," +
           "  COALESCE((SELECT is_bold          FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex), 0)," +
           "  COALESCE((SELECT is_italic        FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex), 0)," +
           "  COALESCE((SELECT text_size        FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex), 14)," +
           "  COALESCE((SELECT text_alignment   FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex), 'LEFT')," +
           "  COALESCE((SELECT image_id         FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex), NULL)," +
           "  :updatedAt," +
           "  COALESCE((SELECT created_at       FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex), :createdAt)" +
           ")")
    void upsertCellContent(long notebookId, int rowIndex, int colIndex, String content, long updatedAt, long createdAt);
    
    /**
     * 使用复合键UPSERT单元格格式（兼容版本）
     */
    @Query("INSERT OR REPLACE INTO cells (" +
           "  id, notebook_id, row_index, col_index, " +
           "  content, text_color, background_color, " +
           "  is_bold, is_italic, text_size, text_alignment, image_id, " +
           "  updated_at, created_at" +
           ") VALUES (" +
           "  (SELECT id FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex)," +
           "  :notebookId, :rowIndex, :colIndex," +
           "  COALESCE((SELECT content          FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex), '')," +
           "  :textColor, :backgroundColor, :isBold, :isItalic, :textSize, :textAlignment," +
           "  COALESCE((SELECT image_id         FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex), NULL)," +
           "  :updatedAt," +
           "  COALESCE((SELECT created_at       FROM cells WHERE notebook_id=:notebookId AND row_index=:rowIndex AND col_index=:colIndex), :createdAt)" +
           ")")
    void upsertCellFormat(long notebookId, int rowIndex, int colIndex, String textColor, String backgroundColor, boolean isBold, boolean isItalic, float textSize, String textAlignment, long updatedAt, long createdAt);
    
    /**
     * 更新单元格
     */
    @Update
    int update(Cell cell);
    
    /**
     * 删除单元格
     */
    @Delete
    int delete(Cell cell);
    
    /**
     * 根据ID获取单元格
     */
    @Query("SELECT * FROM cells WHERE id = :id")
    LiveData<Cell> getById(long id);
    
    /**
     * 根据ID获取单元格（同步）
     */
    @Query("SELECT * FROM cells WHERE id = :id")
    Cell getByIdSync(long id);
    
    /**
     * 根据笔记本ID获取所有单元格
     */
    @Query("SELECT * FROM cells WHERE notebook_id = :notebookId ORDER BY row_index, col_index")
    LiveData<List<Cell>> getCellsByNotebookId(long notebookId);
    
    /**
     * 根据笔记本ID获取所有单元格（同步）
     */
    @Query("SELECT * FROM cells WHERE notebook_id = :notebookId ORDER BY row_index, col_index")
    List<Cell> getCellsByNotebookIdSync(long notebookId);
    
    /**
     * 根据位置获取单元格
     */
    @Query("SELECT * FROM cells WHERE notebook_id = :notebookId AND row_index = :rowIndex AND col_index = :colIndex")
    LiveData<Cell> getCellByPosition(long notebookId, int rowIndex, int colIndex);
    
    /**
     * 根据位置获取单元格（同步）
     */
    @Query("SELECT * FROM cells WHERE notebook_id = :notebookId AND row_index = :rowIndex AND col_index = :colIndex")
    Cell getCellByPositionSync(long notebookId, int rowIndex, int colIndex);
    
    /**
     * 获取指定行的所有单元格
     */
    @Query("SELECT * FROM cells WHERE notebook_id = :notebookId AND row_index = :rowIndex ORDER BY col_index")
    LiveData<List<Cell>> getCellsByRow(long notebookId, int rowIndex);
    
    /**
     * 获取指定列的所有单元格
     */
    @Query("SELECT * FROM cells WHERE notebook_id = :notebookId AND col_index = :colIndex ORDER BY row_index")
    LiveData<List<Cell>> getCellsByColumn(long notebookId, int colIndex);
    
    /**
     * 获取指定区域的单元格
     */
    @Query("SELECT * FROM cells WHERE notebook_id = :notebookId AND row_index BETWEEN :startRow AND :endRow AND col_index BETWEEN :startCol AND :endCol ORDER BY row_index, col_index")
    LiveData<List<Cell>> getCellsByRange(long notebookId, int startRow, int endRow, int startCol, int endCol);
    
    /**
     * 搜索单元格内容
     */
    @Query("SELECT * FROM cells WHERE notebook_id = :notebookId AND content LIKE '%' || :query || '%' ORDER BY row_index, col_index")
    LiveData<List<Cell>> searchCells(long notebookId, String query);
    
    /**
     * 获取包含图片的单元格
     */
    @Query("SELECT * FROM cells WHERE notebook_id = :notebookId AND image_id IS NOT NULL AND image_id != '' ORDER BY row_index, col_index")
    LiveData<List<Cell>> getCellsWithImages(long notebookId);
    
    /**
     * 获取非空单元格
     */
    @Query("SELECT * FROM cells WHERE notebook_id = :notebookId AND (content IS NOT NULL AND content != '' OR image_id IS NOT NULL AND image_id != '') ORDER BY row_index, col_index")
    LiveData<List<Cell>> getNonEmptyCells(long notebookId);
    
    /**
     * 根据列筛选和排序获取单元格
     * @param notebookId 笔记本ID
     * @param colIndex 列索引
     * @param filterType 筛选类型：text_contains, number_range, date_range, boolean
     * @param filterValue 筛选值
     * @param sortOrder 排序方式：ASC, DESC, 或空字符串表示默认排序
     */
    @Query("SELECT * FROM cells WHERE notebook_id = :notebookId AND col_index = :colIndex " +
           "AND (:filterType = '' OR " +
           "(:filterType = 'text_contains' AND content LIKE '%' || :filterValue || '%') OR " +
           "(:filterType = 'number_range' AND CAST(content AS REAL) BETWEEN :minValue AND :maxValue) OR " +
           "(:filterType = 'boolean' AND content = :filterValue)) " +
           "ORDER BY CASE WHEN :sortOrder = 'ASC' THEN content END ASC, " +
           "CASE WHEN :sortOrder = 'DESC' THEN content END DESC, " +
           "CASE WHEN :sortOrder = '' THEN row_index END ASC")
    LiveData<List<Cell>> getCellsByColumnWithFilter(long notebookId, int colIndex, String filterType, 
                                                   String filterValue, double minValue, double maxValue, String sortOrder);
    
    /**
     * 获取指定列的所有单元格并排序
     */
    @Query("SELECT * FROM cells WHERE notebook_id = :notebookId AND col_index = :colIndex " +
           "ORDER BY CASE WHEN :sortOrder = 'ASC' THEN content END ASC, " +
           "CASE WHEN :sortOrder = 'DESC' THEN content END DESC, " +
           "CASE WHEN :sortOrder = '' THEN row_index END ASC")
    LiveData<List<Cell>> getCellsByColumnSorted(long notebookId, int colIndex, String sortOrder);
    
    /**
     * 获取所有单元格并按指定列排序
     */
    @Query("SELECT c.* FROM cells c " +
           "LEFT JOIN cells sort_col ON c.notebook_id = sort_col.notebook_id AND c.row_index = sort_col.row_index AND sort_col.col_index = :sortColIndex " +
           "WHERE c.notebook_id = :notebookId " +
           "ORDER BY CASE WHEN :sortOrder = 'ASC' THEN sort_col.content END ASC, " +
           "CASE WHEN :sortOrder = 'DESC' THEN sort_col.content END DESC, " +
           "CASE WHEN :sortOrder = '' THEN c.row_index END ASC, c.col_index ASC")
    LiveData<List<Cell>> getAllCellsSortedByColumn(long notebookId, int sortColIndex, String sortOrder);
    
    /**
     * 删除笔记本的所有单元格
     */
    @Query("DELETE FROM cells WHERE notebook_id = :notebookId")
    int deleteCellsByNotebookId(long notebookId);
    
    /**
     * 删除指定行的所有单元格
     */
    @Query("DELETE FROM cells WHERE notebook_id = :notebookId AND row_index = :rowIndex")
    int deleteCellsByRow(long notebookId, int rowIndex);
    
    /**
     * 删除指定列的所有单元格
     */
    @Query("DELETE FROM cells WHERE notebook_id = :notebookId AND col_index = :colIndex")
    int deleteCellsByColumn(long notebookId, int colIndex);
    
    /**
     * 更新单元格内容
     */
    @Query("UPDATE cells SET content = :content, updated_at = :updatedAt WHERE id = :id")
    int updateContent(long id, String content, long updatedAt);
    
    /**
     * 更新单元格图片
     */
    @Query("UPDATE cells SET image_id = :imageId, updated_at = :updatedAt WHERE id = :id")
    int updateImage(long id, String imageId, long updatedAt);
    
    /**
     * 更新单元格格式
     */
    @Query("UPDATE cells SET text_color = :textColor, background_color = :backgroundColor, is_bold = :isBold, is_italic = :isItalic, text_size = :textSize, text_alignment = :textAlignment, updated_at = :updatedAt WHERE id = :id")
    int updateFormat(long id, String textColor, String backgroundColor, boolean isBold, boolean isItalic, float textSize, String textAlignment, long updatedAt);
    
    /**
     * 清空单元格内容
     */
    @Query("UPDATE cells SET content = '', image_id = NULL, updated_at = :updatedAt WHERE id = :id")
    int clearCell(long id, long updatedAt);
    
    /**
     * 移动行（更新行索引）
     */
    @Query("UPDATE cells SET row_index = :newRowIndex, updated_at = :updatedAt WHERE notebook_id = :notebookId AND row_index = :oldRowIndex")
    int moveRow(long notebookId, int oldRowIndex, int newRowIndex, long updatedAt);
    
    /**
     * 移动列（更新列索引）
     */
    @Query("UPDATE cells SET col_index = :newColIndex, updated_at = :updatedAt WHERE notebook_id = :notebookId AND col_index = :oldColIndex")
    int moveColumn(long notebookId, int oldColIndex, int newColIndex, long updatedAt);
    
    /**
     * 插入行（将指定行及之后的行索引+1）
     */
    @Query("UPDATE cells SET row_index = row_index + 1, updated_at = :updatedAt WHERE notebook_id = :notebookId AND row_index >= :insertRowIndex")
    int insertRow(long notebookId, int insertRowIndex, long updatedAt);
    
    /**
     * 插入列（将指定列及之后的列索引+1）
     */
    @Query("UPDATE cells SET col_index = col_index + 1, updated_at = :updatedAt WHERE notebook_id = :notebookId AND col_index >= :insertColIndex")
    int insertColumn(long notebookId, int insertColIndex, long updatedAt);
    
    /**
     * 删除行后调整索引（将指定行之后的行索引-1）
     */
    @Query("UPDATE cells SET row_index = row_index - 1, updated_at = :updatedAt WHERE notebook_id = :notebookId AND row_index > :deletedRowIndex")
    int adjustRowIndexAfterDelete(long notebookId, int deletedRowIndex, long updatedAt);
    
    /**
     * 删除列后调整索引（将指定列之后的列索引-1）
     */
    @Query("UPDATE cells SET col_index = col_index - 1, updated_at = :updatedAt WHERE notebook_id = :notebookId AND col_index > :deletedColIndex")
    int adjustColumnIndexAfterDelete(long notebookId, int deletedColIndex, long updatedAt);
    
    /**
     * 获取笔记本的最大行索引
     */
    @Query("SELECT COALESCE(MAX(row_index), -1) FROM cells WHERE notebook_id = :notebookId")
    int getMaxRowIndex(long notebookId);
    
    /**
     * 获取笔记本的最大列索引
     */
    @Query("SELECT COALESCE(MAX(col_index), -1) FROM cells WHERE notebook_id = :notebookId")
    int getMaxColumnIndex(long notebookId);
    
    /**
     * 获取单元格总数
     */
    @Query("SELECT COUNT(*) FROM cells WHERE notebook_id = :notebookId")
    LiveData<Integer> getCellCount(long notebookId);
    
    /**
     * 获取非空单元格总数
     */
    @Query("SELECT COUNT(*) FROM cells WHERE notebook_id = :notebookId AND (content IS NOT NULL AND content != '' OR image_id IS NOT NULL AND image_id != '')")
    LiveData<Integer> getNonEmptyCellCount(long notebookId);
    
    /**
     * 获取所有使用的图片ID
     */
    @Query("SELECT DISTINCT image_id FROM cells WHERE image_id IS NOT NULL AND image_id != ''")
    List<String> getAllImageIds();
    
    /**
     * 根据图片ID获取单元格
     */
    @Query("SELECT * FROM cells WHERE image_id = :imageId")
    List<Cell> getCellsByImageId(String imageId);
}