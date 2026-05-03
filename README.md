# Project_Necore (icather.pages.dev) 项目大纲

## 1. 项目概览

- **项目名称**: Project_Necore (包名: `icather.pages.dev`)
- **项目类型**: Android 应用程序
- **主要开发语言**: Kotlin
- **核心功能**: 这是一个 AI 聊天与大语言模型（LLM）客户端应用。支持与不同的 AI API 服务提供商通信，支持 OCR 功能，具备本地聊天持久化能力，并支持通过动态加载 JSON 插件来扩展各家大语言模型协议。

## 2. 技术栈与架构

- **整体架构**: MVVM (Model-View-ViewModel) 结合单向数据流 (UDF)。
- **构建系统**: 单仓库多变体架构 (Monorepo Multi-variant)，通过 Gradle Product Flavors (`pure`, `full`) 拆分纯净版与插件全量版打包体系。
- **UI 框架**: **Jetpack Compose** (已全面取代传统 XML 布局)。使用了 Material Design 3 组件。通过 `compose-markdown` 支持原生 Markdown 代码高亮和复杂排版渲染。
- **状态管理**: Kotlin Coroutines (协程) `kotlinx.coroutines` 配合 `StateFlow` 实现响应式编程。
- **本地数据库**: Jetpack Room Database。
- **网络请求**: OkHttp 和 Gson 用于处理 HTTP 请求与 JSON 解析。
- **协议解耦**: 使用自研的动态协议中心 (`ProtocolRegistry`)，支持应用内自动扫描资产目录的 `.json` 配置文件实现网络请求格式的动态构建。

## 3. 核心目录结构 (`app/src/main/java/icather/pages/dev`)

项目主要逻辑集中在 `icather.pages.dev` 包下，划分为以下几个主要模块：

### 3.1 核心 UI 层 (Activities & Compose Screens)

所有的核心界面已全面迁移至 Jetpack Compose。

- `ChatActivity.kt` / `ui/screens/ChatScreen.kt`: 核心聊天界面，支持文本、图片附件发送，支持 Markdown 流式渲染 AI 回复。
- `SettingsActivity.kt` / `ui/screens/SettingsScreen.kt`: 应用全局设置界面，包含数据备份/恢复入口、多语言切换以及协议插件指引。
- `ApiConfigActivity.kt` / `ui/screens/ApiConfigScreen.kt`: API 提供商、模型、密钥的管理配置与列表渲染界面。
- `HistoryActivity.kt` / `HistoryAdapter.kt`: 历史会话记录管理界面 (待迁移至 Compose)。
- `AboutActivity.kt` / `LicenseActivity.kt`: 静态说明页面。

### 3.2 状态管理层 (ViewModels)

- `chat/ChatViewModel.kt`: 管理聊天屏幕的状态 (`ChatUiState`)，处理发送消息、加载历史记录、维护附件等业务逻辑。
- `settings/SettingsViewModel.kt`: 管理设置页面的状态，处理导出 JSON、解析导入数据的异步任务。
- `ui/screens/ApiConfigViewModel.kt`: 管理大模型网络配置的新增、切换与删除操作。

### 3.3 数据与领域层 (Repositories)

- `repository/ChatRepository.kt`: 统管 Room 数据库访问和 AI 网络请求，隐藏具体的数据源实现细节，为 `ChatViewModel` 提供干净的接口。
- `repository/SettingsRepository.kt`: 封装繁重的文件 I/O 读写、JSON 序列化及哈希计算操作，管理 SharedPreferences 中的活动 API 配置项。

### 3.4 `api/plugin` 模块 (动态网络架构)

高度解耦的网络请求工厂与插件注册表。

- `ApiService.kt`: 定义了与 AI 平台交互的标准接口（获取回复流、执行 OCR）。
- `ProtocolRegistry.kt`: 全局唯一单例，提供应用启动期的内置协议 (`OpenAI`, `Anthropic`) 与动态 `.json` 插件自动挂载功能。
- `DynamicApiService.kt` / `DynamicProtocolModels.kt`: JSON 声明式协议模型及网络接口动态实现。

### 3.5 `db` 模块 (本地存储层)

基于 Room 的持久化映射。

- `AppDatabase.kt`: Room 数据库入口。
- `ApiConfig.kt` / `Conversation.kt` / `Message.kt`: 数据实体类及对应的 DAO（经历了多次数据库降级与平滑升级 `MIGRATION_4_5`）。

### 3.6 项目根目录资源

