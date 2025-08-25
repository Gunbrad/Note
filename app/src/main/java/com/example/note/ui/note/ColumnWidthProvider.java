package com.example.note.ui.note;

/**
 * 列宽和行高提供者接口
 * 根据当前缩放比例计算实际的像素尺寸
 * 这是实现真正布局缩放的核心接口
 */
public interface ColumnWidthProvider {
    
    /**
     * 获取指定列的实际宽度（像素）
     * @param columnIndex 列索引
     * @return 实际宽度 = baseWidth * scale
     */
    int getColumnWidthPx(int columnIndex);
    
    /**
     * 获取行高（像素）
     * @return 实际行高 = baseRowHeight * scale
     */
    int getRowHeightPx();
    
    /**
     * 获取指定行的行高（像素）
     * @param rowIndex 行索引
     * @return 实际行高 = baseRowHeight * scale
     */
    int getRowHeightPx(int rowIndex);
    
    /**
     * 获取当前缩放比例
     * @return 当前缩放比例
     */
    float getScale();
    
    /**
     * 获取基础列宽（未缩放）
     * @param columnIndex 列索引
     * @return 基础列宽
     */
    float getBaseColumnWidth(int columnIndex);
    
    /**
     * 获取基础行高（未缩放）
     * @return 基础行高
     */
    float getBaseRowHeight();
    
    /**
     * 获取指定行的基础行高（未缩放）
     * @param rowIndex 行索引
     * @return 基础行高
     */
    float getBaseRowHeight(int rowIndex);
    
    /**
     * 获取基础文本大小（未缩放）
     * @return 基础文本大小
     */
    float getBaseTextSize();
    
    /**
     * 获取实际文本大小（像素）
     * @return 实际文本大小 = baseTextSize * scale
     */
    float getTextSizePx();
    
    /**
     * 获取所有列的总宽度（像素）
     * @return 所有列宽度之和
     */
    int getTotalColumnsWidthPx();
    
    /**
     * 将横向滚动偏移量映射为列索引和列内偏移
     * @param scrollX 横向滚动偏移量
     * @return int[]{firstVisibleColIndex, offsetInFirstCol}
     */
    int[] mapScrollXToPositionAndOffset(int scrollX);
    
    /**
     * 获取可滚动的总宽度（像素）
     * @return 可滚动宽度
     */
    int getScrollableWidthPx();
    
    /**
     * 设置列的基准dp宽度
     * @param columnIndex 列索引
     * @param widthDp 基准宽度（dp）
     */
    void setColumnWidthDp(int columnIndex, float widthDp);
    
    /**
     * 设置行的基准dp高度
     * @param heightDp 基准高度（dp）
     */
    void setRowHeightDp(float heightDp);
    
    /**
     * 设置指定行的基准dp高度
     * @param rowIndex 行索引
     * @param heightDp 基准高度（dp）
     */
    void setRowHeightDp(int rowIndex, float heightDp);
    
    /**
     * dp转换为px
     * @param dp dp值
     * @return px值
     */
    int dpToPx(float dp);
    
    /**
     * px转换为dp
     * @param px px值
     * @return dp值
     */
    float pxToDp(int px);
}