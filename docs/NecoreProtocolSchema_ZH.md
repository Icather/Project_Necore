# Necore 协议 Schema 规范 (Necore Protocol Schema Specification)

## 1. 概述
本文档定义了 `Project_Necore` 中高度细化、原子化的能力矩阵。它是 Necore 应用程序与任何外部大语言模型（LLM）提供商之间的绝对“API 接口契约”。

本 Schema 摒弃了模糊的抽象概念，提供了一对一的客观映射接口，并内置了冗余度以适应未来 LLM 的新特性。所有提供商插件（例如 `deepseek.json`）都必须严格遵循此 Schema。App 将完全依赖这些参数来动态调整 UI 界面、过滤拦截请求并管理上下文。

## 2. 超级 Schema 定义 (The Superset Schema Definition)

### 2.1 提供商身份信息 (`provider_info`)
提供商及模型的基础元数据。
```json
"provider_info": {
  "id": "deepseek-v4-pro",
  "display_name": "DeepSeek V4 Pro (DeepSeek)",
  "base_url": "https://api.deepseek.com/v1",
  "is_openai_compatible": true,
  "available_models": ["deepseek-v4-pro", "deepseek-v4-flash"]
}
```
- `available_models`: 可选字段。该提供商下可用的模型 ID 列表，供 App 的提供商分组选择器填充模型下拉。

### 2.2 推理与思考链能力 (`feature_reasoning`)
控制如何处理思维链（Chain-of-Thought, CoT）过程。
```json
"feature_reasoning": {
  "supported": true,
  "trigger_type": "extra_body", // 触发方式：'extra_body', 'param' 或 'system_prompt'
  "trigger_payload": {"thinking": {"type": "enabled"}}, // 实际需注入的 Payload 载荷
  "response_field": "reasoning_content", // API 响应包中包含思考过程的专属字段名
  "allows_temperature": false // 若为 false，App 在发起请求前必须强制剥离 'temperature' 参数
}
```

### 2.3 KV 缓存控制策略 (`feature_cache`)
定义 App 应当如何管理历史记录截断与前缀，从而最优化 Token 成本。
```json
"feature_cache": {
  "supported": true,
  "strategy": "implicit_prefix", // 策略：'implicit_prefix' (DeepSeek 隐式前缀) 或 'explicit_ephemeral' (Claude 显式标记)
  "explicit_max_breakpoints": 0, // 允许的最大显式缓存探测点数量（例如 Claude 允许 4 个）
  "explicit_tag_format": null // 显式标签的具体格式，例如 {"type": "ephemeral"}
}
```

### 2.4 视觉与多模态 (`feature_vision`)
```json
"feature_vision": {
  "supported": true,
  "max_images_per_request": 10,
  "detail_control_supported": true, // 是否支持 'high' / 'low' 清晰度保真控制
  "input_format": "base64" // 支持的输入格式：'base64' 或 'url'
}
```

### 2.5 工具调用 (`feature_tools`)
```json
"feature_tools": {
  "supported": true,
  "strict_mode_supported": true, // 是否支持严格遵循 JSON Schema 约束的结构化输出
  "parallel_tool_calls_supported": true // 是否支持并行工具调用
}
```

### 2.6 结构化输出 (`feature_structured_output`)
```json
"feature_structured_output": {
  "json_mode_supported": true, // 是否支持 response_format: { type: "json_object" }
  "requires_json_keyword_in_prompt": true // 若为 true，App 必须在提示词末尾强行追加“请使用 JSON 格式输出”
}
```

### 2.7 角色与系统设定 (`feature_roles`)
定义提供商如何处理系统提示词。
```json
"feature_roles": {
  "system_role_name": "system", // 例如 OpenAI o1 系列强制要求使用 "developer"
  "supports_system_role": true // 若为 false，App 必须将系统设定强行拼接到第一条用户消息中
}
```

### 2.8 流式传输与连接 (`feature_streaming`)
```json
"feature_streaming": {
  "supported": true // 若为 false，App 的 UI 必须切换为“转圈等待”模式，而非打字机模式
}
```

### 2.9 原生联网搜索 (`feature_native_search`)
```json
"feature_native_search": {
  "supported": true,
  "trigger_param": "use_search" // 激活原生搜索所必需的隐式参数键名
}
```

### 2.10 计费与费率 (`billing_metadata`)
供 App 内部的统计角标直接换算并展示花销。
```json
"billing_metadata": {
  "input_price_per_1m": 1.0,
  "output_price_per_1m": 2.0,
  "cache_hit_price_per_1m": 0.1
}
```

### 2.11 基础请求约束 (`constraints`)
决定 App 的上下文管理器（滑动窗口）触发截断行为的硬性上限。
```json
"constraints": {
  "max_input_tokens": 128000,
  "max_output_tokens": 8192,
  "requests_per_minute_limit": 60, // 供 App 触发指数退避重试 (Exponential Backoff) 的频率墙
  "context_window_safe_threshold": 0.95 // 安全阈值，当累计 Token 达到上限的 95% 时，App 立即触发截断或压缩机制
}
```

---

## 3. 真实厂商映射示例 (Real-World Mapping Examples)

> **排序规则**：有中文名称的提供商优先，按中文首字拼音字母顺序排列；无中文名的提供商排在后面，按英文字母顺序排列。

### 3.1 晴辰云 示例 (`qingchen_protocol.json`)
晴辰云是完全兼容 OpenAI 标准的聚合代理服务，支持流式推理但不使用自定义触发载荷，推理内容通过标准 `reasoning_content` 字段返回。
```json
{
  "provider_info": {
    "id": "QingchenCloud",
    "display_name": "晴辰云",
    "base_url": "https://gpt.qt.cool/v1",
    "is_openai_compatible": true
  },
  "feature_reasoning": {
    "supported": true,
    "response_field": "reasoning_content"
  },
  "feature_roles": {
    "system_role_name": "system",
    "supports_system_role": true
  },
  "feature_streaming": {
    "supported": true
  }
}
```

### 3.2 Anthropic Claude 示例 (`anthropic-claude-sonnet-4.6.json`)
Claude Sonnet 4.6+ 支持通过 `thinking` 响应字段返回扩展思考链，并使用显式临时缓存策略。
```json
{
  "feature_reasoning": {
    "supported": true,
    "trigger_type": "extra_body",
    "trigger_payload": {"thinking": {"type": "enabled", "budget_tokens": 10000}},
    "response_field": "thinking",
    "allows_temperature": false
  },
  "feature_cache": {
    "supported": true,
    "strategy": "explicit_ephemeral",
    "explicit_max_breakpoints": 4,
    "explicit_tag_format": {"type": "ephemeral"}
  }
}
```

### 3.3 DeepSeek 示例 (`deepseek-v4-pro.json`)
DeepSeek 依赖隐式缓存，且在开启推理思考模式时严格禁止传入 temperature 参数。
```json
{
  "feature_reasoning": {
    "supported": true,
    "trigger_type": "extra_body",
    "trigger_payload": {"thinking": {"type": "enabled"}},
    "allows_temperature": false
  },
  "feature_cache": {
    "supported": true,
    "strategy": "implicit_prefix"
  }
}
```

## 4. 客户端落地实现指导 (App-Side Kotlin Implementation)
Android 客户端在运行时，会将这些 JSON 插件反序列化为全局的 `ProtocolSchema` Kotlin 数据类。UI 渲染层和底层网络层将绝对且仅依赖这些布尔值/枚举值进行动态适配。以此实现 App 业务逻辑与各大模型厂商的彻底解耦。
