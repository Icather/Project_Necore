# Project_Necore 核心攻坚与远景待办列表 (TODOs)

*(全盘整合版：将“高级特化架构”、“端侧防弹机制”与“灵魂引擎”按实施难度和可行性归纳为三大战役)*

## 分类 A：[难度 ⭐️ | 可行性 极高] 基础架构重构与协议落地 (Core Architecture)
*(目标：用极高的标准把底座和门面搭建稳固，完全规避基础崩溃)*
- `[x]` **A1. 协议数据模型重构**: 引入安全的嵌套数据类与 `JsonObject` (`DynamicProtocolModels.kt`)。彻底解决 Gson 绕过 Kotlin 构造器导致的 NPE 危机，以及 Map 解析精度丢失问题。
- `[x]` **A2. UI 单向状态渗透**: 更新 UI 开关，基于 `featureReasoning.allowsTemperature` 等字段禁用（Disable）互斥控件，解决“UI 显示与网络层实际发送不一致”的欺骗问题。
- `[x]` **A3. 孤儿配置兜底逻辑**: 在 `ProtocolRegistry` 新增安全隔离区与失效占位符映射，防止 Room 数据库指向已删除的 JSON 导致应用空指针崩溃。
- `[x]` **A4. 编写并挂载测试插件**: 编写首个真正的 `deepseek-v4-pro-20260504.json` 插件（严格遵守新 Schema），彻底跑通这套全新的动态协议通道。

## 分类 B：[难度 ⭐️⭐️ | 可行性 高] 业务流转与上下文特化 (Flow & Context Specialization)
*(目标：压榨大模型性能，极限降低长对话的 API 账单成本)*
- `[x]` **B1. 思考链 (Reasoning) 清洗器**: 修改请求拦截器，在发起多轮对话请求时，自动剔除历史记录中包含的旧 `reasoning_content`，避免破坏大模型的 API 规范及白白浪费巨量 Token。
- `[x]` **B2. 记忆压缩与 Token 退避 (Memory Compression)**: 摒弃粗暴删除历史记录的“缓存毁灭”做法。当网络层拦截到 HTTP 400 (超长报错) 或达到安全阈值时，自动调用底层大模型将中间对话提取为 **Summary 摘要并放回原位**，以构建全新的稳定前缀，继续白嫖硬盘缓存 (KV Cache)。
- `[x]` **B3. 高性能动态拼接与角色映射**: 改造 `ApiRequest`，利用 Gson `@SerializedName` 扩展注解实现零开销请求体组装；并在序列化前，根据 `featureRoles.systemRoleName` 动态替换系统提示词的角色名（如 `system` 转 `developer`）。
- `[x]` **B4. SSE 保活碎片免疫解析器**: 重写底层的流解析器，加入空行过滤与坏块容错机制，确保大模型在极长的“深度思考”阶段发送 `: keep-alive` 注释时不会导致 UI 断流。

## 分类 C：[难度 ⭐️⭐️⭐️ | 可行性 具挑战] 物理极限压榨与远景引擎 (Limits & Soul Engine)
*(目标：打破手机硬件瓶颈，并赋予应用“真正的灵魂”)*
- `[ ]` **C1. Compose 高频重绘节流阀**: 在 ViewModel 收集大模型 Flow 字节流时，引入协程的 `sample/debounce` 限流背压操作符。解决每秒数十次更新长篇 Markdown 导致的主线程极度卡顿 (Jank) 和手机发烫。
- `[ ]` **C2. Base64 多模态 OOM 防爆墙**: 绝不直接上传高清原图。在视觉大模型前置插入后台协程图片压缩器，将长边与画质死死压在安全内存红线内，防止 Base64 暴涨导致闪退。
- `[ ]` **C3. 灵魂组件库 (Identity & User)**: 剥离固化 System Prompt，支持多重 `Identity` 配置文件加载并联动 UI 颜色主题。接入 Tool Calls 特性，赋予模型读写本地 `User.json` 档案的能力，实现跨越时空的“长期记忆”。
- `[ ]` **C4. 情绪与静默关怀 (Soul & Heartbeat)**: 设计情感变量引擎动态改变模型的心情指数。基于 Android `WorkManager` 实现 AI 后台静默唤醒，根据天气/时间向用户推送主动关怀消息，完成向“陪伴体”的终极进化。
