# Necore Protocol Schema Specification

## 1. Overview
This document defines the highly granular, atomic capability matrix for `Project_Necore`. It serves as the absolute "API Contract" between the Necore Application and any external LLM provider. 

Instead of vague abstractions, this schema provides an objective, one-to-one mapping interface with built-in redundancy to accommodate future LLM features. All provider plugins (e.g., `deepseek.json`) must adhere to this schema. The App relies solely on these parameters to dynamically adjust UI, filter requests, and manage context.

## 2. The Superset Schema Definition

### 2.1 Provider Identity (`provider_info`)
Basic metadata for the provider and model.
```json
"provider_info": {
  "id": "deepseek-v4-pro",
  "display_name": "DeepSeek V4 Pro (DeepSeek)",
  "base_url": "https://api.deepseek.com/v1",
  "is_openai_compatible": true,
  "available_models": ["deepseek-v4-pro", "deepseek-v4-flash"]
}
```
- `available_models`: Optional list of model IDs available under this provider. Used by the App's Provider Group selector to populate the model dropdown.

### 2.2 Reasoning Capabilities (`feature_reasoning`)
Controls the handling of Chain-of-Thought (CoT) processes.
```json
"feature_reasoning": {
  "supported": true,
  "trigger_type": "extra_body", // How to activate it: 'extra_body', 'param', 'system_prompt'
  "trigger_payload": {"thinking": {"type": "enabled"}}, // The exact payload to inject when enabling
  "disable_payload": {"thinking": {"type": "disabled"}}, // Payload to inject when explicitly disabling (for models that default to thinking)
  "response_field": "reasoning_content", // The field name in the API response containing the thoughts
  "allows_temperature": false // If false, the App MUST strip 'temperature' from the request
}
```
- `disable_payload`: Optional. Required for models like DeepSeek V4 that **default to thinking mode**. When the user turns off reasoning, the App MUST inject this payload to explicitly disable it. If omitted, the App simply omits the trigger payload.

### 2.3 KV Cache Strategy (`feature_cache`)
Defines how the App should manage history truncation and prefixing to optimize costs.
```json
"feature_cache": {
  "supported": true,
  "strategy": "implicit_prefix", // 'implicit_prefix' (DeepSeek) or 'explicit_ephemeral' (Claude)
  "explicit_max_breakpoints": 0, // Number of allowed cache breakpoints (e.g., 4 for Claude)
  "explicit_tag_format": null // Format of the explicit tag, e.g., {"type": "ephemeral"}
}
```

### 2.4 Vision & Multimodal (`feature_vision`)
```json
"feature_vision": {
  "supported": true,
  "max_images_per_request": 10,
  "detail_control_supported": true, // Whether it supports 'high' / 'low' fidelity
  "input_format": "base64" // 'base64' or 'url'
}
```

### 2.5 Tool Calling (`feature_tools`)
```json
"feature_tools": {
  "supported": true,
  "strict_mode_supported": true, // Whether it strictly adheres to JSON Schema
  "parallel_tool_calls_supported": true
}
```

### 2.6 Structured Output (`feature_structured_output`)
```json
"feature_structured_output": {
  "json_mode_supported": true, // Supports response_format: { type: "json_object" }
  "requires_json_keyword_in_prompt": true // If true, App MUST append 'Please output in JSON'
}
```

### 2.7 Identity & Roles (`feature_roles`)
Defines how the provider handles system messages.
```json
"feature_roles": {
  "system_role_name": "system", // e.g., OpenAI o1 uses "developer"
  "supports_system_role": true // If false, App MUST prepend system prompt to the first user message
}
```

### 2.8 Streaming & Connectivity (`feature_streaming`)
```json
"feature_streaming": {
  "supported": true // If false, the App UI must use a spinner instead of typewriter effect
}
```

### 2.9 Native Web Search (`feature_native_search`)
```json
"feature_native_search": {
  "supported": true,
  "trigger_param": "use_search" // The payload key required to activate native search
}
```

### 2.10 Billing Metadata (`billing_metadata`)
Used by the App to estimate and display costs.
```json
"billing_metadata": {
  "input_price_per_1m": 1.0,
  "output_price_per_1m": 2.0,
  "cache_hit_price_per_1m": 0.1
}
```

### 2.11 Base Constraints (`constraints`)
Hard limits that the App's Context Manager (Sliding Window) must obey.
```json
"constraints": {
  "max_input_tokens": 128000,
  "max_output_tokens": 8192,
  "requests_per_minute_limit": 60, // Used by App to trigger Exponential Backoff
  "context_window_safe_threshold": 0.95 // The App triggers truncation when tokens hit 95% of max
}
```

---

## 3. Real-World Mapping Examples

### 3.1 DeepSeek Example (`deepseek-v4-pro.json`)
DeepSeek relies on implicit caching, strictly forbids temperature when reasoning is active, and **defaults to thinking mode** — requiring an explicit `disable_payload` to turn it off.
```json
{
  "feature_reasoning": {
    "supported": true,
    "trigger_type": "extra_body",
    "trigger_payload": {"thinking": {"type": "enabled"}},
    "disable_payload": {"thinking": {"type": "disabled"}},
    "allows_temperature": false
  },
  "feature_cache": {
    "supported": true,
    "strategy": "implicit_prefix"
  }
}
```

### 3.2 Anthropic Claude Example (`anthropic-claude-sonnet-4.6.json`)
Claude Sonnet 4.6+ supports extended thinking via the `thinking` response field, and uses explicit ephemeral caching.
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

## 4. App-Side Implementation (Kotlin)
The App will deserialize these JSON files into a master `ProtocolSchema` Kotlin Data Class. The UI and the Network Layer will exclusively read from these boolean/enum flags to adapt dynamically, achieving absolute code-level decoupling from the LLM vendors.

### 4.1 Reasoning Mode Logic
When the user toggles reasoning mode:
- **ON**: The App injects `trigger_payload` into the request body.
- **OFF**: If `disable_payload` is defined, the App injects it to explicitly disable thinking. Otherwise, no extra payload is sent.

This two-payload design handles both "opt-in thinking" models (e.g., Anthropic Claude) and "default-thinking" models (e.g., DeepSeek V4).
