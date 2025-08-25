package com.example.note.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.note.data.dao.NotebookDao;
import com.example.note.data.database.AppDatabase;
import com.example.note.data.entity.Notebook;
import com.example.note.util.ColorUtils;
import com.example.note.util.DateUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 笔记本仓库类
 * 提供笔记本数据的统一访问接口
 */
public class NotebookRepository {
    
    private static final String TAG = "NotebookRepository";
    private static volatile NotebookRepository INSTANCE;
    
    private final NotebookDao notebookDao;
    private final Executor executor;
    private final Handler mainHandler;
    
    // LiveData缓存
    private final LiveData<List<Notebook>> allNotebooks;
    private final LiveData<List<Notebook>> deletedNotebooks;
    private final LiveData<Integer> notebookCount;
    
    private NotebookRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        notebookDao = database.notebookDao();
        executor = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化LiveData
        allNotebooks = notebookDao.getAllNotebooks();
        deletedNotebooks = notebookDao.getDeletedNotebooks();
        notebookCount = notebookDao.getNotebookCount();
    }
    
    /**
     * 获取Repository实例（单例模式）
     */
    public static NotebookRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (NotebookRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new NotebookRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 获取所有笔记本
     */
    public LiveData<List<Notebook>> getAllNotebooks() {
        return allNotebooks;
    }
    
    /**
     * 获取回收站中的笔记本
     */
    public LiveData<List<Notebook>> getDeletedNotebooks() {
        return deletedNotebooks;
    }
    
    /**
     * 获取笔记本总数
     */
    public LiveData<Integer> getNotebookCount() {
        return notebookCount;
    }
    
    /**
     * 根据ID获取笔记本
     */
    public LiveData<Notebook> getNotebookById(long id) {
        return notebookDao.getById(id);
    }
    
    /**
     * 搜索笔记本
     */
    public LiveData<List<Notebook>> searchNotebooks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return allNotebooks;
        }
        return notebookDao.searchNotebooks(query.trim());
    }
    
    /**
     * 将成功回调投递到主线程
     */
    private <T> void deliverSuccess(RepositoryCallback<T> callback, T result) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(result));
        }
    }
    
    /**
     * 将错误回调投递到主线程
     */
    private <T> void deliverError(RepositoryCallback<T> callback, Exception error) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(error));
        }
    }
    
    /**
     * 根据颜色筛选笔记本
     */
    public LiveData<List<Notebook>> getNotebooksByColor(String color) {
        return notebookDao.getNotebooksByColor(color);
    }
    
    /**
     * 获取最近的笔记本
     */
    public LiveData<List<Notebook>> getRecentNotebooks(int limit) {
        return notebookDao.getRecentNotebooks(limit);
    }
    
    /**
     * 获取最近更新的笔记本
     */
    public LiveData<List<Notebook>> getRecentlyUpdatedNotebooks(int limit) {
        return notebookDao.getRecentlyUpdatedNotebooks(limit);
    }
    
    /**
     * 创建新笔记本
     */
    public void createNotebook(String title, String color, RepositoryCallback<Long> callback) {
        executor.execute(() -> {
            try {
                // 验证参数
                if (title == null || title.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("标题不能为空"));
                    }
                    return;
                }
                
                // 检查标题是否已存在
                if (notebookDao.countByTitle(title.trim(), 0) > 0) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("标题已存在"));
                    }
                    return;
                }
                
                // 创建笔记本
                Notebook notebook = new Notebook();
                notebook.setTitle(title.trim());
                notebook.setColor(color != null ? color : ColorUtils.getDefaultNotebookColor());
                
                long id = notebookDao.insert(notebook);
                
                if (callback != null) {
                    callback.onSuccess(id);
                }
                
                Log.d(TAG, "Notebook created: " + title + ", id: " + id);
            } catch (Exception e) {
                Log.e(TAG, "Failed to create notebook: " + title, e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新笔记本
     */
    public void updateNotebook(Notebook notebook, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                notebook.touch();
                int result = notebookDao.update(notebook);
                
                if (result > 0) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    Log.d(TAG, "Notebook updated: " + notebook.getId());
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("更新失败"));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update notebook: " + notebook.getId(), e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新笔记本标题
     */
    public void updateNotebookTitle(long id, String title, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                // 验证参数
                if (title == null || title.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("标题不能为空"));
                    }
                    return;
                }
                
                // 检查标题是否已存在
                if (notebookDao.countByTitle(title.trim(), id) > 0) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("标题已存在"));
                    }
                    return;
                }
                
                int result = notebookDao.updateTitle(id, title.trim(), DateUtils.now());
                
                if (result > 0) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    Log.d(TAG, "Notebook title updated: " + id);
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("更新失败"));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update notebook title: " + id, e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新笔记本颜色
     */
    public void updateNotebookColor(long id, String color, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                String validColor = color != null ? color : ColorUtils.getDefaultNotebookColor();
                int result = notebookDao.updateColor(id, validColor, DateUtils.now());
                
                if (result > 0) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    Log.d(TAG, "Notebook color updated: " + id);
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("更新失败"));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update notebook color: " + id, e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 软删除笔记本
     */
    public void deleteNotebook(long id, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                long now = DateUtils.now();
                int result = notebookDao.softDelete(id, now, now);
                
                if (result > 0) {
                    deliverSuccess(callback, null);
                    Log.d(TAG, "Notebook deleted: " + id);
                } else {
                    deliverError(callback, new RuntimeException("删除失败"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete notebook: " + id, e);
                deliverError(callback, e);
            }
        });
    }
    
    /**
     * 恢复删除的笔记本
     */
    public void restoreNotebook(long id, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                int result = notebookDao.restore(id, DateUtils.now());
                
                if (result > 0) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    Log.d(TAG, "Notebook restored: " + id);
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("恢复失败"));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to restore notebook: " + id, e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 置顶笔记本
     */
    public void pinNotebook(long id, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                long now = DateUtils.now();
                int result = notebookDao.pin(id, now);
                
                if (result > 0) {
                    deliverSuccess(callback, null);
                    Log.d(TAG, "Notebook pinned: " + id);
                } else {
                    deliverError(callback, new RuntimeException("置顶失败"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to pin notebook: " + id, e);
                deliverError(callback, e);
            }
        });
    }
    
    /**
     * 取消置顶笔记本
     */
    public void unpinNotebook(long id, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                int result = notebookDao.unpin(id);
                
                if (result > 0) {
                    deliverSuccess(callback, null);
                    Log.d(TAG, "Notebook unpinned: " + id);
                } else {
                    deliverError(callback, new RuntimeException("取消置顶失败"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to unpin notebook: " + id, e);
                deliverError(callback, e);
            }
        });
    }
    
    /**
     * 永久删除笔记本
     */
    public void permanentlyDeleteNotebook(long id, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                Notebook notebook = notebookDao.getByIdSync(id);
                if (notebook != null) {
                    int result = notebookDao.delete(notebook);
                    
                    if (result > 0) {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                        Log.d(TAG, "Notebook permanently deleted: " + id);
                    } else {
                        if (callback != null) {
                            callback.onError(new RuntimeException("删除失败"));
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("笔记本不存在"));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to permanently delete notebook: " + id, e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 清理过期的删除笔记本
     */
    public void cleanupExpiredNotebooks(RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                long thirtyDaysAgo = DateUtils.now() - (30L * 24 * 60 * 60 * 1000);
                int result = notebookDao.deleteExpiredNotebooks(thirtyDaysAgo);
                
                if (callback != null) {
                    callback.onSuccess(result);
                }
                
                Log.d(TAG, "Cleaned up " + result + " expired notebooks");
            } catch (Exception e) {
                Log.e(TAG, "Failed to cleanup expired notebooks", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新笔记本的更新时间
     */
    public void touchNotebook(long id) {
        executor.execute(() -> {
            try {
                notebookDao.touch(id, DateUtils.now());
                Log.d(TAG, "Notebook touched: " + id);
            } catch (Exception e) {
                Log.e(TAG, "Failed to touch notebook: " + id, e);
            }
        });
    }
    
    /**
     * 检查笔记本名称是否已存在
     */
    public void checkNotebookNameExists(String name, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                Notebook notebook = notebookDao.getNotebookByName(name);
                boolean exists = notebook != null;
                
                if (callback != null) {
                    callback.onSuccess(exists);
                }
                
                Log.d(TAG, "Notebook name exists check: " + name + " = " + exists);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check notebook name: " + name, e);
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