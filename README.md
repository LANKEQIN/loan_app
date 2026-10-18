# 超级借钱（SuperLoan）

> 个人使用的**完全离线**分期理财小工具 —— Android 原生 App
> Kotlin + Jetpack Compose (Material 3) + DataStore ｜ 无任何网络权限 ｜ 跟随系统深色模式

---

## 一、这是什么

一个给自己用的离线金融计算器，四个页签通过底部导航切换，另有一层藏在账单列表底部的「作死模式」彩蛋：

| 模块 | 干什么 |
|---|---|
| **分期账单** | 管理各平台分期账单（京东白条 / 抖音月付 / 花呗 / 信用卡分期…），按月勾选还款打卡 |
| **IRR 计算器** | 输入本金 / 期数 / 每期还款额，算出真实月利率与年化利率，并给出利率评级 |
| **月供计算器** | 输入本金 / 期数 / 利率，按三种还款方式算月供、总息费与完整还款计划表 |
| **设置** | 主题与配色方案、账单默认行为、计算器默认项、数据管理、关于 |

核心原则：**完全离线，不申请任何网络权限，所有数据只存在本机。**

> 另外还藏了一层「**作死模式 · 体验服**」：一个模拟网贷 + 剁手的离线小游戏，
> 入口伪装成账单列表底部的一张网贷广告卡，数据与真实账单完全隔离。详见下文第 5 小节。

---

## 二、功能一览

### 1. 分期账单

- 列表卡片展示：平台名 + 评级标签、`商品名 · 月供 × 期数`、本金 · 含息/费金额、出账/还款日、该笔账单的 IRR 月利率 · 年化
- 卡片可展开全部期数清单，每期一个复选框（月份 + 金额），勾选即打卡；**当前月份高亮并带「本月」标签**
- 还款进度条：`已还 X / N 期` + 百分比
- 搜索框按「平台 + 商品名」不区分大小写实时过滤，显示 `找到 X 笔 / 共 Y 笔`
- 新增 / 编辑 / 删除（删除二次确认，并连带删除该账单的还款记录）
- 编辑时期数或起始月被改小，会自动清理超出新周期的已还记录
- 每期等额，月供 = 总还款额 ÷ 期数
- 首次启动写入两条种子数据（仅当本地无数据时）

### 2. IRR 计算器

- 输入：商品本金、分期期数、每期还款金额
- 输出：月利率（IRR）、年化利率、总还款、总息费、利率评级
- 二分法精确求解，参数见下文「算法设计」

### 3. 月供计算器

- 输入：本金、期数、利率、利率类型（年利率 / 月利率）、还款方式（等额本息 / 等额本金 / 等本等费）
- 输出：评级、月供（或首月/末月/每月递减）、总还款、总息费
- 等本等费额外显示：`真实年化 IRR xx.xx%（名义 yy.yy%）`
- 方式对比：`较等额本金：多付利息 ¥x.xx`，差值 < 0.005 显示「基本持平」
- 完整还款计划表：期数 / 月供 / 本金 / 利息 / 剩余本金，可滚动
- 切换利率类型或还款方式时立即重算

### 4. 设置

| 分组 | 选项 |
|---|---|
| **外观** | 主题模式（跟随系统 / 浅色 / 深色）；配色方案（动态取色 Material You + 7 套内置色板） |
| **账单** | 列表默认展开全部期数；新增账单起始月默认当前月 |
| **计算器** | 月供计算器默认还款方式 |
| **数据** | 恢复示例数据；清空全部账单（均需二次确认） |
| **关于** | 应用名、版本、联网权限、数据存储位置说明 |

### 5. 作死模式 · 体验服（彩蛋）

账单列表底部有一张**伪装成网贷广告**的卡片（「限时专属额度 ¥200,000 · 日息低至 0.03% · 立即领取」）。
点它 → 假风控审核（进度条走完 → 「审核不通过」）→「强制放款（反正也是假的）」→ 进入全屏体验服。

体验服是一层**全屏覆盖**，会把底部导航一起盖住，退出后 App 还是原来那个工具，主结构零改动。

