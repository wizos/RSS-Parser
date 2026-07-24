# Loread 的 ROME 替代方案

## 1. 目标

本分支用于验证并逐步实现“以 `RSS-Parser` 替代 Loread 中 ROME 的订阅源解析职责”。

这里的“兼容”是业务能力兼容，不是复刻 ROME 的类名和对象树。Loread 最终只应在一个适配边界把 `RssChannel` / `RssItem` 转换成自身的 `Feed` / `Article`，避免让 ROME API 继续扩散，也避免 fork 长期背负一套无关的兼容层。

成功标准：

1. 用户报告的缺少 `wfw` 命名空间声明的 WordPress 订阅源可正常解析。
2. RSS 2.0、Atom、RDF 的现有能力不回退。
3. 保留 Loread 实际使用的 Media RSS 内容：分组、组描述、缩略图和组内多个 `media:content`。
4. 解析失败时能够稳定获得行号、列号和底层原因，便于生成“第 49 行，第 5 位”一类提示。
5. 默认修复行为保持兼容，同时允许调用方替换或关闭 XML 修复策略。
6. 不引入 Go/Rust/Zig FFI，不制造第二份跨语言对象图。
7. 核心库只负责解析，由调用方提供 XML 字符串或原始字节，不携带联网依赖。

## 2. 已确认的现状

### 2.1 用户报告订阅源

2026-07-24 实际抓取到的订阅源中，以下五个均包含未声明的 `wfw:commentRss` 前缀：

- `https://v2fy.com/feed/`
- `https://www.weisay.com/blog/feed`
- `https://www.chener.net/feed`
- `https://blog.52hyjs.com/feed/`
- `https://laozhang.org/feed/`

其中 `laozhang.org` 当前错误位置正是第 49 行附近。调整 HTTP 协议和请求头后，`runningcheese.com` 也已成功获取并通过解析；`kqwq.com` 仍返回空响应，不能把它的当前内容固化为事实。

ROME 的命名空间感知解析会把未声明前缀视为 XML 错误。RSS-Parser 的 Android/JVM 解析器目前不启用命名空间感知，因此这类输入无需先做字符串修复即可解析。我们仍会加入一个最小回归样本，防止未来上游调整解析器配置后问题复发。

### 2.2 RSS-Parser 相对 Loread 的缺口

当前库已经识别 `media:content` 和 `media:thumbnail`，但存在以下信息损失：

- 每篇文章只保留一个 `RawMediaContent`，后出现的内容会覆盖先出现的内容。
- 没有通用的 `media:group` 模型。
- 没有保存组级 `media:title`、`media:description` 和缩略图。
- Android/JVM 能从底层解析器得到错误位置，但 `RssParsingException` 没有公开这些字段。
- XML 修复逻辑固定在 `RssParser` 内部，调用方不能关闭、替换或按错误类型定制。

这几个缺口正好覆盖 Loread 当前从 `MediaEntryModule` 和 `ParsingFeedException` 使用的能力。

### 2.3 暂不应塞进核心解析器的能力

Loread 还使用 ROME 完成：

- OPML 导入；
- 自定义订阅规则预览时的 RSS 2.0 输出。

它们不是“解析远程订阅源”的热路径。为降低首轮替换风险，本分支第一阶段不在 `rssparser` 核心模块中复制完整 OPML 和 WireFeed 输出体系。Loread 可在迁移解析热路径后暂时保留 `rome-opml` / `rome` 的输出用途；确认解析替换达到预期后，再决定增加轻量独立模块，或直接用 Loread 已有的 Jsoup/字符串生成逻辑完成这两项工作。

## 3. 第一阶段设计

### 3.1 Media RSS 数据模型

新增公共模型：

```kotlin
data class RssMediaGroup(
    val title: String?,
    val description: String?,
    val thumbnail: String?,
    val contents: List<RawMediaContent>,
)
```

并在 `RssItem` 增加：

```kotlin
val mediaContents: List<RawMediaContent> = emptyList()
val mediaGroups: List<RssMediaGroup> = emptyList()
```

兼容原则：

- 保留现有 `rawMediaContent` 字段，不立即制造破坏性迁移。
- `rawMediaContent` 继续表示旧 API 可见的单个内容。
- 新代码分别读取顶层 `mediaContents` 和显式 `mediaGroups`；不在模型中复刻 ROME 的 `Module`、`Reference`、`Metadata` 层次。
- 不把未分组的 `media:content` 伪装成 `media:group`，保持源文档语义。

