# 极简记账（MyApplication）

一个基于 **Java + Android 原生 SDK** 的本地记账应用。数据全部保存在设备本地的 SQLite 数据库中，不联网、不上传，适合日常快速记录收支并查看统计报表。

- 应用名称：极简记账
- 包名：`com.czc.myapplication`
- 版本：1.0（versionCode 1）

---

## 一、功能概览

底部导航共 4 个 Tab，另有编辑、分类管理、设置 3 个二级页面。

### 1. 今日（Tab1）
- 快速录入一笔收入或支出：金额、分类、备注
- 实时展示今日收入、今日支出、今日结余
- 今日流水列表，支持点击进入编辑

### 2. 流水（Tab2）
- 按日期分组展示全部历史记录
- 支持「全部 / 单日 / 单月」三种筛选模式
- 按备注关键字搜索
- 下拉刷新 + 分页无限滚动（每页 50 条）

### 3. 月报（Tab3）
- 当月收入、支出、结余汇总
- 支出 / 收入分类占比饼图
- 本月 vs 上月对比
- 近 6 个月收支趋势柱状图
- 支持切换年月

### 4. 年报（Tab4）
- 全年收入、支出、结余汇总
- 12 个月收支趋势折线图
- 本年 vs 去年对比
- 全年分类汇总
- 底部提供设置入口

### 5. 其他页面
- **记一笔 / 编辑账目**（`EditRecordActivity`）：新增或修改记录，支持日期时间选择、删除记录
- **分类管理**（`CategoryActivity`）：分别管理收入 / 支出分类，支持新增、删除；分类下已有记录时禁止删除，同名分类禁止重复添加
- **设置**（`SettingsActivity`）：
  - 导出全部数据为 CSV（UTF-8 带 BOM，Excel 可直接打开）
  - 进入分类管理
  - 清空全部记账记录（二次确认，分类保留）
  - 查看版本号

---

## 二、技术栈

| 项目 | 说明 |
| --- | --- |
| 语言 | Java 17 |
| 构建工具 | Gradle 8.7 + Android Gradle Plugin 8.2.2 |
| 编译 / 目标 SDK | compileSdk 34，targetSdk 34 |
| 最低支持版本 | minSdk 26（Android 8.0） |
| UI | AppCompat + Material Components，ViewBinding |
| 列表 | RecyclerView + SwipeRefreshLayout |
| 数据存储 | SQLite（`SQLiteOpenHelper` 手写 DAO，无 Room） |
| 图表 | 自绘 View：`PieChartView`、`BarChartView`、`LineChartView` |
| 测试 | JUnit4（单元测试）、Espresso（UI 测试） |

主要依赖：

```kotlin
androidx.appcompat:appcompat:1.6.1
com.google.android.material:material:1.11.0
androidx.constraintlayout:constraintlayout:2.1.4
androidx.swiperefreshlayout:swiperefreshlayout:1.1.0
junit:junit:4.13.2
androidx.test.ext:junit:1.3.0
androidx.test.espresso:espresso-core:3.7.0
androidx.test.espresso:espresso-contrib:3.7.0
```

---

## 三、项目结构