| 页签 | 内容 |
|---|---|
| **借钱** | 5 款画风各异的「产品」（亲友贷 0% → 闪电借 34.8%），拖滑块选额度，一键放款到账 |
| **我的** | 负债总览（余额 / 剩余应还 / 已逾期 / 罚息 / 征信分 / 净资产）+ 时间控制 + 每笔借款的还款卡 |
| **短信** | 催收收件箱（筛选：全部 / 催收 / 亲友）+ 来电记录；未读红点挂在页签上 |
| **商城** | 14 件商品，分「正经区」（iPhone / 显卡 / 无人机…）与「整活区」（虚拟老婆体验卡 / 赛博功德 / 空气币…） |

玩法要点：

- **时间跟随现实**：虚拟日期 = 开局日期 + 真实流逝天数 + 手动快进天数。放着不管，账单照样一天天到期。
- **可手动快进**：`+1 天 / +7 天 / +30 天`（单次上限 90 天），另有「回到现实」按钮清掉快进天数。
- **逾期滚罚息**：罚息日利率 = `max(年化日利率 × 1.5, 日万分之五)`，按每期各自起算，一次性快进也能算准。
- **以贷养贷**：余额不够还款时不会拦你，只会提示「要不，再借一笔？」。
- **破产结局**：逾期满 3 期 → 全屏「你 已 破 产」+ 破产清算单 + 催收短信（最多 7 条，随逾期期数逐级递增）
- **清零重来**：清空余额 / 借款 / 消费记录，开局日期重置为今天，破产次数 +1（战绩留存）。
- **数据隔离**：全部存在独立的 `sim_store_json` 键里，破产清零**绝不会**动到真实账单。

#### 催收演出：短信 + 电话轰炸 + 亲友被骚扰

这是整个彩蛋的高潮，分三个渠道同时发力，触发线随逾期天数升级：

| 逾期阶段 | 短信 | 电话 | 亲友 |
|---|---|---|---|
| 第 1 天 | 平台逾期提醒 | — | — |
| 第 3 天 | 进入催收流程通知 | 未知号码来电在排队 | — |
| 第 7 天 | 已联系紧急联系人 | 400 号码继续打 | 表姐：怎么有人打电话说你欠钱？ |
| 第 15 天 | 已向 12 位通讯录好友群发 | 催收专线轮番上 | 老同学：我这个月接了 6 个找你要钱的电话 |
| 第 30 天 | 法务部诉讼警告 | 某某金融 · 法务部 | 同事：有人打电话到公司前台自称法务部 |

- **短信收件箱**：消息按虚拟日期陆续到达，带发件人、到达时刻（确定性伪随机）与「新」标记。
  收件箱顶部有一句话说明「全部是虚构内容」，并提示现实中遇到暴力催收应依法维权。
- **来电覆盖层**：仿系统来电界面（大圆头像 + 「正在呼叫…」+ 绿色接听 / 红色拒接）。
  接听后会一句一句蹦出催收话术（**纯文字，不放音频**），随时可挂断。
  快进一次性欠下多通电话时，处理完一通立刻弹下一通 —— 这就是字面意义上的电话轰炸；
  右上角另有「全部拒接（世界清净了）」留一条活路。
- **响铃与震动**：来电时播放系统默认铃声并震动（长-短-长-短-长循环）。手机处于静音 / 震动模式时自动不响铃，只震动。
- **消息不会赖掉**：短信与来电都是「事件」，一旦投递就永久保留 —— 即使后来把钱还上，
  也还能在收件箱里翻到当初收到过什么。投递靠一个水位线增量补投，所以快进 1 天和快进 90 天结果完全一致。

模拟器里所有年化利率都沿用全项目统一的复利折算口径，评级标签直接复用 `rateLevelOf()`，
所以「闪电借」会老老实实地被打上「拉完了（五级）」。

配色方案一览：

| 色板 | 种子色 |
|---|---|
| 动态取色 | 跟随系统壁纸（Android 12+，低版本自动回退品牌绿） |
| 品牌绿 | `#1F6F4A` |
| 科技蓝 | `#2563EB` |
| 暮紫 | `#7C3AED` |
| 暖橙 | `#EA580C` |
| 玫红 | `#DB2777` |
| 青碧 | `#0D9488` |
| 石墨 | `#475569` |

每套色板只存一个种子色，运行时由 `buildColorScheme()` 按种子色相推导出完整的浅色 / 深色 Material 3 配色，因此新增色板只需加一行枚举。

