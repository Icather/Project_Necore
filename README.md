<div align="center">

# <img src="docs/assets/app_icon.webp" width="48" height="48" align="absmiddle" /> Necore

**跨生态 AI 聊天客户端 — 一个 App 接入所有大模型**

自带你的 API Key，通过 JSON 声明式插件一键直连 DeepSeek / Qwen / GPT / Claude / Gemini 等主流 LLM。
零服务器、零月费、完全本地。

[![Android](https://img.shields.io/badge/Platform-Android-3ddc84?logo=android&logoColor=white)](https://github.com/Icather/Project_Necore/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/Icather/Project_Necore?color=blue)](https://github.com/Icather/Project_Necore/releases/latest)

🌐 [官网](https://icather.pages.dev) · 📦 [下载 APK](https://github.com/Icather/Project_Necore/releases/latest) · 📖 [架构文档](ARCHITECTURE.md) · 🐛 [反馈 Issue](https://github.com/Icather/Project_Necore/issues/new)

</div>

---

## 💡 为什么选择 Necore？

> **一个 App，所有模型。** 不做中间商，不收月费，你的 Key 直连官方 API。

| 痛点 | Necore 的解决方案 |
|:--|:--|
| 每家大模型都要装一个 App | 一个客户端接入所有提供商，提供商/模型随时切换 |
| API 调用需要写代码 | JSON 插件声明式配置，零代码扩展新模型 |
| 套壳 App 收中间费用 | BYO-Key 架构，直连官方，Token 费用公开透明 |
| 聊天记录只在云端 | 本地 Room 数据库持久化，隐私完全可控 |
| 不同模型的特殊功能用不了 | 动态能力感知，深度思考 / 联网搜索 / 多模态按需启用 |

---

## ✨ 功能亮点

- 🔌 **JSON 插件协议** — 一份 JSON 文件定义一个模型接入点，支持 OpenAI 兼容 / Anthropic / Gemini 等多种协议格式，免编译热扩展
- 🧩 **提供商/模型分离选择器** — 按 `base_url` 自动聚合提供商，每个提供商只需一个 API Key，旗下所有模型自由切换
- 🌊 **SSE 流式逐字渲染** — Okio 原生 `BufferedSource` 解析 + 50ms 节流采样，真正的实时打字机体验
- 💭 **思考链独立渲染** — DeepSeek / QwQ 的推理过程以可折叠区块展示，正文与思维链分离
- 🔍 **联网搜索** — 声明式配置，支持的模型自动显示开关，零代码扩展
- 🌳 **消息版本分支** — 编辑消息不删历史，树状版本管理 + `< 1/2 >` 分支导航
- 📊 **Token 实时监控** — 每条消息的输入/输出 Token、缓存命中率一目了然
- ⏹️ **终止生成** — 一键断流，已生成内容完整保留
- 🔄 **应用内自动更新** — 通过 GitHub Releases API 检测新版本，一键 OTA
- 📡 **局域网端到端加密同步** — ECDH + AES-256-GCM，SAS 短认证码防中间人，零第三方依赖
- 📝 **原生 Markdown 渲染** — 代码高亮、表格、LaTeX 公式，流式阶段轻量渲染 → 结束后精准格式化
- 🛒 **在线插件市场** — 从 GitHub 仓库浏览、下载协议插件，无需重新安装即可扩展新模型支持

---

## 📦 下载安装

前往 [GitHub Releases](https://github.com/Icather/Project_Necore/releases/latest) 下载最新 APK：

| 版本 | 说明 |
|:--|:--|
| `Necore-vX.X.X-full-release.apk` | **推荐**。内置全部协议插件，开箱即用 |
| `Necore-vX.X.X-pure-release.apk` | 纯净版，不含预置插件，可通过「在线插件市场」按需下载 |

> **系统要求**：Android 6.0 (API 23) 及以上

### 快速开始

1. 下载并安装 APK
2. 打开 Necore → 进入「API 配置」页面
3. 选择一个提供商（如 DeepSeek），填入你的 API Key
4. 返回聊天界面，选择模型，开始对话 🚀

---

## 🔌 已适配提供商

Necore 通过 JSON 协议插件接入大模型 API。以下是已适配的提供商与模型：

### 聚合平台

- ✅ **晴辰云** (QingchenCloud) — 第三方 OpenAI 兼容代理 _(2026-05-06)_

### 阿里云百炼平台 (DashScope)

统一 OpenAI 兼容接口，按模型家族分类：

| 模型家族 | 已适配模型 | 更新日期 |
|:--|:--|:--|
| **千问 Qwen** | Qwen3.6-Max-Preview · Qwen3.6-Plus · Qwen3.6-Flash · Qwen3.5-Omni-Plus · QwQ-Plus | 2026-05-07 |
| **DeepSeek (百炼转发)** | DeepSeek-V4-Pro · DeepSeek-V4-Flash | 2026-05-07 |
| **Kimi (Moonshot AI)** | Kimi-K2.6 | 2026-05-07 |
| **GLM (智谱清言)** | GLM-5.1 | 2026-05-07 |
| **MiniMax** | MiniMax-M2.5 | 2026-05-07 |

### 官方直连 API

| 提供商 | 适配模型 | 更新日期 |
|:--|:--|:--|
| **DeepSeek** | deepseek-v4-pro · deepseek-v4-flash (1M 上下文 / 384K 输出) | 2026-05-16 |
| **百度千帆 (ERNIE)** | ERNIE-5.1 | 2026-05-09 |
| **硅基流动 (SiliconFlow)** | DeepSeek-V3 | 2026-05-09 |
| **火山引擎 (豆包)** | Doubao-Seed-2.0-Lite | 2026-05-09 |
| **Anthropic** | Claude Sonnet 4.6 | 2026-05-09 |
| **Google** | Gemini 2.5 Pro | 2026-05-09 |
| **OpenAI** | GPT-4.1 · o4-mini | 2026-05-09 |

> 🆕 **扩展新模型？** 只需在 `protocol_plugins/` 目录下添加一份 JSON 文件即可，无需修改任何代码。详见 [协议插件 Schema 文档](docs/NecoreProtocolSchema_ZH.md)。

---

## 🏗️ 技术架构

| 层级 | 技术选型 |
|:--|:--|
| **架构模式** | MVVM + 单向数据流 (UDF) |
| **UI 框架** | Jetpack Compose + Material Design 3 |
| **语言** | Kotlin 100% |
| **状态管理** | Coroutines + StateFlow |
| **本地存储** | Room Database + SharedPreferences |
| **网络层** | OkHttp + Okio (SSE 原生流解析) + Gson |
| **构建体系** | Gradle Product Flavors (`pure` / `full`) |
| **最低支持** | Android 6.0 (API 23) / Target SDK 36 |

> 📖 深入了解架构设计请阅读 [ARCHITECTURE.md](ARCHITECTURE.md)

---

## 🛠️ 开发指南

### 前置条件

- [Android Studio](https://developer.android.com/studio) (最新稳定版)
- JDK 11+
- Android SDK 36

### 克隆与构建

```bash
# 克隆仓库
git clone https://github.com/Icather/Project_Necore.git
cd Project_Necore/Project_Necore

# 快速编译验证（仅检查语法，不打包）
./gradlew compilePureDebugKotlin

# 构建完整版 Debug APK（含全部协议插件）
./gradlew assembleFullDebug
```

### 项目结构

```
Project_Necore/
├── Project_Necore/              # Android 工程主目录
│   └── app/src/main/java/icather/pages/dev/
│       ├── ui/screens/          # Compose UI 页面
│       ├── chat/                # ChatViewModel 状态管理
│       ├── repository/          # 数据仓库层
│       ├── api/plugin/          # 协议插件注册表 & 动态 API 解析
│       ├── db/                  # Room Entity / DAO / Migrations
│       └── sync/                # 局域网同步模块
├── protocol_plugins/            # JSON 协议插件目录（full 版本打包）
├── docs/                        # 协议 Schema 文档
├── _update/                     # 版本更新相关
├── ARCHITECTURE.md              # 详细架构与技术指南
└── LICENSE                      # MIT License
```

### Build Variants

| Variant | 说明 |
|:--|:--|
| `pureDebug` | 纯净版，不含协议插件 JSON |
| `fullDebug` | **日常开发推荐**，自动打包 `protocol_plugins/` 目录 |
| `fullRelease` | 正式发布版，开启 ProGuard 混淆 |

---

## 🗺️ 路线图

### 近期计划

- [ ] 思考链过滤 — 上传历史时自动清洗截断旧的 Reasoning 记录
- [ ] 阶梯式上下文滑动窗口 — 利用前缀缓存特性，大步长历史丢弃策略

### 远景：灵魂引擎 (Soul Engine)

受 OpenClaw 启发，将 App 从「纯工具」向「赛博陪伴体」进化：

- 🎭 **Identity** — 热切换人格 System Prompt + 主题配色联动
- 👤 **User** — 从日常对话萃取用户偏好，构建本地长期画像
- 💗 **Soul** — 心情指数与好感度参数矩阵，影响回复语气
- 💓 **Heartbeat** — `WorkManager` 后台静默唤醒，AI 主动推送关怀通知

---

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源。

```
MIT License © 2025 一氯氢化物
```

---

<div align="center">

**如果 Necore 对你有帮助，请给一个 ⭐ Star 支持一下！**

[⬆ 回到顶部](#-necore)

</div>