```
MyApplication/
├── app/
│   ├── build.gradle.kts              # 应用模块配置（SDK 版本、依赖、ViewBinding）
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # 4 个 Activity 声明
│       │   ├── java/com/czc/myapplication/
│       │   │   ├── MainActivity.java          # 主界面，底部 4 Tab 切换
│       │   │   ├── data/                      # 数据层
│       │   │   │   ├── DbHelper.java          # 数据库创建、升级、默认分类
│       │   │   │   ├── Record.java            # 记账记录实体
│       │   │   │   ├── RecordDao.java         # 记录增删改查 + 统计聚合
│       │   │   │   ├── Category.java          # 分类实体
│       │   │   │   └── CategoryDao.java       # 分类增删查
│       │   │   ├── ui/                        # 界面层
│       │   │   │   ├── TodayFragment.java     # 今日
│       │   │   │   ├── RecordsFragment.java   # 流水
│       │   │   │   ├── MonthReportFragment.java
│       │   │   │   ├── YearReportFragment.java
│       │   │   │   ├── EditRecordActivity.java
│       │   │   │   ├── CategoryActivity.java
│       │   │   │   ├── SettingsActivity.java
│       │   │   │   ├── RecordAdapter.java
│       │   │   │   └── RecordsAdapter.java
│       │   │   ├── util/                      # 工具类
│       │   │   │   ├── DateUtils.java         # 日期时间格式化与区间计算
│       │   │   │   ├── MoneyUtils.java        # 分/元转换、千分位格式化
│       │   │   │   └── YearMonthPicker.java   # 年月选择器
│       │   │   └── view/                      # 自定义图表
│       │   │       ├── PieChartView.java
│       │   │       ├── BarChartView.java
│       │   │       └── LineChartView.java
│       │   └── res/                           # 布局、图标、颜色、字符串、主题
│       ├── test/                              # 本地单元测试（JUnit4）
│       └── androidTest/                       # 仪器化 UI 测试（Espresso）
├── build.gradle.kts                  # 根构建脚本，声明 AGP 版本
├── settings.gradle.kts               # 仓库配置与模块声明
├── gradle.properties                 # Gradle / AndroidX 全局配置
├── gradle/wrapper/                   # Gradle Wrapper（8.7）
├── gradlew / gradlew.bat             # 跨平台构建脚本
└── local.properties                  # 本机 SDK 路径（不纳入版本控制）
```

---

## 四、数据库设计

数据库文件：`ledger.db`，版本 1，位于应用私有目录。

### category（分类表）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | INTEGER PK AUTOINCREMENT | 主键 |
| name | TEXT NOT NULL | 分类名称 |
| type | INTEGER NOT NULL | 1 = 收入，2 = 支出 |

首次安装时自动写入默认分类：

- 收入：工资、奖金、理财、红包、其他收入
- 支出：餐饮、交通、购物、居住、娱乐、医疗、教育、人情、通讯、其他支出

### records（记账记录表）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | INTEGER PK AUTOINCREMENT | 主键 |
| record_time | INTEGER NOT NULL | 记录时间（毫秒时间戳） |
| type | INTEGER NOT NULL | 1 = 收入，2 = 支出 |
| amount_cents | INTEGER NOT NULL | 金额，单位「分」，避免浮点误差 |
| category_id | INTEGER NOT NULL | 关联分类 id |
| remark | TEXT DEFAULT '' | 备注 |

索引：`idx_records_time(record_time)`、`idx_records_cat(category_id)`。

> 说明：月报 / 年报的统计数据均由 SQL 实时聚合（`SUM` / `GROUP BY`）得出，不额外缓存统计结果。

---

## 五、安装与运行环境

### 1. 环境要求

| 软件 | 版本要求 | 说明 |
| --- | --- | --- |
| JDK | **17**（必须） | AGP 8.x 要求 JDK 17 |
| Android Studio | Hedgehog (2023.1.1) 或更高 | 推荐使用最新稳定版 |
| Android SDK Platform | **API 34** | compileSdk / targetSdk |
| Android SDK Build-Tools | 34.x | 随 SDK 安装 |
| Android SDK Platform-Tools | 最新 | adb 调试 |
| Gradle | 8.7（由 Wrapper 自动下载） | 无需手动安装 |
| 设备 / 模拟器 | Android 8.0（API 26）及以上 | minSdk 26 |

### 2. 安装 JDK 17