---

## 三、共用业务规则

### 利率评级（三个计算器模块 + 作死模式共用）

| 年化利率 | 等级 | 说明 | 配色 |
|---|---|---|---|
| < 4% | 夯（一级） | 神仙利率，闭眼借 | `#16A34A` 绿 |
| 4% – 8% | 顶级（二级） | 银行良心价 | `#4D9E1F` 黄绿 |
| 8% – 15% | 人上人（三级） | 正常消费贷 | `#C08A00` 琥珀 |
| 15% – 24% | NPC（四级） | 网贷韭菜区 | `#E2600C` 橙 |
| ≥ 24% | 拉完了（五级） | 快跑，是高利贷 | `#DC2626` 红 |

### 格式化规则

- 金额：`¥` + 保留两位小数，如 `¥9.36`
- 利率：百分数保留两位小数，如 `13.15%`
- 月份：内部统一 `YYYY-MM`，展示为 `2026 年 8 月`

### 边界规则

- **总还款额 ≤ 本金时视为免息**：IRR 返回 0，且**不显示评级**
- **年化 = (1 + 月 IRR)^12 − 1**（复利折算，不是简单 ×12）
- 等额本金末期本金取剩余本金、等额本息末期同理，用于消除浮点除不尽的误差
- **作死模式沿用同一口径**：月利率 = `(1 + 年化)^(1/12) − 1`，日利率 = `(1 + 年化)^(1/365) − 1`；
  月供按等额本息，罚息日利率 = `max(日利率 × 1.5, 日万分之五)`

---

## 四、技术栈

| 项 | 选择 |
|---|---|
| 语言 | Kotlin 2.4.21 |
| UI | Jetpack Compose（Material 3），单 Activity + 底部导航 4 Tab；作死模式为其上的一层全屏覆盖 |
| 存储 | Preferences DataStore 存 JSON（手写 org.json 编解码，免去序列化插件） |
| 构架 | Application 持有单例仓库 + ViewModel(StateFlow) + 纯函数算法层 |
| 构建 | Gradle 8.13 + Android Gradle Plugin 8.13.2 |
| SDK | compileSdk 36 / targetSdk 36 / minSdk 26 |

依赖版本（与 compileSdk 36 兼容的版本线）：

```
androidx.compose:compose-bom:2026.02.01      # ui 1.10.4 / material3 1.4.0
androidx.compose.material:material-icons-core:1.7.8
androidx.core:core-ktx:1.18.0
androidx.activity:activity-compose:1.11.0
androidx.lifecycle:lifecycle-*:2.9.4
androidx.datastore:datastore-preferences:1.1.7
```

---

## 五、环境要求与构建

### 环境

- JDK 21（本机：`D:\Java21`）
- Android SDK（本机：`D:\AndroidStudioSDK`，需含 `platforms;android-36` 与 `build-tools;36.0.0`）
- `local.properties` 里写 `sdk.dir=D:\\AndroidStudioSDK`

### 用 Android Studio

直接 `Open` 本目录 → Sync → Run。工程是标准 Gradle 结构，`gradlew` 与 wrapper 已生成。

### 命令行

```powershell
$env:JAVA_HOME="D:\Java21"
$env:ANDROID_HOME="D:\AndroidStudioSDK"
$env:ANDROID_SDK_ROOT="D:\AndroidStudioSDK"
.\gradlew.bat assembleDebug
```

产物：`app\build\outputs\apk\debug\app-debug.apk`

### 安装到手机

```powershell
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
```

---

## 六、工程结构