### 3.2 集中式状态管理

Media RSS 状态统一放进 `ChannelFactory`：

- `startMediaGroup()`
- `addMediaContent(url, type, medium)`
- `setMediaGroupTitle(value)`
- `setMediaGroupDescription(value)`
- `setMediaGroupThumbnail(url)`
- `endMediaGroup()`

Android XML Pull、JVM SAX、Apple `NSXMLParser` 和 Web 映射层只负责把平台事件转交给这些方法，不各自维护一套模型组装逻辑。这样可以让多平台输出保持一致，也减少以后跟随上游时的冲突面。

首轮仅保存 Loread 确实消费的 URL、MIME type、medium、标题、描述和缩略图。宽高、时长、码率、评分等字段等有真实调用方后再增加。

### 3.3 可诊断异常

在保留现有两参数构造方式的前提下，为 `RssParsingException` 增加构造时确定的只读公开信息：

```kotlin
val lineNumber: Int?
val columnNumber: Int?
```

平台映射：

- Android：`XmlPullParserException.lineNumber / columnNumber`
- JVM：`SAXParseException.lineNumber / columnNumber`
- Apple：当前 Kotlin/Native 公共 Foundation 绑定未暴露 locator 属性，首阶段返回 `null`
- Web：底层库没有可靠位置时返回 `null`

异常仍保留原始 `cause`，Loread 可按“位置 + cause.message”生成用户提示。

### 3.4 可配置恢复策略

新增公共函数接口：

```kotlin
fun interface XmlFeedRecovery {
    fun repair(xml: String, failure: RssParsingException): String?
}
```

规则：

- 默认策略封装当前库已有的修复行为，保持现有调用者行为不变。
- 返回 `null` 表示不重试；返回字符串表示用修复后的内容重试一次。
- 各平台 Builder 接受同一个可选 `recovery` 参数。
- 不做无限重试，不在核心库硬编码 Loread 域名。
- Loread 如需修复特定前缀，可以在自身适配层注入策略；但本次报告的 Android 热路径应先依靠解析器原生宽容能力。

## 4. 测试方案

### 4.1 回归样本

加入最小化、无站点正文的测试 XML：

1. 未声明 `wfw` 前缀，验证 Android/JVM 兼容行为。
2. 一个 item 含两个 `media:group`，每组含描述、缩略图和多个 `media:content`。
3. `media:content` 不在 group 内，验证隐式分组。
4. 人工构造的非法 XML，验证 JVM/Android 异常位置不为空。
5. 自定义 recovery 修复后成功、返回 `null` 时保留首次异常。

测试样本只保留触发条件，不提交抓取到的整份第三方订阅内容。

### 4.2 验证命令

首轮至少执行：

```bash
./gradlew :rssparser:jvmTest
./gradlew :rssparser:testAndroidHostTest
./gradlew :rssparser:apiCheck
```

并编译可在当前 Apple Silicon 主机运行的 Apple 目标。Web 的 XML 序列化结构与流式解析器不同；若第一阶段不能在不扩大改动的前提下完整支持多组 Media RSS，应明确保留为后续项，而不能让 Android/JVM 的替换被阻塞。

### 4.3 Loread 验收

库内测试通过后，在 Loread 的独立迁移分支进行：

- 同一份 XML 分别交给 ROME 和 RSS-Parser。
- 比较 feed 标题、链接、文章数量、guid、日期、正文、附件和 Media RSS 输出。
- 用用户报告的七个 URL 做在线冒烟测试。
- 观察峰值内存与解析耗时；只保留最终 Loread 对象，避免同时长期持有两套解析结果。

Loread 迁移不属于本仓库首阶段改动，防止库实现和应用切换混在一个不可独立回滚的提交里。

## 5. 分阶段交付

### 阶段 A：本分支立即实现

- 缺少 `wfw` 声明的回归测试。
- Media RSS 分组和多内容模型。
- Android/JVM/Apple 解析接入；Web 在可控范围内同步。
- 行号、列号异常信息。
- 可配置的单次 XML recovery。
- 公共 API dump、README 说明和测试。

