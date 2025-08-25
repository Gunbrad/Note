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
    
    // ★ 像素宽度快照机制（修改建议3.md）
    private int[] pixelWidthSnapshot = null;
    private float lastSnapshotScale = -1f;
    private int lastSnapshotColumnCount = -1;
    
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
        // ★ 使用像素宽度快照确保一致性
        ensurePixelWidthSnapshot();
        if (pixelWidthSnapshot != null && columnIndex >= 0 && columnIndex < pixelWidthSnapshot.length) {
            return pixelWidthSnapshot[columnIndex];
        }
        // 回退到原始计算
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
            // ★ 列宽变化时刷新像素宽度快照
            invalidatePixelWidthSnapshot();
        }
    }
    
    @Override
    public int[] mapScrollXToPositionAndOffset(int scrollX) {
        List<Column> columns = noteViewModel.getColumns().getValue();
        if (columns == null || columns.isEmpty()) {
            return new int[]{0, 0};
        }

        // 严格限制 scrollX 范围，考虑视口宽度
        int totalWidth = getTotalColumnsWidthPx();
        int maxScroll = Math.max(0, totalWidth - 1); // 防止完全滚出视口
        scrollX = Math.max(0, Math.min(scrollX, maxScroll));
        
        int position = 0;
        int accumulatedWidth = 0;
        
        // 精确计算位置
        for (int i = 0; i < columns.size(); i++) {
            int columnWidth = getColumnWidthPx(i);
            if (accumulatedWidth + columnWidth > scrollX) {
                position = i;
                break;
            }
            accumulatedWidth += columnWidth;
            position = i + 1; // 防止越界
        }
        
        // 确保 position 不超出范围
        position = Math.min(position, columns.size() - 1);
        
        int offsetInFirst = Math.max(0, scrollX - accumulatedWidth);
        // 限制偏移量不超过该列宽度
        if (position < columns.size()) {
            offsetInFirst = Math.min(offsetInFirst, getColumnWidthPx(position) - 1);
        }
        
        return new int[]{position, offsetInFirst};
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
            // ★ 列宽变化时刷新像素宽度快照
            invalidatePixelWidthSnapshot();
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
    
    /**
     * ★ 确保像素宽度快照是最新的（修改建议3.md）
     * 使用最大余数法分配像素，确保header和rows的列宽完全一致
     */
    private void ensurePixelWidthSnapshot() {
        List<Column> columns = noteViewModel.getColumns().getValue();
        if (columns == null || columns.isEmpty()) {
            pixelWidthSnapshot = null;
            return;
        }
        
        float currentScale = getScale();
        int currentColumnCount = columns.size();
        
        // 检查是否需要重新计算快照
        if (pixelWidthSnapshot == null || 
            Math.abs(currentScale - lastSnapshotScale) > 0.001f ||
            currentColumnCount != lastSnapshotColumnCount) {
            
            // 重新计算像素宽度快照
            pixelWidthSnapshot = new int[currentColumnCount];
            
            // 计算每列的理论像素宽度（浮点数）
            float[] theoreticalWidths = new float[currentColumnCount];
            float totalTheoreticalWidth = 0f;
            for (int i = 0; i < currentColumnCount; i++) {
                theoreticalWidths[i] = getBaseColumnWidth(i) * currentScale;
                totalTheoreticalWidth += theoreticalWidths[i];
            }
            
            // 计算总的整数像素宽度
            int totalIntegerWidth = Math.round(totalTheoreticalWidth);
            
            // 使用最大余数法分配像素
            int allocatedWidth = 0;
            float[] remainders = new float[currentColumnCount];
            
            // 第一轮：分配整数部分
            for (int i = 0; i < currentColumnCount; i++) {
                int integerPart = (int) theoreticalWidths[i];
                pixelWidthSnapshot[i] = integerPart;
                allocatedWidth += integerPart;
                remainders[i] = theoreticalWidths[i] - integerPart;
            }
            
            // 第二轮：分配剩余像素给余数最大的列
            int remainingPixels = totalIntegerWidth - allocatedWidth;
            for (int round = 0; round < remainingPixels; round++) {
                int maxRemainderIndex = 0;
                for (int i = 1; i < currentColumnCount; i++) {
                    if (remainders[i] > remainders[maxRemainderIndex]) {
                        maxRemainderIndex = i;
                    }
                }
                pixelWidthSnapshot[maxRemainderIndex]++;
                remainders[maxRemainderIndex] = 0f; // 已分配，清零余数
            }
            
            // 更新快照状态
            lastSnapshotScale = currentScale;
            lastSnapshotColumnCount = currentColumnCount;
        }
    }
    
    /**
     * ★ 强制刷新像素宽度快照
     * 当列宽发生变化时调用
     */
    public void invalidatePixelWidthSnapshot() {
        pixelWidthSnapshot = null;
        lastSnapshotScale = -1f;
        lastSnapshotColumnCount = -1;
    }
}