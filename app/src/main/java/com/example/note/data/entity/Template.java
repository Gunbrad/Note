package com.example.note.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 模板实体类
 * 对应数据库表：templates
 */
@Entity(
    tableName = "templates",
    indices = {
        @Index(value = "created_at"),
        @Index(value = "is_system")
    }
)
public class Template {
    
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    
    @ColumnInfo(name = "name")
    private String name;
    
    @ColumnInfo(name = "description")
    private String description;
    
    @ColumnInfo(name = "rows")
    private int rows;
    
    @ColumnInfo(name = "cols")
    private int cols;
    
    @ColumnInfo(name = "data")
    private String data; // JSON格式的模板数据
    
    @ColumnInfo(name = "is_system")
    private boolean isSystem; // 是否为系统预置模板
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    // 构造函数
    public Template() {
        this.createdAt = System.currentTimeMillis();
        this.isSystem = false;
        this.rows = 10;
        this.cols = 5;
    }
    
    public Template(String name, String description, int rows, int cols) {
        this();
        this.name = name;
        this.description = description;
        this.rows = rows;
        this.cols = cols;
    }
    
    public Template(String name, String description, int rows, int cols, String data, boolean isSystem) {
        this(name, description, rows, cols);
        this.data = data;
        this.isSystem = isSystem;
    }
    
    // Getter和Setter方法
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getRows() {
        return rows;
    }
    
    public void setRows(int rows) {
        this.rows = rows;
    }
    
    public int getCols() {
        return cols;
    }
    
    public void setCols(int cols) {
        this.cols = cols;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public boolean isSystem() {
        return isSystem;
    }
    
    public void setSystem(boolean system) {
        isSystem = system;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Template{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", rows=" + rows +
                ", cols=" + cols +
                ", data='" + data + '\'' +
                ", isSystem=" + isSystem +
                ", createdAt=" + createdAt +
                '}';
    }
}