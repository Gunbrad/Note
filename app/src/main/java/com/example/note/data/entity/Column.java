package com.example.note.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 列实体类
 * 对应数据库表：columns
 */
@Entity(
    tableName = "columns",
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
        @Index(value = {"notebook_id", "column_index"}, unique = true)
    }
)
public class Column {
    
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    
    @ColumnInfo(name = "notebook_id")
    private long notebookId;
    
    @ColumnInfo(name = "column_index")
    private int columnIndex;
    
    @ColumnInfo(name = "name")
    private String name;
    
    @ColumnInfo(name = "width")
    private float width;
    
    @ColumnInfo(name = "type")
    private String type; // TEXT, NUMBER, DATE, BOOLEAN, IMAGE
    
    @ColumnInfo(name = "sort_order")
    private String sortOrder; // ASCENDING, DESCENDING, null
    
    @ColumnInfo(name = "filter_value")
    private String filterValue;
    
    @ColumnInfo(name = "is_visible")
    private boolean isVisible;
    
    @ColumnInfo(name = "is_frozen")
    private boolean isFrozen;
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    @ColumnInfo(name = "updated_at")
    private long updatedAt;
    
    // 构造函数
    public Column() {
        this.width = 150.0f; // 默认列宽
        this.type = "TEXT"; // 默认类型
        this.isVisible = true;
        this.isFrozen = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Column(long notebookId, int columnIndex, String name) {
        this();
        this.notebookId = notebookId;
        this.columnIndex = columnIndex;
        this.name = name;
    }
    
    // Getters and Setters
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
    
    public int getColumnIndex() {
        return columnIndex;
    }
    
    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public float getWidth() {
        return width;
    }
    
    public void setWidth(float width) {
        this.width = width;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public SortOrder getSortOrderEnum() {
        if (sortOrder == null) return null;
        try {
            return SortOrder.valueOf(sortOrder);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public void setSortOrderEnum(SortOrder sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder.name() : null;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getFilterValue() {
        return filterValue;
    }
    
    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean hasFilter() {
        return filterValue != null && !filterValue.trim().isEmpty();
    }
    
    public boolean isVisible() {
        return isVisible;
    }
    
    public void setVisible(boolean visible) {
        isVisible = visible;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean isFrozen() {
        return isFrozen;
    }
    
    public void setFrozen(boolean frozen) {
        isFrozen = frozen;
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
    
    // 排序枚举
    public enum SortOrder {
        ASCENDING,
        DESCENDING
    }
    
    // 列类型枚举
    public enum ColumnType {
        TEXT("TEXT"),
        NUMBER("NUMBER"),
        DATE("DATE"),
        BOOLEAN("BOOLEAN"),
        IMAGE("IMAGE");
        
        private final String value;
        
        ColumnType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static ColumnType fromString(String value) {
            for (ColumnType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return TEXT; // 默认返回TEXT类型
        }
    }
}