```
d:\loan_app\
├─ settings.gradle.kts / build.gradle.kts / gradle.properties
├─ gradlew(.bat) + gradle\wrapper\
├─ local.properties                      ← 本机 SDK 路径（已 gitignore）
└─ app\
   ├─ build.gradle.kts
   └─ src\main\
      ├─ AndroidManifest.xml             ← 不含 INTERNET；仅声明 VIBRATE（模拟来电震动）
      ├─ res\
      │  ├─ drawable\ic_launcher_foreground.xml   （¥ 图标，纯矢量）
      │  ├─ mipmap-anydpi-v26\ic_launcher*.xml    （自适应图标）
      │  ├─ values\ strings / colors / themes
      │  ├─ values-night\ colors / themes          （深色模式宿主主题）
      │  └─ xml\ backup_rules / data_extraction_rules
      └─ java\com\superloan\app\
         ├─ SuperLoanApp.kt             Application，持有单例仓库
         ├─ MainActivity.kt             单 Activity 宿主，enableEdgeToEdge
         ├─ core\                       纯函数算法层（无 Android 依赖）
         │  ├─ Format.kt                金额 / 利率 / 月份 格式化 + 月份运算
         │  ├─ RateRating.kt            五级利率评级枚举 + rateLevelOf()
         │  ├─ IrrCalculator.kt         二分法 IRR + 复利年化 + IrrResult
         │  ├─ LoanCalculator.kt        三种还款方式 / 计划表 / 方式对比 / loanLevel()
         │  ├─ SimGame.kt               作死模式：产品 / 商品目录 + 到期推导 + 罚息与破产判定
         │  └─ SimCollection.kt         作死模式：催收短信 / 来电 / 亲友骚扰的时间线生成
         ├─ data\
         │  ├─ AppDataStore.kt          共用的 DataStore 实例（账单 / 设置 / 模拟器同一文件）
         │  ├─ Bill.kt                  账单模型 + newBillId() + BillStoreData
         │  ├─ BillJsonCodec.kt         org.json 手写编解码
         │  ├─ BillRepository.kt        DataStore 读写 + 种子数据 + 增删改打卡 + 清空/重置
         │  ├─ AppSettings.kt           设置模型 + ThemeMode 枚举
         │  ├─ SettingsJsonCodec.kt     设置项 org.json 编解码
         │  ├─ SettingsRepository.kt    设置读写（原子「读改写」）
         │  ├─ SimModels.kt             SimStoreData 存档模型 + newSimLoanId()
         │  ├─ SimJsonCodec.kt          模拟器存档 org.json 编解码
         │  └─ SimRepository.kt         模拟器读写（borrow / buy / repay / 快进 / 催收投递 / 清零重来）
         └─ ui\
            ├─ AppRoot.kt               内容区 + 底部导航（4 Tab）+ 体验服全屏覆盖
            ├─ LocalAppSettings.kt      全局设置 CompositionLocal
            ├─ theme\Theme.kt           深浅色与配色装配
            ├─ theme\Palettes.kt        色板枚举 + 由种子色生成 Material 3 配色
            ├─ components\UiComponents.kt  评级标签 / 卡片 / 键值行 / 选项芯片 / 数字输入框
            ├─ bills\
            │  ├─ BillsScreen.kt        搜索 + 列表 + 删除确认 + 表单入口 + 广告位入口
            │  ├─ BillCard.kt           账单卡片 + 期数清单 + 进度条
            │  ├─ BillEditorDialog.kt   全屏新增/编辑表单（校验失败 Snackbar + 聚焦）
            │  ├─ MonthPickerDialog.kt  年月选择弹窗
            │  └─ BillsViewModel.kt     状态持有者
            ├─ irr\IrrScreen.kt
            ├─ loan\LoanScreen.kt
            ├─ settings\
            │  ├─ SettingsScreen.kt     外观 / 账单 / 计算器 / 数据 / 关于
            │  └─ SettingsViewModel.kt  设置状态持有者
            └─ sim\                     作死模式 · 体验服
               ├─ SimRoot.kt           全屏容器 + 假风控审核页 + 顶部状态条 + 四页签导航 + 来电调度
               ├─ SimAdBanner.kt       伪装广告位卡片（体验服入口）
               ├─ SimMarketPage.kt     借钱页：产品列表 + 额度滑块 + 放款
               ├─ SimDebtPage.kt       我的负债页：仪表盘 + 时间控制 + 还款卡
               ├─ SimMessagePage.kt    短信页：催收收件箱 + 来电记录
               ├─ SimCallOverlay.kt    来电覆盖层：响铃 / 接听后对话 / 拒接
               ├─ SimShopPage.kt       商城页：正经区 + 整活区
               ├─ SimBankruptPage.kt   破产结局页：清算单 + 催收短信 + 清零重来
               ├─ SimRingtone.kt       来电铃声与震动（VIBRATE 权限的唯一用途）
               └─ SimViewModel.kt      状态持有者（存档 + 真实日期 → 派生 SimStatus）
```

---

