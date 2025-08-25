package com.example.note.ui.note;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.example.note.data.entity.Column;
import com.example.note.data.entity.Notebook;
import com.example.note.data.repository.RowRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ColumnWidthProvider的实现类
 * 基于NoteViewModel提供动态的列宽和行高计算
 */
public class ColumnWidthProviderImpl implements ColumnWidthProvider {
    
    private final NoteViewModel noteViewModel;
    private final Context context;
    private final RowRepository rowRepository;
    private float baseRowHeight; // dp转换为px
    private final float baseTextSize; // sp转换为px
    
    // 行高缓存，避免在主线程访问数据库
    private final Map<String, Float> rowHeightCache = new HashMap<>();
    
    // 默认尺寸（像素）
    private static final float DEFAULT_COLUMN_WIDTH_PX = 120f;
    private static final float DEFAULT_ROW_HEIGHT_PX = 44f;
    private static final float DEFAULT_TEXT_SIZE_PX = 14f;
    
    public ColumnWidthProviderImpl(NoteViewModel noteViewModel, Context context, RowRepository rowRepository, float baseRowHeightPx, float baseTextSizePx) {
        this.noteViewModel = noteViewModel;
        this.context = context;
        this.rowRepository = rowRepository;
        this.baseRowHeight = baseRowHeightPx;
        this.baseTextSize = baseTextSizePx;
    }
    
    public ColumnWidthProviderImpl(NoteViewModel noteViewModel, Context context, RowRepository rowRepository) {
        this(noteViewModel, context, rowRepository, DEFAULT_ROW_HEIGHT_PX, DEFAULT_TEXT_SIZE_PX);
    }
    
    @Override
    public int getColumnWidthPx(int columnIndex) {
        float baseWidth = getBaseColumnWidth(columnIndex);
        return Math.round(baseWidth * getScale());
    }
    
    @Override
    public int getRowHeightPx() {
        return Math.round(baseRowHeight * getScale());
    }
    
    @Override
    public int getRowHeightPx(int rowIndex) {
        float baseHeight = getBaseRowHeight(rowIndex);
        return Math.round(baseHeight * getScale());
    }
    
    @Override
    public float getScale() {
        Float scale = noteViewModel.getScale().getValue();
        return scale != null ? scale : 1.0f;
    }
    
    @Override
    public float getBaseColumnWidth(int columnIndex) {
        List<Column> columns = noteViewModel.getColumns().getValue();
        if (columns != null && columnIndex >= 0 && columnIndex < columns.size()) {
            Column column = columns.get(columnIndex);
            return column.getWidth(); // 这是基础宽度
        }
        return DEFAULT_COLUMN_WIDTH_PX;
    }
    
    @Override
    public float getBaseRowHeight() {
        return baseRowHeight;
    }
    
    @Override
    public float getBaseRowHeight(int rowIndex) {
        if (noteViewModel != null) {
            Notebook notebook = noteViewModel.getCurrentNotebook().getValue();
            if (notebook != null) {
                String cacheKey = notebook.getId() + "_" + rowIndex;
                Float cachedHeight = rowHeightCache.get(cacheKey);
                if (cachedHeight != null) {
                    return cachedHeight;
                }
                // 如果缓存中没有，返回默认值
                // 实际的数据库查询应该在后台线程中进行
                return DEFAULT_ROW_HEIGHT_PX;
            }
        }
        return DEFAULT_ROW_HEIGHT_PX;
    }
    
    @Override
    public float getBaseTextSize() {
        return baseTextSize;
    }
    
    @Override
    public float getTextSizePx() {
        return baseTextSize * getScale();
    }
    
    @Override
    public int getTotalColumnsWidthPx() {
        List<Column> columns = noteViewModel.getColumns().getValue();
        if (columns == null || columns.isEmpty()) {
            return 0;
        }
        
        int totalWidth = 0;
        for (int i = 0; i < columns.size(); i++) {
            totalWidth += getColumnWidthPx(i);
        }
        return totalWidth;
    }
    
    /**
     * 设置列的基础宽度
     * @param columnIndex 列索引
     * @param baseWidth 基础宽度
     */
    public void setBaseColumnWidth(int columnIndex, float baseWidth) {
        List<Column> columns = noteViewModel.getColumns().getValue();
        if (columns != null && columnIndex >= 0 && columnIndex < columns.size()) {
            Column column = columns.get(columnIndex);
            column.setWidth(baseWidth);
            noteViewModel.updateColumn(column);
        }
    }
    
    @Override
    public int[] mapScrollXToPositionAndOffset(int scrollX) {
        List<Column> columns = noteViewModel.getColumns().getValue();
        if (columns == null || columns.isEmpty()) {
            return new int[]{0, 0};
        }
        
        int accumulatedWidth = 0;
        for (int i = 0; i < columns.size(); i++) {
            int columnWidth = getColumnWidthPx(i);
            if (scrollX < accumulatedWidth + columnWidth) {
                // 找到了第一个可见列
                int offsetInFirstCol = scrollX - accumulatedWidth;
                return new int[]{i, offsetInFirstCol};
            }
            accumulatedWidth += columnWidth;
        }
        
        // 如果scrollX超出了所有列的总宽度，返回最后一列
        return new int[]{columns.size() - 1, getColumnWidthPx(columns.size() - 1)};
    }
    
    @Override
    public int getScrollableWidthPx() {
        return getTotalColumnsWidthPx();
    }
    
    @Override
    public void setColumnWidthDp(int columnIndex, float widthDp) {
        List<Column> columns = noteViewModel.getColumns().getValue();
        if (columns != null && columnIndex >= 0 && columnIndex < columns.size()) {
            Column column = columns.get(columnIndex);
            column.setWidth(widthDp);
            noteViewModel.updateColumn(column);
        }
    }
    
    @Override
    public void setRowHeightDp(float heightDp) {
        this.baseRowHeight = heightDp;
    }
    
    @Override
    public void setRowHeightDp(int rowIndex, float heightDp) {
        if (noteViewModel != null && rowRepository != null) {
            Notebook notebook = noteViewModel.getCurrentNotebook().getValue();
            if (notebook != null) {
                // 更新缓存
                String cacheKey = notebook.getId() + "_" + rowIndex;
                rowHeightCache.put(cacheKey, heightDp);
                // 异步保存到数据库
                rowRepository.saveRowAsync(notebook.getId(), rowIndex, heightDp, null);
            }
        }
    }
    
    @Override
    public int dpToPx(float dp) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }
    
    @Override
    public float pxToDp(int px) {
        return px / context.getResources().getDisplayMetrics().density;
    }
    
    /**
     * 预加载行高数据到缓存中
     * 应该在后台线程中调用
     */
    public void preloadRowHeights(long notebookId, int maxRowIndex) {
        if (rowRepository != null) {
            // 在后台线程中执行
            new Thread(() -> {
                for (int i = 0; i <= maxRowIndex; i++) {
                    try {
                        Float height = rowRepository.getRowHeight(notebookId, i);
                        if (height != null) {
                            String cacheKey = notebookId + "_" + i;
                            rowHeightCache.put(cacheKey, height);
                        }
                    } catch (Exception e) {
                        // 忽略错误，使用默认值
                    }
                }
            }).start();
        }
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        rowHeightCache.clear();
    }
}