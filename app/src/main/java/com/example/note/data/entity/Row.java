package com.example.note.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 行实体类
 * 对应数据库表：rows
 * 存储每行的高度等属性
 */
@Entity(
    tableName = "rows",
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
        @Index(value = {"notebook_id", "row_index"}, unique = true)
    }
)
public class Row {
    
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    
    @ColumnInfo(name = "notebook_id")
    private long notebookId;
    
    @ColumnInfo(name = "row_index")
    private int rowIndex;
    
    @ColumnInfo(name = "height_dp")
    private float height; // 行高，单位dp
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    @ColumnInfo(name = "updated_at")
    private long updatedAt;
    
    // 构造函数
    public Row() {
        this.height = 44.0f; // 默认行高44dp
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
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
    }
    
    public float getHeight() {
        return height;
    }
    
    public void setHeight(float height) {
        this.height = height;
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
    
    @Override
    public String toString() {
        return "Row{" +
                "id=" + id +
                ", notebookId=" + notebookId +
                ", rowIndex=" + rowIndex +
                ", height=" + height +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}