- `protocol_plugins/`: 存放独立的提供商协议 JSON 文件，供开发者测试或通过构建系统的 `full` flavor 自动打包至应用内部。

## 4. 核心工作流

1. **开机注册**: `MainApplication` 启动时调用 `ProtocolRegistry.init(this)`，加载全部动态协议。
2. **状态配置**: ViewModel 从 Room 和 SharedPreferences 中加载选中的 `ApiConfig`。
3. **会话管理**: 用户发消息时，触发 Repository 创建或绑定当前会话 (`Conversation`)。
4. **消息发送与渲染**:
   - 通过 `ChatRepository` 获取 `ProtocolRegistry` 分发的 `DynamicApiService`。
   - 网络响应块通过 Flow 实时推送到 `ChatViewModel` 的 `uiState`。
   - Compose 监听到状态变化，利用 Markdown 库动态刷新聊天列表，同时异步落盘至 Room。

## 5. 项目代码诊断与改进路线 (v2026.05)

### 5.1 架构与 UI 痛点 (✅ 已解决)

经过最近一周的深度重构：

1. **全面 Compose 化**: 清除了遗留的 XML 布局，构建了极简的 `ApiConfigScreen` 与 `SettingsScreen`。
2. **架构动态解耦**: 彻底废弃了写死各个厂商的工厂模式，引入单仓库双轨变体打包机制和 JSON 配置声明协议的插件化模式。
3. **数据模型规范化**: 清理了历史技术债，重新梳理了模型名字段结构。

### 5.2 大语言模型应用痛点 (⏳ 下一步重点)

1. **上下文爆炸风险 (Context Window 溢出)**: 每次请求将全部历史数据传给大模型，缺乏滑动窗口截断 (Sliding Window) 或 AI 自动摘要压缩机制，极易引发 API 限流报错。
2. **缺失网络容错与弹性 (Resilience)**: 发生网络中断时缺乏指数退避重试 (Exponential Backoff) 策略。
3. **缺乏结构化 Prompt 管理**: 现阶段尚未隔离系统设定 (System Prompt) 和角色扮演上下文，输出格式无法收敛。

## 6. 待适配的提供商 (国内主流大模型 API 生态)

基于最新上线的“单仓库多变体架构与 JSON 动态协议加载”系统，后续将逐步为以下国内主流 API 提供商编写独立的 `.json` 插件并放入 `protocol_plugins` 目录供外部下载或全量编译：

1. **DeepSeek (深度求索)** - 以极高性价比与强大的代码/数学推理能力著称。
2. **SiliconFlow (硅基流动)** - 聚合平台，支持超高速推理和丰富的开源模型阵列。
3. **Zhipu AI (智谱 AI / ChatGLM)** - GLM 系列，国内商用落地最广的模型之一。
4. **Moonshot AI (月之暗面 / Kimi)** - 主打超长上下文（Context Window）处理能力。
5. **Baidu Qianfan (百度千帆 / 文心一言)** - 企业级全面覆盖，中文理解底蕴深厚。
6. **Alibaba DashScope (阿里灵积 / 通义千问)** - Qwen 系列，开源界的绝对主力。
7. **ByteDance Volcengine (字节火山引擎 / 豆包)** - 极具价格优势，生态覆盖面广。
8. **Minimax (稀宇科技)** - 在角色扮演（Roleplay）和语音多模态领域表现突出。
9. **Lingyi Wanwu (零一万物 / Yi)** - 李开复创立，Yi 系列开源模型。
10. **Baichuan AI (百川智能)** - 专注医疗与中文领域的精调优化。
11. **Tencent Hunyuan (腾讯混元)** - 腾讯生态内部打磨的主力通用大模型。
12. **SenseTime (商汤科技 / 日日新)** - 视觉与多模态大模型的早期领军者。

### 6.1 插件文件命名规范

为了方便在 GitHub 仓库中管理和供用户辨识，所有放入 `protocol_plugins` 目录的 JSON 配置文件，强制遵循以下命名规则：
**格式**: `[提供商]-[模型名称(可选)]-[最后修改日期].json`

**示例**:

- `deepseek-20260504.json` (提供商通用协议)
- `zhipu-glm4v-20260504.json` (针对特定多模态模型的独立协议)

**规范说明**:

- 提供商和模型名称尽量使用小写英文，单词间使用连字符 `-` 连接。
- 最后修改日期采用 `yyyyMMdd` 的 8 位纯数字格式，以便于在资源管理器中自然排序，同时也充当版本号的角色，用户看一眼文件名就能判断插件是否需要更新。