### 阶段 B：Loread 解析热路径迁移

- 在 Loread 新建单一 `RssParser -> Feed/Article` 适配器。
- 先只替换本地 RSS 后端的远程 XML 解析。
- 保留开关或独立回滚提交，完成 ROME / RSS-Parser 对照测试。
- 验收通过后移除 `rome-modules` 和解析路径上的 `rome` 引用。

### 阶段 C：清理剩余 ROME 用途

- OPML：优先复用 Loread 已有 Jsoup 方案补齐导入；确有跨项目复用价值时再建独立轻量模块。
- RSS 2.0 输出：为自定义订阅预览实现只覆盖现有字段的生成器。
- 清空最后的 ROME 引用后再移除依赖。

## 6. 内存与性能约束

- 继续使用 Kotlin/JVM 流式解析器，不引入 JNI/FFI。
- 删除核心库的 OkHttp/Ktor 依赖和 URL 下载入口；新增 `parse(ByteArray, baseUrl)`，让 XML 解析器直接感知原始字节、编码声明和相对链接上下文。
- Media RSS 只新增实际存在的数据列表；普通订阅项的 `mediaGroups` 使用共享空列表。
- recovery 仅在首次解析失败后才把 XML materialize 为字符串，与当前行为一致。
- Loread 迁移完成后不能同时缓存 `SyndFeed` 与 `RssChannel`。
- 基准比较应分开记录下载、解析、适配三个阶段，避免把网络波动误判为解析器性能。

## 7. 回滚方式

当前工作树位于 `codex/loread-compatibility` 分支，`master` 保持在 fork 的原始基线。为方便先审查差异，本轮实现暂未自动提交；确认后应把本轮文件提交到当前分支。此后若验证不达预期：

```bash
git switch master
```

在提交前如需临时切换，应先包含未跟踪文件一起 stash，避免未提交改动跟随工作树：

```bash
git stash push -u -m "rss-parser loread compatibility"
git switch master
```

Loread 的应用迁移将在另一个独立分支进行，因此放弃本库方案不会牵连当前应用主线。

## 8. 明确不做

- 不提供 ROME 包名或类型的兼容壳。
- 不完整实现 Media RSS 全规范。
- 不在核心库中加入站点白名单或域名特判。
- 不在第一阶段引入 Go/Rust/Zig 原生库。
- 不把 OPML、RSS 输出和远程订阅解析强行合并成一个大模块。

## 9. 阶段 A 实施记录

阶段 A 已在 `codex/loread-compatibility` 分支完成：

- 新增 `RssMediaGroup`，完整保留组标题、描述、缩略图和多个 `media:content`。
- 顶层 `media:content` 保存在 `RssItem.mediaContents`，不再创建隐式分组。
- 保留旧 `rawMediaContent` 字段及既有图片、音频、视频推断行为。
- Android、JVM、Apple 和 Web 共用 `ChannelFactory` 的 Media RSS 组装逻辑。
- `RssParsingException` 对外提供可选行号、列号，Android/JVM 已接入底层解析器位置。
- 新增可替换、可关闭且最多重试一次的 `XmlFeedRecovery`。
- 核心库移除自行联网能力及 OkHttp/Ktor 依赖，保留 `String`、原始 `ByteArray` 和 `baseUrl` 解析入口。
- 该变更按破坏性 API 处理，版本标记为 `7.0.0-SNAPSHOT`；默认 Gradle 构建只包含 `:rssparser`，避免 Loread 复合构建配置示例工程。
- 增加缺少 `wfw` 声明、Media RSS 分组、多内容和 recovery 的回归测试。
- README 与公共 API dump 已同步。

验证结果：

```text
:rssparser:jvmTest             通过
:rssparser:testAndroidHostTest 通过
:rssparser:jsBrowserTest       通过
:rssparser:macosArm64Test      通过
:rssparser:apiCheck            通过
```

用户提供的七个在线地址中，当前可下载的六个均在关闭 recovery 的情况下通过 JVM 与 Android Host 解析；`kqwq.com` 因服务器返回空响应未能在线验证。在线结果只用于冒烟验证，稳定回归仍由仓库中的最小 XML 样本承担。

Loread 应用侧的解析适配仍按阶段 B 单独实施，避免把库能力建设和应用切换绑定在同一个回滚单元。
