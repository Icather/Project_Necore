# Project_Necore (icather.pages.dev) 项目大纲与架构指南

## 1. 项目概览

- **项目名称**: Project_Necore (包名: `icather.pages.dev`)
- **项目类型**: Android 应用程序
- **主要开发语言**: Kotlin
- **核心功能**: 这是一个跨生态、高扩展性的 Android AI 聚合客户端。支持通过动态加载 JSON 插件一键直连各类大语言模型（LLM）API。具备原生 Markdown 渲染、本地聊天持久化、OCR 视觉功能以及深度定制的大模型能力动态感知。

## 2. 核心架构与技术栈

- **整体架构**: MVVM (Model-View-ViewModel) 结合单向数据流 (UDF)。
- **构建系统**: 单仓库多变体架构 (Monorepo Multi-variant)，通过 Gradle Product Flavors (`pure`, `full`) 拆分纯净版与插件全量版打包体系。
- **UI 框架**: **Jetpack Compose** (全面取代传统 XML 布局)。使用 Material Design 3 组件，配合 `compose-markdown` 支持极其丰富的文本排版。
- **状态管理**: Kotlin Coroutines (协程) `kotlinx.coroutines` 配合 `StateFlow` 实现响应式编程。
- **本地存储**: Jetpack Room Database (持久化对话与 API 配置) 与 SharedPreferences。
- **网络请求**: OkHttp (底层通信与 SSE 流解析) 与 Gson (JSON 反序列化)。

## 3. 高级特性架构

本项目突破了传统“套壳聊天软件”的局限，针对大语言模型特性进行了深度架构设计：

### 3.1 动态模型能力感知机制 (Dynamic Capabilities)
为了应对不同大模型提供商特有的功能（如 DeepSeek 的“思考模式”、Anthropic 的“结构化输出”等），项目引入了底层直达 UI 的动态能力感知架构：
1. **协议层能力声明**：通过 `ProtocolPluginJson` 中新增的 `capabilities: List<String>` 字段，大模型插件可免编译向应用宣告自身的独占功能。
2. **底层参数透传**：网络核心接口 `ApiService.getCompletion` 提供 `options: Map<String, Any>` 参数字典，允许 UI 层的设置直接穿透至底层请求负载。
3. **UI 按需渲染**：在 `ChatScreen` 聊天界面，Compose 实时监听模型的 `supportedCapabilities`，精准且动态地渲染相应的交互控件（如“思考模式”专属开关），实现零侵入式的界面解耦。

### 3.2 性能监控：实时 Token 与缓存命中率 (Token & Cache Hit Metrics)
为直观量化上下文管理策略的性能收益，项目已实装并持久化了全链路 Token 监控架构：
1. **数据库扩容持久化**：`messages` 表 (MIGRATION_5_6) 新增 `inputTokens`、`outputTokens` 和 `cacheHitTokens`，确保 Token 消耗日志伴随聊天记录永久落盘。
2. **流式拦截解析**：在 SSE 尾流提取大模型发回的 `usage` JSON 块，精准备捉 `prompt_cache_hit_tokens` 等特有指标。
3. **动态角标渲染**：在 AI 消息气泡底部渲染工程化格式 (K/M 单位转化) 及百分比换算的角标（例如：`📥 输入: 32.5K (命中率: 98.2%) | 📤 输出: 1.2K`），为开发者提供极致的性能调优视野。

## 4. 核心目录结构与工作流 (`app/src/main/java/icather/pages/dev`)

### 4.1 目录结构
- **`ui/screens/` & `Activities`**: 核心 UI 层，含 `ChatScreen.kt` (主聊天流)、`ApiConfigScreen.kt` (模型管理) 及 `SettingsScreen.kt`。
- **`chat/` & `settings/` (ViewModels)**: 状态管理中枢，维系业务逻辑、分发网络 Flow 流。
- **`repository/`**: 领域库层，封装繁重的文件 I/O、Room 数据库读写及工厂模式。
- **`api/plugin/`**: 网络动态化基石。包含 `ProtocolRegistry.kt` (单例插件注册表) 与 `DynamicApiService.kt` (JSON 声明式协议网络解析器)。
- **`db/`**: 本地存储映射层（Entity、DAO 与 Migrations）。
- **`protocol_plugins/` (根目录资产)**: 存放独立提供商协议 JSON 文件，供构建系统注入。

### 4.2 核心事件流向
1. `Application` 启动时触发 `ProtocolRegistry` 自动扫描并挂载全量动态协议。
2. ViewModel 根据选中的配置请求 `ChatRepository` 获取分发的 `ApiService`。
3. 发送消息后，SSE 响应块通过 Flow 实时推送到 `ChatUiState`，Compose 动态逐字渲染 Markdown，同时异步将正文及 Token 统计落盘至 Room。

## 5. API 提供商规范与知识库

基于 JSON 动态协议加载架构，本项目旨在无缝接入国内外大模型生态。

