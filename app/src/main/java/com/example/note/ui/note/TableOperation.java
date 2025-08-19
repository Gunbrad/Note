package com.example.note.ui.note;

import com.example.note.data.entity.Cell;
import com.example.note.data.entity.Column;

import java.util.List;

/**
 * 表格操作类
 * 用于撤销重做功能的操作记录
 */
public class TableOperation {
    
    public enum OperationType {
        ADD_ROW,
        DELETE_ROW,
        ADD_COLUMN,
        DELETE_COLUMN,
        UPDATE_CELL,
        UPDATE_COLUMN,
        SORT_COLUMN,
        FILTER_COLUMN
    }
    
    private OperationType type;
    private int position;
    private Object oldValue;
    private Object newValue;
    private List<Cell> affectedCells;
    private Column affectedColumn;
    private long timestamp;
    
    public TableOperation(OperationType type, int position) {
        this.type = type;
        this.position = position;
        this.timestamp = System.currentTimeMillis();
    }
    
    public TableOperation(OperationType type, int position, Object oldValue, Object newValue) {
        this.type = type;
        this.position = position;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public OperationType getType() {
        return type;
    }
    
    public void setType(OperationType type) {
        this.type = type;
    }
    
    public int getPosition() {
        return position;
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
    
    public Object getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }
    
    public Object getNewValue() {
        return newValue;
    }
    
    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }
    
    public List<Cell> getAffectedCells() {
        return affectedCells;
    }
    
    public void setAffectedCells(List<Cell> affectedCells) {
        this.affectedCells = affectedCells;
    }
    
    public Column getAffectedColumn() {
        return affectedColumn;
    }
    
    public void setAffectedColumn(Column affectedColumn) {
        this.affectedColumn = affectedColumn;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "TableOperation{" +
                "type=" + type +
                ", position=" + position +
                ", timestamp=" + timestamp +
                '}';
    }
}