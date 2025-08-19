# 民宿表格笔记 APP

一个专为民宿房间信息管理设计的离线表格笔记应用，采用类 Excel 交互方式，但更加轻量便捷。

## 📋 项目概述

**目标**：个人离线笔记 App，用"表格"记录民宿房间相关信息，偏 Excel 交互，但更轻量。

**平台**：Android 手机；不联网；仅 Java 实现；数据全本地存储。

**范围**：现阶段只专注"表格模板 + 表格笔记"能力；不实现导入/导出、不做提醒。

**关键理念**：免配置起步，边用边长出结构；首页瀑布流，笔记内类 Excel 网格，不用卡片视图。

## 📚 目录

1. [核心功能拆解与验收要点](#1-核心功能拆解与验收要点)
2. [架构与技术路线](#2-架构与技术路线)
3. [构建与运行环境](#3-构建与运行环境)
4. [数据与本地存储设计](#4-数据与本地存储设计)
5. [界面与交互设计](#5-界面与交互设计)
6. [搜索与筛选排序实现](#6-搜索与筛选排序实现)
7. [图片处理与文件布局](#7-图片处理与文件布局)
8. [撤销/重做、回收站与清理](#8-撤销重做回收站与清理)
9. [性能目标与手段](#9-性能目标与手段)
10. [质量保障与测试计划](#10-质量保障与测试计划)
11. [可访问性、适配与本地化](#11-可访问性适配与本地化)
12. [安全与隐私](#12-安全与隐私)
13. [包结构与代码约定](#13-包结构与代码约定)
14. [路线图与非目标](#14-路线图与非目标)
15. [FAQ 与排障](#15-faq-与排障)

## 1. 核心功能拆解与验收要点

### 一）主页（瀑布流）

- **展示**：瀑布流显示各笔记（支持 100+ 本流畅滚动）。
- **搜索**：输入即过滤；笔记名 + 聚合文本索引联动搜索。
- **排序**：按更新时间、名称；支持置顶/归档分组。
- **新建**：浮动"+"创建笔记，输入唯一名并选择格式（仅表格）；默认 5×5。
- **管理**：长按卡片 → 重命名、颜色、置顶、归档、删除（回收站）。
- **验收**：100+ 本流畅；搜索/排序即时反馈。

### 二）创建笔记与模板

- **新建笔记**：输入唯一名 → 选"表格" → 浏览模板 → 应用模板（生成列结构，保留 5 行空白）。
- **创建模板**：在笔记页"更多"→"存为模板"，序列化列结构。
- **验收**：新建后零空转；首屏提示"点 + 添加列/行"。

### 三）笔记页面（类 Excel 但简约）

#### 列与行：
- "+ 列"在表头最右，"+ 行"在首列顶部；最后一行回车自动新增。
- 列头：改名、隐藏/显示、拖动排序、冻结首列；极简设置（显示名/关键字段/宽度/隐藏）。

#### 单元格与类型（免配置）：
- **自动识别**：基于单元格内容与列内多数值推断（文本/日期/数字/图片/布尔）。
- **友好控件**：日期选择器、数字键盘、图片缩略图与相册/相机选取；布尔勾选。
- **快捷**：长按单元格 → 复制/粘贴/向下填充/插入图片；双击进入表单模式；连续录入；拖拽边框调整整列/整行尺寸。

#### 筛选与排序（就地、轻量）：
- 列头轻筛（文本包含、日期/数字区间、布尔）。
- 全局筛选：底部抽屉组合条件，可保存为 Chips。
- 排序：列头循环 ↑/↓/无；可加次级排序；记住上次。

#### 其他功能：
- 图片管理：单张查看/删除；自动压缩与缩略图生成。
- 撤销/重做与回收站：最近 N 步可撤销；删行/列入回收站（可恢复）。
- 快捷智能：识别日期列后提供"今日/本周/本月"快捷筛；粘贴多行文本自动拆行。
- 顶部导航栏：返回/更多（含"存为模板"）、搜索、撤销/恢复、保存；编辑工具条（加粗、对齐、字号、背景色、换行开关、复制/粘贴、向下填充、撤销/重做）。
- 默认：冻结首列、同步滚动；编辑即保存。

**验收**：万级行检索数百毫秒返回；交互顺滑。

### 四）搜索

- **主页**：笔记名 LIKE 与 FTS 命中融合返回。
- **笔记内**：全文检索文本型内容，高亮。
- **验收**：FTS 支撑，数百毫秒返回。

### 补充限制

- 单元格支持文本/数字/日期/布尔/图片；用户无需手动设类型。
- 笔记间相互独立；不允许重名。
- 仅安卓手机；无导入/导出；无提醒；表格页不使用"卡片式单元格"。

## 2. 架构与技术路线

**模式**：MVVM + Repository（全 Java）

**组件**：
- AndroidX: ViewModel, LiveData, Activity Result API
- Room 2.6.1（SQLite，WAL；部分 FTS/触发器用原生 SQL）
- WorkManager 2.9.0（图片压缩/缩略/清理、定期维护）
- RecyclerView 1.3.2（瀑布流 + 网格）
- Glide 4.16.0（图片加载）
- Photo Picker（无需外部存储权限）

**并发**：Room IO + Executors；WorkManager 后台；主线程仅 UI。

**依赖注入**：简化为手动构造或轻量工厂（Java 环境下避免额外 DI 框架）。

**日志**：android.util.Log + 可切换 DebugLogger。

## 3. 构建与运行环境

- **minSdkVersion**: 33
- **targetSdkVersion**: 35（Android 15）
- **compileSdkVersion**: 35
- **Java 17**（Gradle Toolchain），启用 coreLibraryDesugaring
- **打包签名与安装**面向本地（无需 Play 要求）
- **权限**：不申请外部存储；使用系统 Photo Picker；相机权限仅在需要拍照时按需申请

### Gradle 关键片段（build.gradle）

```gradle
android {
  compileSdkVersion 35
  defaultConfig {
    minSdkVersion 34
    targetSdkVersion 35
  }
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_17
    targetCompatibility JavaVersion.VERSION_17
    coreLibraryDesugaringEnabled true
  }
}

dependencies {
  implementation "androidx.recyclerview:recyclerview:1.3.2"
  implementation "androidx.activity:activity:1.9.0"
  implementation "androidx.lifecycle:lifecycle-viewmodel:2.8.4"
  implementation "androidx.lifecycle:lifecycle-livedata:2.8.4"
  implementation "androidx.room:room-runtime:2.6.1"
  annotationProcessor "androidx.room:room-compiler:2.6.1"
  implementation "androidx.work:work-runtime:2.9.0"
  implementation "com.github.bumptech.glide:glide:4.16.0"
  annotationProcessor "com.github.bumptech.glide:compiler:4.16.0"
  coreLibraryDesugaring "com.android.tools:desugar_jdk_libs:2.0.4"
}
```

### AndroidManifest 关键点

- 不声明 INTERNET 权限。
- 使用 Photo Picker（无需 READ_EXTERNAL_STORAGE）。
- 可选 CAMERA（若支持直接拍照）。

## 4. 数据与本地存储设计

**时间戳**：统一使用 INTEGER epochMillis（UTC）。

### 表结构（Room + 原生 SQL 创建部分约束/触发器）

#### notebooks
```sql
id INTEGER PK
name TEXT UNIQUE NOT NULL
color TEXT NOT NULL
pinned INTEGER NOT NULL DEFAULT 0
archived INTEGER NOT NULL DEFAULT 0
created_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
last_opened_at INTEGER NOT NULL
deleted_at INTEGER NULL
```

#### columns
```sql
id INTEGER PK
notebook_id INTEGER NOT NULL REFERENCES notebooks(id) ON DELETE CASCADE
name TEXT NOT NULL
order_index INTEGER NOT NULL
width_dp INTEGER NOT NULL
hidden INTEGER NOT NULL DEFAULT 0
is_key INTEGER NOT NULL DEFAULT 0
created_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
```

#### rows
```sql
id INTEGER PK
notebook_id INTEGER NOT NULL REFERENCES notebooks(id) ON DELETE CASCADE
order_index INTEGER NOT NULL
height_dp INTEGER NOT NULL DEFAULT 44
created_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
deleted_at INTEGER NULL
```

#### cells（EAV）
```sql
id INTEGER PK
notebook_id INTEGER NOT NULL REFERENCES notebooks(id) ON DELETE CASCADE
row_id INTEGER NOT NULL REFERENCES rows(id) ON DELETE CASCADE
column_id INTEGER NOT NULL REFERENCES columns(id) ON DELETE CASCADE
type TEXT NOT NULL CHECK (type IN ('text','number','date','bool','image'))
text_value TEXT NULL
number_value REAL NULL
date_value INTEGER NULL
bool_value INTEGER NULL
image_id TEXT NULL
bg_color TEXT NULL
text_color TEXT NULL
align INTEGER NULL // -1 左 0 中 1 右
created_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
UNIQUE(row_id, column_id)
```

#### cells_text（FTS 内容表）
```sql
rowid INTEGER PRIMARY KEY
notebook_id INTEGER NOT NULL
row_id INTEGER NOT NULL
column_id INTEGER NOT NULL
text TEXT NOT NULL
```

#### cells_text_fts（FTS5 虚表）
```sql
FTS5(text, content='cells_text', content_rowid='rowid')
```

#### images（图片元数据与引用计数）
```sql
image_id TEXT PRIMARY KEY
width INTEGER NOT NULL
height INTEGER NOT NULL
bytes INTEGER NOT NULL
ref_count INTEGER NOT NULL
created_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
```

#### templates
```sql
id INTEGER PK
name TEXT UNIQUE NOT NULL
schema_json TEXT NOT NULL
created_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
```

### 索引

- idx_rows_note_order(notebook_id, order_index)
- idx_cols_note_order(notebook_id, order_index)
- idx_cells_row_col(row_id, column_id)
- idx_cells_note_col(notebook_id, column_id)
- idx_cells_col_number(column_id, number_value)
- idx_cells_col_date(column_id, date_value)
- idx_cells_col_bool(column_id, bool_value)
- idx_cells_text_note_col(notebook_id, column_id)

### SQLite 配置（应用启动时执行）

```sql
PRAGMA journal_mode=WAL;
PRAGMA synchronous=NORMAL;
PRAGMA foreign_keys=ON;
```

### 触发器（在 RoomDatabase.Callback.onCreate/onOpen 中执行原生 SQL）

**同步 cells → cells_text 与 FTS**：
INSERT/UPDATE/DELETE cells 时，若 type='text' 或 text_value 变化，更新 cells_text；FTS 采用 external content 自动跟随。

**维护 images.ref_count**：
cells.image_id 插入/替换/清空时对应 +1/-1；删除行/列/单元格时 -1。

**软删除策略**：
notebooks/rows/columns 设置 deleted_at 后由清理任务 30 天后物理删除并级联清理关联 cells、cells_text、FTS、图片引用。

#### 示例触发器（节选）

```sql
-- cells_text 同步（插入）
CREATE TRIGGER IF NOT EXISTS trg_cells_ins_text AFTER INSERT ON cells
WHEN NEW.type='text' AND NEW.text_value IS NOT NULL
BEGIN
  INSERT INTO cells_text(rowid, notebook_id, row_id, column_id, text)
  VALUES (NEW.id, NEW.notebook_id, NEW.row_id, NEW.column_id, NEW.text_value);
END;

-- cells_text 同步（更新）
CREATE TRIGGER IF NOT EXISTS trg_cells_upd_text AFTER UPDATE OF text_value, type ON cells
BEGIN
  DELETE FROM cells_text WHERE rowid=OLD.id;
  INSERT INTO cells_text(rowid, notebook_id, row_id, column_id, text)
  SELECT NEW.id, NEW.notebook_id, NEW.row_id, NEW.column_id, NEW.text_value
  WHERE NEW.type='text' AND NEW.text_value IS NOT NULL;
END;

-- cells_text 同步（删除）
CREATE TRIGGER IF NOT EXISTS trg_cells_del_text AFTER DELETE ON cells
BEGIN
  DELETE FROM cells_text WHERE rowid=OLD.id;
END;

-- images ref_count（插入）
CREATE TRIGGER IF NOT EXISTS trg_cells_ins_img AFTER INSERT ON cells
WHEN NEW.image_id IS NOT NULL
BEGIN
  INSERT INTO images(image_id, width, height, bytes, ref_count, created_at, updated_at)
  VALUES (NEW.image_id, 0, 0, 0, 1, strftime('%s','now')*1000, strftime('%s','now')*1000)
  ON CONFLICT(image_id) DO UPDATE SET ref_count=ref_count+1, updated_at=strftime('%s','now')*1000;
END;

-- images ref_count（替换）
CREATE TRIGGER IF NOT EXISTS trg_cells_upd_img AFTER UPDATE OF image_id ON cells
BEGIN
  -- 减旧
  UPDATE images SET ref_count=ref_count-1, updated_at=strftime('%s','now')*1000
  WHERE image_id=OLD.image_id AND OLD.image_id IS NOT NULL;
  -- 增新
  INSERT INTO images(image_id, width, height, bytes, ref_count, created_at, updated_at)
  VALUES (NEW.image_id, 0, 0, 0, 1, strftime('%s','now')*1000, strftime('%s','now')*1000)
  ON CONFLICT(image_id) DO UPDATE SET ref_count=ref_count+1, updated_at=strftime('%s','now')*1000;
END;

-- images ref_count（删除）
CREATE TRIGGER IF NOT EXISTS trg_cells_del_img AFTER DELETE ON cells
WHEN OLD.image_id IS NOT NULL
BEGIN
  UPDATE images SET ref_count=ref_count-1, updated_at=strftime('%s','now')*1000
  WHERE image_id=OLD.image_id;
END;
```

### 文件布局

**应用私有目录**：
- files/images/{uuid}.jpg
- files/thumbs/{uuid}_w256.jpg

## 5. 界面与交互设计

### 首页（瀑布流）

- RecyclerView + StaggeredGridLayoutManager（2–3 列自适应）
- 分组展示：Pinned → 普通 → Archived（单 Adapter，Section Header）
- 搜索：150ms 节流；LiveData 驱动；即时过滤
- 长按卡片管理；删除入回收站

### 笔记页面（类 DataGrip 的表格）

#### 布局：
- 左侧冻结首列 RV，右侧其余列 RV；共享垂直滚动监听，同步滚动。
- 右侧每行内嵌水平 RV 承载单元格；统一水平偏移控制器；禁用子 RV 嵌套滚动冲突。

#### 列头：
- 显示：列名、排序箭头、筛选漏斗；末列"+ 列"；首列顶部"+ 行"。
- 就地设置：改名、隐藏/显示、拖动排序、冻结首列、宽度、是否关键字段。

#### 单元格编辑：
- 双击进入编辑；长按弹出菜单（复制/粘贴/剪切/清空、向下/向右填充、插入/替换图片、背景/文本色、对齐）。
- 识别类型后弹相应控件：日期选择器、数字键盘、图片选择（相机/相册）、布尔开关。
- 连续录入：保存后跳同列下一行。
- 拖拽边框调整整列宽/整行高；缩放（0.75–2.0）影响字体/行高/列宽。

#### 顶部导航：
- 返回、更多（含"存为模板"）、搜索、撤销/重做、保存
- 工具条（常驻/编辑态）：加粗、对齐、字号、背景色、换行、复制/粘贴、向下填充、撤销/重做

**指南**：首次进入显示"点 + 添加列/行"

### 默认参数

- 行高 44dp，列宽 120dp；首列冻结；编辑即保存（失焦或确认即落库）
- 搜索输入节流 150ms；SnackBar 展示撤销提示

## 6. 搜索与筛选排序实现

### 主页搜索

name LIKE '%q%' 与 FTS 命中的 notebook_id 并集，去重后按置顶、归档、更新时间排序。

### 笔记内全文检索

在 cells_text_fts 上限定 notebook_id 查询，返回 row_id/column_id；使用 snippet/offsets 生成高亮片段。

### 筛选

#### 列头轻筛：
- 文本包含：对该列在 FTS 限定 column_id 的 row_id 集合
- 数字区间：cells.number_value BETWEEN
- 日期区间：cells.date_value BETWEEN（epochMillis）
- 布尔：cells.bool_value = 0/1

#### 组合筛选：
第一个条件写入 temp_filter(row_id)，后续 INNER JOIN 收窄；最终与 rows 连接按 order_index 恢复顺序。

#### 快捷筛：保存多条件为 Chips，一键应用。

### 排序

ORDER BY 目标列对应类型字段（number/date/text），NULLS LAST；支持二级排序链；记忆上次配置。

### 类型推断（免配置）

每列维护最近 200 个非空单元格类型频次；占比最高者作为"呈现类型"，仅影响控件与筛选 UI，不强制转换既有数据。

## 7. 图片处理与文件布局

- **选取**：ActivityResultContracts.PickVisualMedia（Photo Picker）
- **存储**：复制为 files/images/{uuid}.jpg，长边 2048px，JPEG 质量 82，EXIF 方向校正、去除冗余标签
- **缩略**：files/thumbs/{uuid}_w256.jpg（短边 256）
- **引用**：cells.image_id 保存 uuid；images.ref_count 通过触发器维护
- **查看**：缩略 → 全屏查看；可删除（清空 image_id，ref_count-1）
- **清理**：WorkManager 每日任务删除 ref_count=0 的孤儿文件与记录

## 8. 撤销/重做、回收站与清理

### 撤销/重做

- 每本笔记维护内存环形栈（默认深度 50）
- 命令模式：记录 do/undo 行为与受影响范围快照；批量合并；数据库写在单事务中
- UI：SnackBar 展示撤销提示与恢复入口

### 回收站

- 删除行/列：rows.deleted_at 设置或 columns.hidden 标记；30 天后后台硬删并清理关联
- 笔记删除：notebooks.deleted_at 设置，延迟物理删除

### 维护任务（WorkManager）

- **每日**：孤儿图片清理、超期回收站硬删、ANALYZE
- **每月**：VACUUM（App 处于空闲/充电/Wi-Fi 非必需，因为离线）

## 9. 性能目标与手段

### 目标

- 主页 100+ 本流畅滚动；首屏 < 200ms
- 笔记页：万级行全文检索/筛选 100–300ms 返回
- 编辑落库单次 < 16ms（主线程无阻塞）

### 手段

- DiffUtil + stable IDs + 共享 RecycledViewPool，setItemViewCacheSize=20
- SQLite WAL；批量写入 500 条/事务
- 查询分批分页；只渲染可视窗口 + 预取
- FTS5 + 专用索引；查询节流与去抖
- 图片缩略优先加载；原图延迟加载；Glide 内存/磁盘缓存
- 统一滚动控制器减少嵌套滚动抖动

## 10. 质量保障与测试计划

### 测试类型

- **单元测试**：Repository、类型推断、筛选/排序组合 SQL
- **仓内集成测试**：Room DAO（使用 inMemoryDatabaseBuilder + 触发器/FTS 初始化）
- **UI 测试**：RecyclerView 交互、长按菜单、拖拽、缩放、同步滚动
- **性能测试**：大数据集脚本灌入（≥ 10 万单元格），测检索耗时
- **稳定性**：进程杀死恢复（WAL + 事务一致性）

### 验收清单（节选）

- [ ] 主页 100+ 本流畅
- [ ] 搜索/排序即时
- [ ] 新建后零空转；首屏提示露出
- [ ] 首列冻结与同步滚动稳定
- [ ] 文本/数字/日期/布尔/图片控件正确弹出
- [ ] 多条件筛选与快捷筛可用
- [ ] 撤销/重做可回溯 N 步
- [ ] 回收站还原与 30 天后硬删
- [ ] 图片压缩与缩略生成正确，孤儿清理正常

## 11. 可访问性、适配与本地化

- **TalkBack 描述**：列名、行号、单元格内容与类型提示
- **触控目标**：最小 44dp；手势与菜单均可达（无障碍替代操作）
- **文本缩放**：跟随系统字体比例；缩放上限 2.0
- **适配**：窄屏优先；横屏优化（更宽视口）；不同密度下尺寸等比缩放
- **本地化**：简体中文为主，字符串集中 strings.xml

## 12. 安全与隐私

- 完全离线，不请求网络权限
- 图片与数据库均存于应用私有目录
- 不采集个人数据；可选本地崩溃日志（仅设备本地）
- 回收站与物理删除遵循最小保留策略；彻底删除图片文件与索引

## 13. 包结构与代码约定

### 建议包结构（Java）

```
data/
  db/（RoomDatabase、DAO、触发器初始化）
  repo/（仓库类，聚合 DAO + 文件操作）
  model/（Entity/POJO/DTO）
domain/
  command/（撤销/重做命令）
  filter/（筛选与排序表达式）
  infer/（类型推断策略）
ui/
  home/（主页瀑布流）
  notebook/（网格页：冻结列/滚动协调/适配器）
  dialog/（模板选择、列设置等）
  widgets/（单元格视图、列头、工具条）
worker/（图片压缩、缩略、清理、维护）
util/（时间、颜色、UUID、文件、Glide 工具）
di/（简单工厂/Service Locator）
```

### 代码约定

- 只使用 Java 17 语法（避开 Kotlin/协程）
- 主线程无数据库/磁盘 IO
- DAO 原则：读写分离，分页与限流；复杂查询用 @RawQuery
- UI 状态通过 ViewModel + LiveData 暴露；避免直接持有 Context
- 单元格渲染采用 ViewHolder 池复用，禁止在 onBind 做重计算

## 14. 路线图与非目标

### 近期（MVP）

- 表格模板（创建/应用/更新）
- 基础筛选与排序；全文检索；撤销/重做；回收站
- 图片压缩与缩略；孤儿清理

### 中期

- 快捷筛 Chips；多级排序 UI
- 多选拖拽矩形框选与批量操作
- 更丰富单元格样式（局部加粗/富文本受限）

### 非目标（当前阶段不做）

- 云同步/分享/协作
- 导入/导出
- 定时提醒
- 非表格格式的笔记类型

## 15. FAQ 与排障

### FTS5 与 Room 兼容性？
使用 Room 正常表管理；FTS5 虚表与触发器在 RoomDatabase.Callback 中原生 SQL 创建；查询用 SupportSQLiteQuery。

### 为什么两套 RV 做冻结列？
简化同步滚动与回收复用，避免超大单列表在横向滚动下的复杂性与抖动。

### 类型推断是否会误判？
仅影响呈现控件与筛选 UI，不修改已存数据；提供"强制输入"为兜底。

### 大数据下卡顿？
确保分页加载、可视窗口渲染、索引齐全；排查 Glide 大图策略与缓存命中；审查任何主线程阻塞。

## 附：关键实现片段

### 主页搜索（Repository 伪代码）

```java
public LiveData<List<NotebookCard>> searchNotebooks(String q) {
  return Transformations.switchMap(queryLiveData(q), query -> db.runInTransaction(() -> {
    Set<Long> ids = new HashSet<>(dao.findNotebookIdsByNameLike(query));
    ids.addAll(dao.findNotebookIdsByFts(query));
    return dao.loadNotebookCardsByIdsOrdered(new ArrayList<>(ids));
  }));
}
```

### 笔记内 FTS 查询（原生）

```sql
SELECT row_id, column_id, snippet(cells_text_fts, -1, '[', ']', '…', 10) AS hl
FROM cells_text_fts
JOIN cells_text ON cells_text.rowid = cells_text_fts.rowid
WHERE cells_text.notebook_id = ? AND cells_text_fts MATCH ?
LIMIT 100;
```

### WorkManager 定时（Java）

```java
PeriodicWorkRequest daily = new PeriodicWorkRequest.Builder(MaintenanceWorker.class, 1, TimeUnit.DAYS)
    .setConstraints(new Constraints.Builder().setRequiresBatteryNotLow(true).build())
    .build();
WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily_maintenance", ExistingPeriodicWorkPolicy.KEEP, daily);
```

### Manifest 权限（精简）

```xml
<uses-permission android:name="android.permission.CAMERA" android:required="false" />
<!-- 不声明 INTERNET / 存储读写权限 -->
```

---

