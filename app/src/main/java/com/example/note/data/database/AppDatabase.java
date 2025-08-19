package com.example.note.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.note.data.dao.CellDao;
import com.example.note.data.dao.ColumnDao;
import com.example.note.data.dao.NotebookDao;
import com.example.note.data.dao.TemplateDao;
import com.example.note.data.entity.Cell;
import com.example.note.data.entity.Column;
import com.example.note.data.entity.Notebook;
import com.example.note.data.entity.Template;

/**
 * 应用数据库
 * Room数据库的主要配置类
 */
@Database(
        entities = {Notebook.class, Column.class, Cell.class, Template.class},
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "note_database";
    private static volatile AppDatabase INSTANCE;
    
    // 抽象方法，返回DAO接口
    public abstract NotebookDao notebookDao();
    public abstract CellDao cellDao();
    public abstract ColumnDao columnDao();
    public abstract TemplateDao templateDao();
    
    /**
     * 获取数据库实例（单例模式）
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // 启用WAL模式
                    .addCallback(DATABASE_CALLBACK) // 添加数据库回调
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // 添加数据库迁移
                    .fallbackToDestructiveMigration() // 允许破坏性迁移
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 数据库回调
     * 用于在数据库创建时初始化数据
     */
    private static final Callback DATABASE_CALLBACK = new Callback() {
        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            super.onCreate(db);
            
            // 创建FTS虚拟表用于全文搜索
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS cells_fts USING fts4(content=cells, content_text)");
            
            // 创建触发器，自动更新FTS表
            db.execSQL("CREATE TRIGGER IF NOT EXISTS cells_fts_insert AFTER INSERT ON cells BEGIN " +
                    "INSERT INTO cells_fts(docid, content_text) VALUES (new.id, new.content); END;");
            
            db.execSQL("CREATE TRIGGER IF NOT EXISTS cells_fts_update AFTER UPDATE ON cells BEGIN " +
                    "UPDATE cells_fts SET content_text = new.content WHERE docid = new.id; END;");
            
            db.execSQL("CREATE TRIGGER IF NOT EXISTS cells_fts_delete AFTER DELETE ON cells BEGIN " +
                    "DELETE FROM cells_fts WHERE docid = old.id; END;");
            
            // 创建笔记本更新时间触发器
            db.execSQL("CREATE TRIGGER IF NOT EXISTS update_notebook_time AFTER UPDATE ON cells BEGIN " +
                    "UPDATE notebooks SET updated_at = " + System.currentTimeMillis() + " WHERE id = new.notebook_id; END;");
            
            // 插入系统预置模板
            insertSystemTemplates(db);
        }
        
        @Override
        public void onOpen(SupportSQLiteDatabase db) {
            super.onOpen(db);
            
            // 启用外键约束
            db.execSQL("PRAGMA foreign_keys=ON");
            
            // 设置同步模式为NORMAL（平衡性能和安全性）
            db.execSQL("PRAGMA synchronous=NORMAL");
            
            // 设置缓存大小（2MB）
            db.execSQL("PRAGMA cache_size=2000");
            
            // 设置临时存储为内存
            db.execSQL("PRAGMA temp_store=MEMORY");
        }
    };
    
    /**
     * 插入系统预置模板
     */
    private static void insertSystemTemplates(SupportSQLiteDatabase db) {
        long currentTime = System.currentTimeMillis();
        
        // 空白模板
        db.execSQL("INSERT INTO templates (name, description, rows, cols, data, is_system, created_at) VALUES " +
                "('空白表格', '创建一个空白的表格笔记', 10, 5, NULL, 1, " + currentTime + ")");
        
        // 日程安排模板
        String scheduleData = "{\"headers\":[\"时间\",\"事项\",\"地点\",\"备注\"],\"rows\":10}";
        db.execSQL("INSERT INTO templates (name, description, rows, cols, data, is_system, created_at) VALUES " +
                "('日程安排', '用于记录日程安排的模板', 10, 4, '" + scheduleData + "', 1, " + currentTime + ")");
        
        // 收支记录模板
        String expenseData = "{\"headers\":[\"日期\",\"项目\",\"收入\",\"支出\",\"余额\"],\"rows\":15}";
        db.execSQL("INSERT INTO templates (name, description, rows, cols, data, is_system, created_at) VALUES " +
                "('收支记录', '用于记录收入和支出的模板', 15, 5, '" + expenseData + "', 1, " + currentTime + ")");
        
        // 房间清单模板
        String roomData = "{\"headers\":[\"房间号\",\"房型\",\"状态\",\"客人\",\"备注\"],\"rows\":20}";
        db.execSQL("INSERT INTO templates (name, description, rows, cols, data, is_system, created_at) VALUES " +
                "('房间清单', '民宿房间信息管理模板', 20, 5, '" + roomData + "', 1, " + currentTime + ")");
        
        // 购物清单模板
        String shoppingData = "{\"headers\":[\"物品\",\"数量\",\"单价\",\"总价\",\"已购买\"],\"rows\":15}";
        db.execSQL("INSERT INTO templates (name, description, rows, cols, data, is_system, created_at) VALUES " +
                "('购物清单', '用于记录购物清单的模板', 15, 5, '" + shoppingData + "', 1, " + currentTime + ")");
        
        // 联系人模板
        String contactData = "{\"headers\":[\"姓名\",\"电话\",\"邮箱\",\"地址\",\"备注\"],\"rows\":20}";
        db.execSQL("INSERT INTO templates (name, description, rows, cols, data, is_system, created_at) VALUES " +
                "('联系人', '用于管理联系人信息的模板', 20, 5, '" + contactData + "', 1, " + currentTime + ")");
    }
    
    /**
     * 关闭数据库（用于测试）
     */
    public static void closeDatabase() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
    
    /**
     * 数据库迁移（预留）
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 添加text_alignment字段到cells表
            database.execSQL("ALTER TABLE cells ADD COLUMN text_alignment TEXT DEFAULT 'LEFT'");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 检查并添加text_alignment字段（如果不存在）
            database.execSQL("ALTER TABLE cells ADD COLUMN text_alignment TEXT DEFAULT 'LEFT'");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 修改columns表的width字段类型从INTEGER改为REAL
            database.execSQL("CREATE TABLE columns_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "notebook_id INTEGER NOT NULL, " +
                    "column_index INTEGER NOT NULL, " +
                    "name TEXT, " +
                    "width REAL NOT NULL, " +
                    "type TEXT, " +
                    "sort_order TEXT, " +
                    "filter_value TEXT, " +
                    "is_visible INTEGER NOT NULL, " +
                    "is_frozen INTEGER NOT NULL, " +
                    "created_at INTEGER NOT NULL, " +
                    "updated_at INTEGER NOT NULL, " +
                    "FOREIGN KEY(notebook_id) REFERENCES notebooks(id) ON DELETE CASCADE)");
            
            // 复制数据，处理可能缺失的字段
            database.execSQL("INSERT INTO columns_new (id, notebook_id, column_index, name, width, type, sort_order, filter_value, is_visible, is_frozen, created_at, updated_at) " +
                    "SELECT id, notebook_id, " +
                    "COALESCE(column_index, 0) as column_index, " +
                    "name, " +
                    "CAST(width AS REAL) as width, " +
                    "COALESCE(type, 'TEXT') as type, " +
                    "sort_order, " +
                    "filter_value, " +
                    "is_visible, " +
                    "COALESCE(is_frozen, 0) as is_frozen, " +
                    "COALESCE(created_at, " + System.currentTimeMillis() + ") as created_at, " +
                    "COALESCE(updated_at, " + System.currentTimeMillis() + ") as updated_at " +
                    "FROM columns");
            
            // 删除旧表
            database.execSQL("DROP TABLE columns");
            
            // 重命名新表
            database.execSQL("ALTER TABLE columns_new RENAME TO columns");
            
            // 重新创建索引
            database.execSQL("CREATE INDEX index_columns_notebook_id ON columns(notebook_id)");
            database.execSQL("CREATE UNIQUE INDEX index_columns_notebook_id_column_index ON columns(notebook_id, column_index)");
        }
    };
}