> **排序规则**：中文名提供商按拼音首字母排序在前，英文名按字母顺序排后。
> **数据来源**：[阿里云百炼模型广场](https://help.aliyun.com/zh/model-studio/getting-started/models)（`base_url: https://dashscope.aliyuncs.com/compatible-mode/v1`）及各官方直连 API。

### 5.0 待适配提供商与模型清单

#### 聚合平台

- `[x]` **晴辰云** (QingchenCloud) — `qingchen_protocol.json`　_(2026-05-06)_
  - 第三方 OpenAI 兼容代理，转发多家模型

#### 阿里云百炼平台 (Alibaba Bailian / DashScope)

百炼平台提供统一的 OpenAI 兼容接口，以下按**模型家族**分类：

**千问系列 (Qwen)**
- `[x]` Qwen3.6-Max-Preview — `bailian-qwen3.6-max-preview.json`　_(2026-05-07)_
- `[x]` Qwen3.6-Plus — `bailian-qwen3.6-plus.json`　_(2026-05-07)_
- `[x]` Qwen3.6-Flash — `bailian-qwen3.6-flash.json`　_(2026-05-07)_
- `[x]` Qwen3.5-Omni-Plus — `bailian-qwen3.5-omni-plus.json` (多模态：文本/图像)　_(2026-05-07)_
- `[x]` QwQ-Plus — `bailian-qwq-plus.json` (深度推理模型)　_(2026-05-07)_

**DeepSeek 系列 (百炼转发)**
- `[x]` DeepSeek-V4-Pro (百炼版) — `bailian-deepseek-v4-pro.json`　_(2026-05-07)_
- `[x]` DeepSeek-V4-Flash — `bailian-deepseek-v4-flash.json`　_(2026-05-07)_

**Kimi 系列 (Moonshot AI)**
- `[x]` Kimi-K2.6 — `bailian-kimi-k2.6.json`　_(2026-05-07)_

**GLM 系列 (智谱清言 / Zhipu AI)**
- `[x]` GLM-5.1 — `bailian-glm-5.1.json`　_(2026-05-07)_

**MiniMax 系列**
- `[x]` MiniMax-M2.5 — `bailian-MiniMax-M2.5.json`　_(2026-05-07)_
- `[ ]` MiniMax/Speech-2.8-HD — 语音合成

#### 官方直连 API（非百炼转发）

- `[x]` 百度千帆 (Baidu Qianfan / ERNIE) — `qianfan-ernie-5.1.json`　_(2026-05-09)_
- `[x]` 硅基流动 (SiliconFlow) — `siliconflow-deepseek-v3.json`　_(2026-05-09)_
- `[x]` 字节火山引擎 (ByteDance Volcengine / 豆包) — `volcengine-doubao-seed-2.0-lite.json`　_(2026-05-09)_
- `[x]` DeepSeek 官方 (`api.deepseek.com`) — `deepseek-v4-pro-20260504.json`　_(2026-05-04)_
- `[x]` Anthropic (Claude) — `anthropic-claude-sonnet-4.6.json`　_(2026-05-09)_
- `[x]` Google (Gemini) — `google-gemini-2.5-pro.json`　_(2026-05-09)_
- `[x]` OpenAI (GPT / o-series) — `openai-gpt-4.1.json` / `openai-o4-mini.json`　_(2026-05-09)_

### 5.1 插件文件命名规范
**格式**: `[提供商]-[模型名称(可选)]-[最后修改日期(yyyyMMdd)].json` (示例: `deepseek-v4-pro-20260504.json`)

### 5.2 提供商特性指导手册
#### 5.2.1 DeepSeek (2026/5/4)
1. **网络连接基建**：兼容 OpenAI SDK 格式。流处理超长推理时会发送空行或 `: keep-alive` 防止超时。需实现指数退避重试 (Exponential Backoff)。
2. **上下文硬盘缓存（KV Cache）**：默认开启。缓存匹配策略极其严苛（按前缀绝对匹配）。**开发建议**：进行截断和系统 Prompt 注入时，必须保证前缀绝对稳定。
3. **思考模式 (Thinking Mode)**：启用时需提供 `extra_body={"thinking": {"type": "enabled"}}`。**绝对禁忌**：多轮对话若未发生工具调用，严禁将历史对话中的思维链 (`reasoning_content`) 重新拼接上传，否则易引发 HTTP 400 且破坏缓存。

## 6. 进阶路线图 (Roadmap)

### 6.1 近期攻坚：上下文特化管理
为彻底解决 Context Window 爆炸与 Token 烧费问题：
- **思考链过滤机制**: 在封包上传历史记录前，清洗截断旧的 Reasoning 记录。
- **阶梯式上下文滑动窗口**: 利用前缀缓存特性，避免平滑滚动导致的“每轮必 Miss”，实施大步长历史丢弃策略。

### 6.2 远景规划：灵魂引擎 (Soul Engine)
受开源项目 OpenClaw 架构启发，将 App 从“纯工具”向“赛博陪伴体”进化：
- **Identity (身份与 UI 联动)**: 支持热切换不同人格的 System Prompt 及对应的主题配色。
- **User (用户画像库)**: 利用 Tool Calls 从日常对话中智能萃取用户偏好，记入本地长期档案。
- **Soul (情感状态机)**: 加入心情指数与好感度（Affection）参数矩阵，影响大模型回复语气。
- **Heartbeat (心跳机制)**: 借助 Android `WorkManager` 进行后台静默定时唤醒，由 AI 判定是否主动向用户推送关怀通知（Notification）。