## 七、数据与持久化

账单模型（`Bill`）：

```kotlin
Bill {
  id: String            // bill_ + 时间戳36进制 + 6位随机串
  platform: String      // 平台，必填
  item: String          // 商品/用途，必填
  principal: Double     // 本金，> 0
  totalAmount: Double   // 总还款额，≥ principal
  months: Int           // 期数，≥ 1（实现上限 600）
  startMonth: String    // 首期账单月份 YYYY-MM
  billingDay: Int?      // 每月出账日 1-31，可空
  repaymentDay: Int?    // 每月还款日 1-31，可空
}
// endMonth 不落库，始终由 startMonth + (months - 1) 推导
// monthlyPayment = totalAmount / months
```

还款记录与账单分开保存：

```kotlin
BillStoreData {
  bills: List<Bill>
  paid: Map<String, List<String>>   // paid[billId] = ["2026-08", "2026-09", ...]
  seeded: Boolean                   // 是否已写过种子，避免清空后被反复写入
}
```

存储介质：`Preferences DataStore`，文件名 `super_loan`，内含三个键，彼此完全独立：

| 键 | 内容 |
|---|---|
| `bill_store_json` | 上述 `BillStoreData` 的 JSON（真实账单） |
| `app_settings_json` | 应用设置 `AppSettings` 的 JSON |
| `sim_store_json` | 作死模式存档 `SimStoreData` 的 JSON（纯虚构） |

作死模式存档结构：

```kotlin
SimStoreData {
  startedToday: String            // 开局日期 YYYY-MM-DD，真实时间轴的锚点
  fastForwardDays: Int            // 手动快进累计天数
  wallet: Double                  // 手上余额
  loans: List<SimLoan>            // 模拟借款（产品 / 本金 / 年化 / 期数 / 月供 / 起始天 / 还款日 / 已还期数）
  purchases: List<SimPurchase>    // 模拟消费记录
  bankruptcyCount: Int            // 历史破产次数（战绩）
  messages: List<SimMessage>      // 已投递的催收短信（事件，投递后永久保留）
  calls: List<SimCall>            // 已投递的来电记录（含接听 / 拒接状态）
  readMessageCount: Int           // 已读条数，未读数 = 消息总数 − 它
  watermarkDay: Int               // 催收投递水位：已投递到第几个虚拟天
}
```

关键设计分两层：

1. **纯派生不进存档**：到期期数、罚息、征信分、破产判定全部由 `simStatus(存档, 真实日期)` 现算。
   这样一次性快进 90 天也不会漏算或重复结算，存档里不会留下自相矛盾的状态。
2. **事件必须落库**：短信与来电是「已经发生过的事」，一旦投递就永久保留。
   投递靠 `watermarkDay` 这条水位线增量补投 —— 只生成 (水位, 当前天] 区间内的新事件，
   并且每条事件都有**确定性 id**（由「借款 id + 期数 + 事件类型」拼成），写库前再按 id 去重一次。
   于是快进 1 天与快进 90 天结果完全一致，也永远不会重复收到同一条催收短信。

设置项结构：

```kotlin
AppSettings {
  themeMode: ThemeMode            // SYSTEM / LIGHT / DARK
  paletteId: String               // 对应 ui.theme.PaletteId.id
  billListExpanded: Boolean       // 账单列表默认展开全部期数
  newBillStartMonthCurrent: Boolean  // 新增账单起始月默认当前月
  defaultRepaymentType: String    // 月供计算器默认还款方式（RepaymentType.name）
}
```

数据损坏时解码自动降级为空数据 / 默认设置，保证 App 仍能启动。

---

## 八、隐私与权限

`AndroidManifest.xml` 中**不含任何网络权限**（无 `INTERNET` / `ACCESS_NETWORK_STATE`），
应用无法联网，数据不可能外泄。

唯一的权限是 `VIBRATE` —— 普通权限，安装即授予、没有运行时弹窗，仅用于
「作死模式」里模拟来电的震动效果，不读取任何隐私数据，也不联网。
如果不想要震动（比如夜里怕被吓到），删掉 Manifest 里那一行 `<uses-permission>` 即可回到零权限版本。

---

## 九、验收用例

