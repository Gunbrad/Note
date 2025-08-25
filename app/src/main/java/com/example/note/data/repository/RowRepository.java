package com.example.note.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.note.data.dao.RowDao;
import com.example.note.data.database.AppDatabase;
import com.example.note.data.entity.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 行仓库类
 * 提供行数据的统一访问接口
 */
public class RowRepository {
    
    private static final String TAG = "RowRepository";
    private static volatile RowRepository INSTANCE;
    
    private final RowDao rowDao;
    private final Executor executor;
    
    private RowRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        rowDao = database.rowDao();
        executor = Executors.newFixedThreadPool(4);
    }
    
    /**
     * 获取仓库实例（单例模式）
     */
    public static RowRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (RowRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RowRepository(context);
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 获取指定笔记本的所有行
     */
    public LiveData<List<Row>> getRowsByNotebookId(long notebookId) {
        return rowDao.getRowsByNotebookId(notebookId);
    }
    
    /**
     * 同步获取指定笔记本的所有行
     */
    public List<Row> getRowsByNotebookIdSync(long notebookId) {
        return rowDao.getRowsByNotebookIdSync(notebookId);
    }
    
    /**
     * 获取指定行的高度
     */
    public Float getRowHeight(long notebookId, int rowIndex) {
        return rowDao.getRowHeight(notebookId, rowIndex);
    }
    
    /**
     * 异步获取指定行的高度
     */
    public void getRowHeightAsync(long notebookId, int rowIndex, RowHeightCallback callback) {
        executor.execute(() -> {
            try {
                Float height = rowDao.getRowHeight(notebookId, rowIndex);
                if (callback != null) {
                    callback.onResult(height != null ? height : 44.0f); // 默认44dp
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting row height", e);
                if (callback != null) {
                    callback.onResult(44.0f); // 默认44dp
                }
            }
        });
    }
    
    /**
     * 保存行
     */
    public void saveRow(Row row, SaveCallback callback) {
        executor.execute(() -> {
            try {
                long result = rowDao.insertRow(row);
                Log.d(TAG, "Row saved with ID: " + result);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving row", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 批量保存行
     */
    public void saveRows(List<Row> rows, SaveCallback callback) {
        executor.execute(() -> {
            try {
                List<Long> results = rowDao.insertRows(rows);
                Log.d(TAG, "Rows saved: " + results.size());
                if (callback != null) {
                    callback.onSuccess(results.size());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving rows", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新行高
     */
    public void updateRowHeight(long notebookId, int rowIndex, float height, SaveCallback callback) {
        executor.execute(() -> {
            try {
                long updatedAt = System.currentTimeMillis();
                int result = rowDao.updateRowHeight(notebookId, rowIndex, height, updatedAt);
                
                // 如果没有更新到记录，说明该行不存在，需要创建
                if (result == 0) {
                    Row newRow = new Row();
                    newRow.setNotebookId(notebookId);
                    newRow.setRowIndex(rowIndex);
                    newRow.setHeight(height);
                    newRow.setCreatedAt(updatedAt);
                    newRow.setUpdatedAt(updatedAt);
                    
                    long insertResult = rowDao.insertRow(newRow);
                    Log.d(TAG, "New row created with height: " + height + ", ID: " + insertResult);
                    if (callback != null) {
                        callback.onSuccess(insertResult);
                    }
                } else {
                    Log.d(TAG, "Row height updated: " + height);
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating row height", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 异步保存行高（别名方法）
     */
    public void saveRowAsync(long notebookId, int rowIndex, float height, SaveCallback callback) {
        updateRowHeight(notebookId, rowIndex, height, callback);
    }
    
    /**
     * 初始化指定笔记本的行数据
     */
    public void initializeRows(long notebookId, int rowCount, SaveCallback callback) {
        executor.execute(() -> {
            try {
                List<Row> rows = new ArrayList<>();
                long currentTime = System.currentTimeMillis();
                
                for (int i = 0; i < rowCount; i++) {
                    Row row = new Row();
                    row.setNotebookId(notebookId);
                    row.setRowIndex(i);
                    row.setHeight(44.0f); // 默认行高
                    row.setCreatedAt(currentTime);
                    row.setUpdatedAt(currentTime);
                    rows.add(row);
                }
                
                List<Long> results = rowDao.insertRows(rows);
                Log.d(TAG, "Initialized " + results.size() + " rows for notebook: " + notebookId);
                if (callback != null) {
                    callback.onSuccess(results.size());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing rows", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 删除指定笔记本的所有行
     */
    public void deleteRowsByNotebookId(long notebookId, SaveCallback callback) {
        executor.execute(() -> {
            try {
                int result = rowDao.deleteRowsByNotebookId(notebookId);
                Log.d(TAG, "Deleted " + result + " rows for notebook: " + notebookId);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting rows", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 插入行时调整后续行的索引
     */
    public void insertRowAt(long notebookId, int rowIndex, SaveCallback callback) {
        executor.execute(() -> {
            try {
                long updatedAt = System.currentTimeMillis();
                
                // 先调整后续行的索引
                rowDao.incrementRowIndicesFrom(notebookId, rowIndex, updatedAt);
                
                // 创建新行
                Row newRow = new Row();
                newRow.setNotebookId(notebookId);
                newRow.setRowIndex(rowIndex);
                newRow.setHeight(44.0f); // 默认行高
                newRow.setCreatedAt(updatedAt);
                newRow.setUpdatedAt(updatedAt);
                
                long result = rowDao.insertRow(newRow);
                Log.d(TAG, "Row inserted at index " + rowIndex + ", ID: " + result);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting row", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 删除行时调整后续行的索引
     */
    public void deleteRowAt(long notebookId, int rowIndex, SaveCallback callback) {
        executor.execute(() -> {
            try {
                long updatedAt = System.currentTimeMillis();
                
                // 删除指定行索引及之后的所有行
                rowDao.deleteRowsFromIndex(notebookId, rowIndex);
                
                Log.d(TAG, "Rows deleted from index: " + rowIndex);
                if (callback != null) {
                    callback.onSuccess(1);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting row", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 行高回调接口
     */
    public interface RowHeightCallback {
        void onResult(float height);
    }
    
    /**
     * 保存回调接口
     */
    public interface SaveCallback {
        void onSuccess(long result);
        void onError(Exception e);
    }
}