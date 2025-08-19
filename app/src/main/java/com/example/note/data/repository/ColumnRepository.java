package com.example.note.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.note.data.dao.ColumnDao;
import com.example.note.data.database.AppDatabase;
import com.example.note.data.entity.Column;
import com.example.note.data.repository.NotebookRepository.RepositoryCallback;
import com.example.note.util.DateUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 列仓库类
 * 提供列数据的统一访问接口
 */
public class ColumnRepository {
    
    private static final String TAG = "ColumnRepository";
    private static volatile ColumnRepository INSTANCE;
    
    private final ColumnDao columnDao;
    private final Executor executor;
    
    private ColumnRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        columnDao = database.columnDao();
        executor = Executors.newFixedThreadPool(4);
    }
    
    /**
     * 获取仓库实例（单例模式）
     */
    public static ColumnRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ColumnRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ColumnRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 根据笔记本ID获取所有列
     */
    public LiveData<List<Column>> getColumnsByNotebookId(long notebookId) {
        return columnDao.getColumnsByNotebookId(notebookId);
    }
    
    /**
     * 根据位置获取列
     */
    public LiveData<Column> getColumnByPosition(long notebookId, int columnIndex) {
        return columnDao.getColumnByPosition(notebookId, columnIndex);
    }
    
    /**
     * 获取可见列
     */
    public LiveData<List<Column>> getVisibleColumns(long notebookId) {
        return columnDao.getVisibleColumns(notebookId);
    }
    
    /**
     * 获取冻结列
     */
    public LiveData<List<Column>> getFrozenColumns(long notebookId) {
        return columnDao.getFrozenColumns(notebookId);
    }
    
    /**
     * 获取列总数
     */
    public LiveData<Integer> getColumnCount(long notebookId) {
        return columnDao.getColumnCount(notebookId);
    }
    
    /**
     * 获取最大列索引
     */
    public void getMaxColumnIndex(long notebookId, RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                int maxIndex = columnDao.getMaxColumnIndex(notebookId);
                if (callback != null) {
                    callback.onSuccess(maxIndex);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get max column index", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 保存列
     */
    public void saveColumn(Column column, RepositoryCallback<Long> callback) {
        executor.execute(() -> {
            try {
                long currentTime = DateUtils.now();
                if (column.getId() == 0) {
                    // 新增
                    column.setCreatedAt(currentTime);
                    column.setUpdatedAt(currentTime);
                    long id = columnDao.insert(column);
                    column.setId(id);
                    
                    if (callback != null) {
                        callback.onSuccess(id);
                    }
                    
                    Log.d(TAG, "Column saved with ID: " + id);
                } else {
                    // 更新
                    column.setUpdatedAt(currentTime);
                    int rowsAffected = columnDao.update(column);
                    
                    if (callback != null) {
                        callback.onSuccess((long) rowsAffected);
                    }
                    
                    Log.d(TAG, "Column updated: " + column.getId());
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to save column", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 批量保存列
     */
    public void saveColumns(List<Column> columns, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                long currentTime = DateUtils.now();
                for (Column column : columns) {
                    if (column.getId() == 0) {
                        column.setCreatedAt(currentTime);
                    }
                    column.setUpdatedAt(currentTime);
                }
                
                columnDao.insertAll(columns);
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Columns saved: " + columns.size());
            } catch (Exception e) {
                Log.e(TAG, "Failed to save columns", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 删除列
     */
    public void deleteColumn(long notebookId, int columnIndex, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                // 删除指定列
                columnDao.deleteColumnByPosition(notebookId, columnIndex);
                
                // 调整后续列的索引
                columnDao.adjustColumnIndexAfterDelete(notebookId, columnIndex, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Column deleted: " + columnIndex);
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete column", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 插入列
     */
    public void insertColumn(long notebookId, int columnIndex, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                // 调整后续列的索引
                columnDao.insertColumn(notebookId, columnIndex, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Column inserted at: " + columnIndex);
            } catch (Exception e) {
                Log.e(TAG, "Failed to insert column", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 移动列
     */
    public void moveColumn(long notebookId, int fromIndex, int toIndex, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                columnDao.moveColumn(notebookId, fromIndex, toIndex, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Column moved from " + fromIndex + " to " + toIndex);
            } catch (Exception e) {
                Log.e(TAG, "Failed to move column", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新列宽度
     */
    public void updateColumnWidth(long columnId, float width, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                columnDao.updateWidth(columnId, width, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Column width updated: " + columnId + " -> " + width);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update column width", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新列名称
     */
    public void updateColumnName(long columnId, String name, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                columnDao.updateName(columnId, name, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Column name updated: " + columnId + " -> " + name);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update column name", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新列可见性
     */
    public void updateColumnVisibility(long columnId, boolean isVisible, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                columnDao.updateVisibility(columnId, isVisible, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Column visibility updated: " + columnId + " -> " + isVisible);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update column visibility", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新列排序
     */
    public void updateColumnSortOrder(long columnId, String sortOrder, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                columnDao.updateSortOrder(columnId, sortOrder, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Column sort order updated: " + columnId + " -> " + sortOrder);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update column sort order", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新列筛选值
     */
    public void updateColumnFilterValue(long columnId, String filterValue, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                columnDao.updateFilterValue(columnId, filterValue, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Column filter value updated: " + columnId + " -> " + filterValue);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update column filter value", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 删除笔记本的所有列
     */
    public void deleteColumnsByNotebookId(long notebookId, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                columnDao.deleteColumnsByNotebookId(notebookId);
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "All columns deleted for notebook: " + notebookId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete columns by notebook ID", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Repository回调接口
     */
    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
}