| # | 输入 | 预期 | 结果 |
|---|---|---|---|
| T1 | IRR：本金 198.07，24 期，每期 9.3629（总还款 224.71） | 月 IRR ≈ 1.0352%，年化 ≈ 13.1541%，总息费 ¥26.64，人上人（三级） | 通过 |
| T2 | IRR：本金 1690.86，12 期，总还款 1783.70 | 月 IRR ≈ 0.8321%，年化 ≈ 10.4549% | 通过 |
| T3 | 月供：本金 6000，年化 7.2%，12 期，等额本息 | 月供 ¥519.71，总利息 ¥236.57，较等额本金多付 ¥2.57 | 通过 |
| T4 | 月供：本金 6000，年化 7.2%，12 期，等额本金 | 首月 ¥536.00，末月 ¥503.00，每月递减 ¥3.00，总利息 ¥234.00 | 通过 |
| T5 | 月供：本金 6000，月费率 0.75%，12 期，等本等费 | 每月 ¥545.00，总息费 ¥540.00，名义年化 9%，真实年化 ≈ 17.4778%，NPC（四级） | 通过 |
| T6 | 免息：本金 1000，总还款 1000 | IRR = 0，不显示评级；月供总利息 = 0 | 通过 |
| T7 | 账单：新增账单总还款 100 < 本金 200 | 拦截提交并提示 | 通过 |

> 说明：T1 中 `9.36` 是 `224.71 ÷ 24 = 9.36291666…` 的四舍五入值。想复现 13.1541%，IRR 输入框里填 `9.3629` 或更多位小数；若填 `9.36`，得到的是数学上正确的 13.1189%。

> T1–T7 全部是**算法级对账**（三个计算器模块），在 Python 原型与最终 Kotlin 实现中两次核对通过。
> 「作死模式」属于演出型玩法，没有可对账的期望数值，**不纳入本表**；该部分目前只验证到
> `assembleDebug` 编译通过并产出 APK，**尚未做实机运行时验收**，逐条检查清单见
> [计划总览书.md](计划总览书.md) 第九节。

---

## 十、已知取舍与版本说明

1. **IRR 输入精度**：`每期还款金额` 允许填多位小数（不做两位小数截断），以复现精确的年化结果；界面已给出提示文案。
2. **期数上限 600**：需求只写「期数 ≥ 1」，实现额外加了 1–600 的上限，防止误输入超大值导致期数列表 / 计划表渲染卡死。
3. **版本线锁定原因**：本机 SDK 只有 `android-36.1`。2026 年中的 androidx 版本（compose-ui 1.12.x、core-ktx 1.19.x、lifecycle 2.11.x）要求 **AGP 9.1+ 且 compileSdk 37**；所以锁定为 AGP 8.13.2 + compileSdk 36 + Compose BOM 2026.02.01 这套组合。若日后升级 AGP 9.x 并装上 `platforms;android-37`，可整体上调。
4. **图标库需显式引入**：新版 `material3` 不再传递依赖 `androidx.compose.material:material-icons-core`，必须在 `app/build.gradle.kts` 中显式声明，否则 `Icons.Filled.*` 全部报未解析。
5. **构建期警告**：Gradle 8.13 对 Kotlin 2.4 而言偏旧（Kotlin 2.5 起要求 ≥ 8.14.4），仅告警不影响构建。要消除可把 wrapper 升到 Gradle 8.14.4+。
6. **Gradle 发行版拉取慢**：`gradlew` 首次运行要从 `services.gradle.org` 下 137 MB 的发行版，
   而 wrapper 的 `networkTimeout=10000` 在这种网络下很容易直接超时失败。若遇到
   `Downloading ... failed: timeout (10000ms)`，可手动从国内镜像预置到 wrapper 缓存目录，
   无需改动仓库中的 `gradle-wrapper.properties`：

   ```powershell
   $dir="$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d"
   Invoke-WebRequest "https://mirrors.cloud.tencent.com/gradle/gradle-8.13-bin.zip" -OutFile "$dir\gradle-8.13-bin.zip"
   Expand-Archive "$dir\gradle-8.13-bin.zip" -DestinationPath $dir -Force
   New-Item "$dir\gradle-8.13-bin.zip.ok" -ItemType File -Force
   ```

   装好之后用 `.\gradlew.bat assembleDebug --offline` 即可离线构建（依赖已在本机缓存）。
