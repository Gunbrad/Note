package com.example.note.data.database;

import android.content.Context;
import android.util.Log;

import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 数据库助手类
 * 管理SQLite的高级配置和触发器操作
 */
public class DatabaseHelper {
    
    private static final String TAG = "DatabaseHelper";
    private static final Executor executor = Executors.newSingleThreadExecutor();
    
    private final AppDatabase database;
    
    public DatabaseHelper(Context context) {
        this.database = AppDatabase.getInstance(context);
    }
    
    /**
     * 初始化数据库配置
     */
    public void initializeDatabase() {
        executor.execute(() -> {
            try {
                SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
                
                // 设置WAL模式（已在AppDatabase中设置）
                Log.d(TAG, "Database initialized with WAL mode");
                
                // 验证FTS表是否存在
                verifyFtsTable(db);
                
                // 验证触发器是否存在
                verifyTriggers(db);
                
                // 优化数据库
                optimizeDatabase(db);
                
                Log.d(TAG, "Database initialization completed");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize database", e);
            }
        });
    }
    
    /**
     * 验证FTS表是否存在
     */
    private void verifyFtsTable(SupportSQLiteDatabase db) {
        try {
            // 检查FTS表是否存在
            android.database.Cursor cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='cells_fts'");
            boolean ftsExists = cursor.moveToFirst();
            cursor.close();
            
            if (!ftsExists) {
                Log.w(TAG, "FTS table not found, creating...");
                createFtsTable(db);
            } else {
                Log.d(TAG, "FTS table verified");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to verify FTS table", e);
        }
    }
    
    /**
     * 创建FTS表和相关触发器
     */
    private void createFtsTable(SupportSQLiteDatabase db) {
        try {
            // 创建FTS虚拟表
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS cells_fts USING fts4(content=cells, content)");
            
            // 创建触发器
            db.execSQL("CREATE TRIGGER IF NOT EXISTS cells_fts_insert AFTER INSERT ON cells BEGIN " +
                    "INSERT INTO cells_fts(docid, content) VALUES (new.id, new.content); END;");
            
            db.execSQL("CREATE TRIGGER IF NOT EXISTS cells_fts_update AFTER UPDATE ON cells BEGIN " +
                    "UPDATE cells_fts SET content = new.content WHERE docid = new.id; END;");
            
            db.execSQL("CREATE TRIGGER IF NOT EXISTS cells_fts_delete AFTER DELETE ON cells BEGIN " +
                    "DELETE FROM cells_fts WHERE docid = old.id; END;");
            
            // 初始化FTS表数据
            db.execSQL("INSERT INTO cells_fts(docid, content) SELECT id, content FROM cells WHERE content IS NOT NULL AND content != ''");
            
            Log.d(TAG, "FTS table and triggers created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create FTS table", e);
        }
    }
    
    /**
     * 验证触发器是否存在
     */
    private void verifyTriggers(SupportSQLiteDatabase db) {
        try {
            String[] triggerNames = {
                "cells_fts_insert",
                "cells_fts_update", 
                "cells_fts_delete"
            };
            
            for (String triggerName : triggerNames) {
                android.database.Cursor cursor = db.query("SELECT name FROM sqlite_master WHERE type='trigger' AND name=?", new String[]{triggerName});
                boolean exists = cursor.moveToFirst();
                cursor.close();
                
                if (!exists) {
                    Log.w(TAG, "Trigger not found: " + triggerName);
                    createMissingTrigger(db, triggerName);
                } else {
                    Log.d(TAG, "Trigger verified: " + triggerName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to verify triggers", e);
        }
    }
    
    /**
     * 创建缺失的触发器
     */
    private void createMissingTrigger(SupportSQLiteDatabase db, String triggerName) {
        try {
            switch (triggerName) {
                case "cells_fts_insert":
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS cells_fts_insert AFTER INSERT ON cells BEGIN " +
                            "INSERT INTO cells_fts(docid, content) VALUES (new.id, new.content); END;");
                    break;
                case "cells_fts_update":
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS cells_fts_update AFTER UPDATE ON cells BEGIN " +
                            "UPDATE cells_fts SET content = new.content WHERE docid = new.id; END;");
                    break;
                case "cells_fts_delete":
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS cells_fts_delete AFTER DELETE ON cells BEGIN " +
                            "DELETE FROM cells_fts WHERE docid = old.id; END;");
                    break;

            }
            Log.d(TAG, "Created missing trigger: " + triggerName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create trigger: " + triggerName, e);
        }
    }
    
    /**
     * 优化数据库
     */
    private void optimizeDatabase(SupportSQLiteDatabase db) {
        try {
            // 分析表统计信息
            db.execSQL("ANALYZE");
            
            // 重建FTS索引
            db.execSQL("INSERT INTO cells_fts(cells_fts) VALUES('rebuild')");
            
            // 清理未使用的页面
            db.execSQL("VACUUM");
            
            Log.d(TAG, "Database optimization completed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to optimize database", e);
        }
    }
    
    /**
     * 执行全文搜索
     */
    public void performFullTextSearch(String query, SearchCallback callback) {
        executor.execute(() -> {
            try {
                SupportSQLiteDatabase db = database.getOpenHelper().getReadableDatabase();
                
                // 构建FTS查询
                String ftsQuery = "SELECT cells.* FROM cells " +
                        "JOIN cells_fts ON cells.id = cells_fts.docid " +
                        "WHERE cells_fts MATCH ? " +
                        "ORDER BY cells.updated_at DESC";
                
                android.database.Cursor cursor = db.query(ftsQuery, new String[]{query + "*"});
                
                // 处理搜索结果
                java.util.List<Long> cellIds = new java.util.ArrayList<>();
                while (cursor.moveToNext()) {
                    long cellId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    cellIds.add(cellId);
                }
                cursor.close();
                
                // 回调结果
                if (callback != null) {
                    callback.onSearchCompleted(cellIds);
                }
                
                Log.d(TAG, "Full text search completed, found " + cellIds.size() + " results");
            } catch (Exception e) {
                Log.e(TAG, "Failed to perform full text search", e);
                if (callback != null) {
                    callback.onSearchError(e);
                }
            }
        });
    }
    
    /**
     * 清理过期数据
     */
    public void cleanupExpiredData() {
        executor.execute(() -> {
            try {
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                
                // 清理回收站中30天前删除的笔记本
                int deletedNotebooks = database.notebookDao().deleteExpiredNotebooks(thirtyDaysAgo);
                
                Log.d(TAG, "Cleanup completed, deleted " + deletedNotebooks + " expired notebooks");
            } catch (Exception e) {
                Log.e(TAG, "Failed to cleanup expired data", e);
            }
        });
    }
    
    /**
     * 获取数据库统计信息
     */
    public void getDatabaseStats(StatsCallback callback) {
        executor.execute(() -> {
            try {
                SupportSQLiteDatabase db = database.getOpenHelper().getReadableDatabase();
                
                // 获取各表的记录数
                android.database.Cursor cursor = db.query("SELECT COUNT(*) FROM notebooks WHERE is_deleted = 0");
                cursor.moveToFirst();
                int notebookCount = cursor.getInt(0);
                cursor.close();
                
                cursor = db.query("SELECT COUNT(*) FROM cells");
                cursor.moveToFirst();
                int cellCount = cursor.getInt(0);
                cursor.close();
                
                cursor = db.query("SELECT COUNT(*) FROM templates");
                cursor.moveToFirst();
                int templateCount = cursor.getInt(0);
                cursor.close();
                
                // 获取数据库文件大小
                cursor = db.query("PRAGMA page_count");
                cursor.moveToFirst();
                int pageCount = cursor.getInt(0);
                cursor.close();
                
                cursor = db.query("PRAGMA page_size");
                cursor.moveToFirst();
                int pageSize = cursor.getInt(0);
                cursor.close();
                
                long databaseSize = (long) pageCount * pageSize;
                
                DatabaseStats stats = new DatabaseStats(notebookCount, cellCount, templateCount, databaseSize);
                
                if (callback != null) {
                    callback.onStatsReady(stats);
                }
                
                Log.d(TAG, "Database stats: " + stats.toString());
            } catch (Exception e) {
                Log.e(TAG, "Failed to get database stats", e);
                if (callback != null) {
                    callback.onStatsError(e);
                }
            }
        });
    }
    
    /**
     * 搜索回调接口
     */
    public interface SearchCallback {
        void onSearchCompleted(java.util.List<Long> cellIds);
        void onSearchError(Exception error);
    }
    
    /**
     * 统计信息回调接口
     */
    public interface StatsCallback {
        void onStatsReady(DatabaseStats stats);
        void onStatsError(Exception error);
    }
    
    /**
     * 数据库统计信息类
     */
    public static class DatabaseStats {
        public final int notebookCount;
        public final int cellCount;
        public final int templateCount;
        public final long databaseSize;
        
        public DatabaseStats(int notebookCount, int cellCount, int templateCount, long databaseSize) {
            this.notebookCount = notebookCount;
            this.cellCount = cellCount;
            this.templateCount = templateCount;
            this.databaseSize = databaseSize;
        }
        
        @Override
        public String toString() {
            return "DatabaseStats{" +
                    "notebookCount=" + notebookCount +
                    ", cellCount=" + cellCount +
                    ", templateCount=" + templateCount +
                    ", databaseSize=" + databaseSize +
                    '}';
        }
    }
}