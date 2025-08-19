package com.example.note.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.note.data.entity.CellType;

/**
 * 单元格实体类
 * 对应数据库表：cells
 */
@Entity(
    tableName = "cells",
    foreignKeys = {
        @ForeignKey(
            entity = Notebook.class,
            parentColumns = "id",
            childColumns = "notebook_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = "notebook_id"),
        @Index(value = {"notebook_id", "row_index", "col_index"}, unique = true),
        @Index(value = "updated_at")
    }
)
public class Cell {
    
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    
    @ColumnInfo(name = "notebook_id")
    private long notebookId;
    
    @ColumnInfo(name = "row_index")
    private int rowIndex;
    
    @ColumnInfo(name = "col_index")
    private int colIndex;
    
    @ColumnInfo(name = "content")
    private String content;
    
    @ColumnInfo(name = "text_color")
    private String textColor;
    
    @ColumnInfo(name = "background_color")
    private String backgroundColor;
    
    @ColumnInfo(name = "is_bold")
    private boolean isBold;
    
    @ColumnInfo(name = "is_italic")
    private boolean isItalic;
    
    @ColumnInfo(name = "text_size")
    private float textSize;
    
    @ColumnInfo(name = "text_alignment")
    private String textAlignment;
    
    @ColumnInfo(name = "image_id")
    private String imageId;
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    @ColumnInfo(name = "updated_at")
    private long updatedAt;
    
    // 构造函数
    public Cell() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.content = "";
        this.textColor = "#000000";
        this.backgroundColor = "#FFFFFF";
        this.isBold = false;
        this.isItalic = false;
        this.textSize = 14.0f;
        this.textAlignment = "LEFT";
    }
    
    public Cell(long notebookId, int rowIndex, int colIndex) {
        this();
        this.notebookId = notebookId;
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
    }
    
    public Cell(long notebookId, int rowIndex, int colIndex, String content) {
        this(notebookId, rowIndex, colIndex);
        this.content = content;
    }
    
    // Getter和Setter方法
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getNotebookId() {
        return notebookId;
    }
    
    public void setNotebookId(long notebookId) {
        this.notebookId = notebookId;
    }
    
    public int getRowIndex() {
        return rowIndex;
    }
    
    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int getColIndex() {
        return colIndex;
    }
    
    public void setColIndex(int colIndex) {
        this.colIndex = colIndex;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getTextColor() {
        return textColor;
    }
    
    public void setTextColor(String textColor) {
        this.textColor = textColor;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getBackgroundColor() {
        return backgroundColor;
    }
    
    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean isBold() {
        return isBold;
    }
    
    public void setBold(boolean bold) {
        isBold = bold;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean isItalic() {
        return isItalic;
    }
    
    public void setItalic(boolean italic) {
        isItalic = italic;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public float getTextSize() {
        return textSize;
    }
    
    public void setTextSize(float textSize) {
        this.textSize = textSize;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getTextAlignment() {
        return textAlignment;
    }
    
    public void setTextAlignment(String textAlignment) {
        this.textAlignment = textAlignment;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getImageId() {
        return imageId;
    }
    
    public void setImageId(String imageId) {
        this.imageId = imageId;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 检查单元格是否为空
     */
    public boolean isEmpty() {
        return (content == null || content.trim().isEmpty()) && 
               (imageId == null || imageId.trim().isEmpty());
    }
    
    /**
     * 检查单元格是否包含图片
     */
    public boolean hasImage() {
        return imageId != null && !imageId.trim().isEmpty();
    }
    
    /**
     * 检查单元格是否包含文本
     */
    public boolean hasText() {
        return content != null && !content.trim().isEmpty();
    }
    
    /**
     * 清空单元格内容
     */
    public void clear() {
        this.content = "";
        this.imageId = null;
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * 重置格式为默认值
     */
    public void resetFormat() {
        this.textColor = "#000000";
        this.backgroundColor = "#FFFFFF";
        this.isBold = false;
        this.isItalic = false;
        this.textSize = 14.0f;
        this.textAlignment = "LEFT";
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * 更新时间戳
     */
    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
    
    // DataGrip风格的新增方法
    
    /**
     * 获取单元格显示值（用于适配器）
     */
    public String getValue() {
        return content != null ? content : "";
    }
    
    /**
     * 设置单元格值（用于适配器）
     */
    public void setValue(String value) {
        this.content = value;
        touch();
    }
    
    /**
     * 推断单元格类型
     */
    public CellType getType() {
        return CellType.detectType(content);
    }
    

    
    /**
     * 检查单元格是否被选中（临时状态，不存储到数据库）
     */
    private transient boolean selected = false;
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     * 获取格式化的显示文本
     */
    public String getFormattedText() {
        if (isEmpty()) {
            return "";
        }
        
        CellType type = getType();
        switch (type) {
            case NUMBER:
                try {
                    double num = Double.parseDouble(content);
                    if (num == (long) num) {
                        return String.valueOf((long) num);
                    } else {
                        return String.format("%.2f", num);
                    }
                } catch (NumberFormatException e) {
                    return content;
                }
            case BOOLEAN:
                if ("true".equalsIgnoreCase(content) || "1".equals(content) || "是".equals(content)) {
                    return "是";
                } else {
                    return "否";
                }
            default:
                return content;
        }
    }
    
    @Override
    public String toString() {
        return "Cell{" +
                "id=" + id +
                ", notebookId=" + notebookId +
                ", rowIndex=" + rowIndex +
                ", colIndex=" + colIndex +
                ", content='" + content + '\'' +
                ", textColor='" + textColor + '\'' +
                ", backgroundColor='" + backgroundColor + '\'' +
                ", isBold=" + isBold +
                ", isItalic=" + isItalic +
                ", textSize=" + textSize +
                ", imageId='" + imageId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}