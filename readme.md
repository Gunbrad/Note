# Note APP

> 🤖 **AI 项目** - 本项目是我用 Claude 3.5 Sonnet + GPT-5 开发完成的

一款功能强大的 Android 表格笔记应用，专为高效数据管理和表格编辑而设计。采用现代化的 Material Design 界面，提供流畅的用户体验和丰富的表格编辑功能。

## ✨ 主要特性

### 📝 笔记本管理
- **瀑布流布局** - 美观的卡片式笔记本展示
- **实时搜索** - 支持笔记本标题的即时搜索过滤
- **智能排序** - 按创建时间、修改时间、标题等多种方式排序
- **置顶功能** - 重要笔记本可置顶显示
- **颜色标记** - 自定义笔记本颜色，便于分类管理

### 📊 表格编辑
- **专业表格界面** - 类似 DataGrip 的表格编辑体验
- **冻结首列** - 双 RecyclerView 布局，支持行头冻结
- **多种数据类型** - 支持文本、数字、日期、布尔值等多种单元格类型
- **行列管理** - 灵活的行列添加、删除、调整功能
- **同步滚动** - 行头与数据区域完美同步滚动
- **缩放支持** - 支持表格缩放，适应不同屏幕尺寸

### 🔍 搜索与筛选
- **全文检索** - 基于 SQLite FTS 的高性能全文搜索
- **即时过滤** - 搜索结果实时更新，无需等待
- **搜索节流** - 优化搜索性能，避免频繁查询

### 🎨 用户体验
- **Material Design 3** - 现代化的界面设计
- **深色模式支持** - 适应不同使用环境
- **流畅动画** - 精心设计的交互动画
- **响应式布局** - 适配不同屏幕尺寸

## 🛠️ 技术栈

### 架构模式
- **MVVM** - Model-View-ViewModel 架构
- **Repository Pattern** - 数据访问层抽象
- **LiveData** - 响应式数据绑定

### 核心技术
- **Android SDK** - Target API 36 (Android 16)
- **Room Database** - 本地数据持久化
- **SQLite FTS** - 全文搜索引擎
- **RecyclerView** - 高性能列表展示
- **Material Components** - Google 官方 UI 组件库

### 开发工具
- **Gradle** - 构建系统
- **ProGuard** - 代码混淆和优化
- **Android Studio** - 集成开发环境

## 📱 系统要求

- **最低版本**: Android 8.0 (API 26)
- **目标版本**: Android 16 (API 36)
- **架构支持**: ARM64, ARMv7, x86, x86_64
- **存储空间**: 约 3MB

## 🚀 安装使用

### 从源码构建

1. **克隆仓库**
   ```bash
   git clone https://github.com/your-username/Note.git
   cd Note
   ```

2. **打开项目**
   - 使用 Android Studio 打开项目
   - 等待 Gradle 同步完成

3. **构建应用**
   ```bash
   ./gradlew assembleRelease
   ```

4. **安装到设备**
   - 生成的 APK 位于 `app/build/outputs/apk/release/`
   - 手动安装到 Android 设备

### 快速开始

1. **创建笔记本** - 点击主页右下角的 + 按钮
2. **输入标题** - 为笔记本设置一个描述性的名称
3. **开始编辑** - 点击笔记本卡片进入表格编辑界面
4. **添加数据** - 点击单元格开始输入数据
5. **管理行列** - 使用工具栏添加或删除行列

## 📁 项目结构

```
app/src/main/
├── java/com/example/note/
│   ├── data/                 # 数据层
│   │   ├── entity/          # 数据实体
│   │   ├── dao/             # 数据访问对象
│   │   └── repository/      # 仓库模式实现
│   ├── ui/                  # 用户界面层
│   │   ├── main/           # 主页相关
│   │   ├── note/           # 笔记编辑相关
│   │   └── dialog/         # 对话框组件
│   └── utils/              # 工具类
└── res/                    # 资源文件
    ├── layout/             # 布局文件
    ├── values/             # 值资源
    └── drawable/           # 图片资源
```


### 已完成功能
- ✅ 基础架构搭建
- ✅ 数据库设计与实现
- ✅ 主页瀑布流界面
- ✅ 笔记本管理功能
- ✅ 基础表格编辑
- ✅ 搜索与排序功能




## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👨‍💻 开发者

- **Gunbrad** - 开发者
- **Claude 3.5 Sonnet** - AI 编程助手
- **GPT-5** - AI 协作伙伴

## 📞 联系方式

- 邮箱: loserben1314@gmail.com
- GitHub: [@Gunbrad](https://github.com/Gunbrad)

---

⭐ 如果这个项目对你有帮助，请给它一个 Star！