### 6.2 各类提供商的API官方文档核心指标

为了针对性地优化底层的请求逻辑与上下文管理，我们将持续通过官方文档提炼各家模型用于指导开发的结论：

#### 6.2.1 DeepSeek(2026/5/4)

1. **接口兼容性与基建**：
   - 基础 URL 为 `https://api.deepseek.com`。同时支持 Anthropic API 格式兼容（`https://api.deepseek.com/anthropic`）。
   - 完全兼容 OpenAI SDK。并发限速时返回 `HTTP 429` 错误；处理超长推理时会在 SSE 流中发送空行或 `: keep-alive` 注释以防止连接超时。**开发建议**：必须实现指数退避重试 (Exponential Backoff)，并在流解析器中过滤空行与保活注释。
2. **Token 与上下文硬盘缓存（KV Cache）**：
   - 计费：中文 1 字符 ≈ 0.6 Tokens，英文 1 字符 ≈ 0.3 Tokens。
   - 缓存机制：系统默认开启上下文硬盘缓存（缓存命中后费用降至 1/10）。触发条件为完整匹配此前的缓存前缀单元（按请求结束位置、公共前缀、或固定 Token 间隔落盘）。API 响应的 `usage` 中新增 `prompt_cache_hit_tokens` 和 `prompt_cache_miss_tokens` 字段以评估命中率。**开发建议**：在多轮对话滑动窗口截断时，强烈建议保持 System Prompt 和历史对话前缀绝对稳定，最大化复用缓存。
3. **思考模式 (Thinking Mode)**：
   - **注意：`deepseek-chat` 和 `deepseek-reasoner` 标识将于 2026/07/24 彻底弃用。** 未来统一使用 `deepseek-v4-pro`（或 flash）。
   - 开启方式：向请求体注入 `extra_body={"thinking": {"type": "enabled"}}` 及 `reasoning_effort="high"`（或 `max`）。思考模式不支持 `temperature` 等部分输出惩罚参数。
   - 上下文拼接：在多轮对话中，若中间未发生工具调用，则前一轮的思维链（`reasoning_content`）**不应**参与下一轮拼接；若发生了工具调用，则**必须**完整回传包含 `reasoning_content` 的 Assistant 消息体，否则返回 HTTP 400。
4. **结构化与控制指令增强 (Beta 功能)**：
   - **JSON Output**：支持通过 `response_format={'type': 'json_object'}` 强制输出 JSON 格式。**开发建议**：需在 prompt 中带入 `json` 关键字并附带示例结构，同时注意防范极小概率返回空 content 的情况。
   - **前缀续写 (Prefix Completion)**：设置 `base_url="https://api.deepseek.com/beta"`。在消息列表末尾提供 role 为 `assistant` 的消息，并附加 `prefix: True`，可强制模型接着给定内容续写（如强制输出特定代码块开头）。
   - **FIM 补全 (Fill In the Middle)**：通过 `/completions` 端点提供 `prompt` 和 `suffix` 参数实现中间内容补全，最大补全长度 4K。
   - **函数调用 (Tool Calls)**：支持标准 OpenAI Tool Calls，并在 Beta 接口下支持 `strict: true` 模式，强制模型严格遵循设定的 JSON Schema 格式输出（支持 enum, anyOf 及 $ref 递归结构）。

### 6.3 动态模型能力感知机制 (Dynamic Capabilities)

为了应对不同大模型提供商特有的功能（如 DeepSeek 的“思考模式”、Anthropic 的“结构化输出”等），项目引入了底层直达 UI 的动态能力感知架构：

1. **协议层能力声明**：`ProtocolPluginJson` 中新增了 `capabilities: List<String>` 字段。通过 JSON 插件的免编译化声明，大模型可以将自己支持的高级功能告诉应用。
2. **底层参数透传**：网络核心接口 `ApiService.getCompletion` 新增了 `options: Map<String, Any>` 参数字典，允许 UI 层的设置直接穿透至底层，组装专有的请求体字段（如 `extra_body`）。
3. **UI 动态渲染**：在 `ChatScreen` 的主聊天界面，Compose 会实时监听当前所选模型的 `supportedCapabilities`。当且仅当命中特定能力标识（例如 `"thinking_mode"`）时，才会动态渲染出配套的交互开关（Toggle/Switch），实现零侵入式的界面解耦。