- Windows：从 [Adoptium](https://adoptium.net/) 或 Oracle 下载 JDK 17 安装包，安装后配置 `JAVA_HOME`：

```powershell
# 检查版本，应输出 17.x
java -version
```

- macOS：

```bash
brew install openjdk@17
```

- Linux（Debian/Ubuntu）：

```bash
sudo apt install openjdk-17-jdk
```

### 3. 安装 Android Studio 与 SDK

1. 从 [Android 开发者官网](https://developer.android.com/studio) 下载并安装 Android Studio。
2. 首次启动时按向导安装 SDK；或在 **Settings → Languages & Frameworks → Android SDK** 中勾选：
   - **SDK Platforms**：Android 14.0（API 34）
   - **SDK Tools**：Android SDK Build-Tools、Android SDK Platform-Tools、Android Emulator
3. 创建模拟器：**Device Manager → Create Device**，选择 API 26 及以上的系统镜像。

### 4. 配置 SDK 路径

项目通过根目录的 `local.properties` 指定 SDK 位置（该文件已被 `.gitignore` 忽略，不会提交）：

```properties
sdk.dir=D\:\\Android\\Sdk
```

- 用 Android Studio 打开项目时会自动生成该文件；
- 若手动构建，请按本机实际路径修改，注意 Windows 路径中的反斜杠需转义；
- 也可改用环境变量 `ANDROID_HOME` / `ANDROID_SDK_ROOT`。

### 5. 导入并运行

**方式一：Android Studio（推荐）**

1. `File → Open`，选择项目根目录 `MyApplication`；
2. 等待 Gradle Sync 完成（首次会下载 Gradle 8.7 与依赖，需联网）；
3. 选择 `app` 运行配置，连接真机或启动模拟器；
4. 点击 **Run ▶** 安装并启动应用。

**方式二：命令行**

```powershell
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

生成的 APK 位于：

```
app/build/outputs/apk/debug/app-debug.apk
```

安装到已连接的设备：

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

或直接构建并安装：

```powershell
.\gradlew.bat installDebug
```

### 6. 运行测试

```powershell
# 本地单元测试（MoneyUtils / DateUtils，无需设备）
.\gradlew.bat testDebugUnitTest

# 仪器化 UI 测试（需连接设备或模拟器）
.\gradlew.bat connectedDebugAndroidTest
```

UI 测试 `LedgerFlowTest` 覆盖完整流程：录入 → 统计联动 → 编辑 → 删除 → 分类管理 → 清空数据。

### 7. 打包发布版

```powershell
.\gradlew.bat assembleRelease
```

> 当前 `release` 未配置签名与代码混淆（`isMinifyEnabled = false`），正式发布前需在 `app/build.gradle.kts` 中补充 `signingConfigs` 与 `proguard` 配置。

---

## 六、常见问题

**Q1：Gradle Sync 失败，提示 `Unsupported class file major version` 或 JDK 版本错误？**
A：确认 Gradle JDK 为 17。Android Studio 中路径：`Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK`，选择 JDK 17。

**Q2：提示找不到 Android SDK？**
A：检查 `local.properties` 中的 `sdk.dir` 是否指向本机真实 SDK 目录，或设置 `ANDROID_HOME` 环境变量。

**Q3：依赖下载缓慢或失败？**
A：`settings.gradle.kts` 中已配置 `google()` 与 `mavenCentral()`。网络受限时可在 `~/.gradle/init.gradle` 中配置国内镜像源。

**Q4：中文显示乱码？**
A：项目源码与资源文件均为 UTF-8 编码，`gradle.properties` 已设置 `-Dfile.encoding=UTF-8`。请确保编辑器使用 UTF-8 打开文件。

**Q5：数据存在哪里？如何备份？**
A：数据保存在应用私有目录的 `ledger.db` 中。可通过「设置 → 导出全部数据（CSV）」导出备份；卸载应用会清除全部数据。

---

## 七、开发约定

- 金额统一以 **分（long）** 存储与计算，仅在展示时通过 `MoneyUtils` 转换为元，避免浮点精度问题。
- 时间统一使用 **毫秒时间戳**，日期区间计算集中在 `DateUtils`，与 SQLite 的 `localtime` 修饰符保持一致。
- 界面使用 **ViewBinding** 访问控件，不使用 `findViewById`。
- 统计逻辑放在 DAO 层用 SQL 聚合完成，界面层只负责展示。
