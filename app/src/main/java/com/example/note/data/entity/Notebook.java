package com.example.note.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 笔记本实体类
 * 对应数据库表：notebooks
 */
@Entity(
    tableName = "notebooks",
    indices = {
        @Index(value = "created_at"),
        @Index(value = "updated_at"),
        @Index(value = "is_deleted"),
        @Index(value = "is_pinned")
    }
)
public class Notebook {
    
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    
    @ColumnInfo(name = "title")
    private String title;
    
    @ColumnInfo(name = "color")
    private String color;
    
    @ColumnInfo(name = "description")
    private String description;
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    @ColumnInfo(name = "updated_at")
    private long updatedAt;
    
    @ColumnInfo(name = "is_deleted")
    private boolean isDeleted;
    
    @ColumnInfo(name = "deleted_at")
    private Long deletedAt;
    
    @ColumnInfo(name = "is_pinned")
    private boolean isPinned;
    
    @ColumnInfo(name = "pinned_at")
    private Long pinnedAt;
    
    // 构造函数
    public Notebook() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.isDeleted = false;
        this.isPinned = false;
    }
    
    public Notebook(String title, String color) {
        this();
        this.title = title;
        this.color = color;
    }
    
    // Getter和Setter方法
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public boolean isDeleted() {
        return isDeleted;
    }
    
    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
        if (deleted) {
            this.deletedAt = System.currentTimeMillis();
        } else {
            this.deletedAt = null;
        }
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Long getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(Long deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    public boolean isPinned() {
        return isPinned;
    }
    
    public void setPinned(boolean pinned) {
        isPinned = pinned;
        if (pinned) {
            this.pinnedAt = System.currentTimeMillis();
        } else {
            this.pinnedAt = null;
        }
        // 注意：置顶操作不应该修改updatedAt，updatedAt只反映内容编辑时间
    }
    
    public Long getPinnedAt() {
        return pinnedAt;
    }
    
    public void setPinnedAt(Long pinnedAt) {
        this.pinnedAt = pinnedAt;
    }
    
    /**
     * 软删除
     */
    public void softDelete() {
        setDeleted(true);
    }
    
    /**
     * 恢复删除
     */
    public void restore() {
        setDeleted(false);
    }
    
    /**
     * 置顶
     */
    public void pin() {
        setPinned(true);
    }
    
    /**
     * 取消置顶
     */
    public void unpin() {
        setPinned(false);
    }
    
    /**
     * 更新时间戳
     */
    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "Notebook{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", color='" + color + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isDeleted=" + isDeleted +
                ", deletedAt=" + deletedAt +
                ", isPinned=" + isPinned +
                ", pinnedAt=" + pinnedAt +
                '}';
    }
}