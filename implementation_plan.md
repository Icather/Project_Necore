# D3. 灵魂组件库 (Identity & User) — 实施方案

## 背景

将固化的 System Prompt 剥离为可配置的 **Identity（人设档案）** 系统，并通过 Tool Calls 赋予模型读写本地 `User.json` 用户档案的能力，实现跨会话"长期记忆"。

参考 SillyTavern（AI酒馆）的架构：
- **角色卡 = Identity**：Name + System Prompt + 开场白 + 对话示例
- **User Persona = User.json**：用户偏好、记忆条目，由模型通过 Tool Calls 主动维护
- **上下文编排**：Identity 系统指令 + User.json 注入 + 聊天历史 → 最终 Prompt

> [!IMPORTANT]
> 已按用户要求移除"UI主题联动"子功能。Identity 只影响 System Prompt 和行为，不影响界面颜色。

---

## 用户确认项

> [!WARNING]
> **开关设计**：D3 涉及超越常规 AI 的功能（长期记忆、主动记录用户信息）。方案中在设置页添加以下开关（默认全部开启）：
> 1. **「AI 人设系统」开关** — 控制是否注入 Identity System Prompt
> 2. **「长期记忆」开关** — 控制是否注入 User.json 到 System Prompt + 是否允许 Tool Calls 写入

---

## 受影响文件清单

### 第一层：数据模型

---

#### [NEW] Identity.kt
`db/Identity.kt` — Room Entity

```kotlin
@Entity(tableName = "identities")
data class Identity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,           // "默认助手"、"猫娘"、"英语老师"
    val systemPrompt: String,   // 核心人设提示词
    val greeting: String = "",  // 可选：首次对话的开场白
    val isActive: Boolean = false
)
```

#### [NEW] IdentityDao.kt
```kotlin
@Dao
interface IdentityDao {
    @Query("SELECT * FROM identities ORDER BY id ASC")
    fun getAll(): Flow<List<Identity>>

    @Query("SELECT * FROM identities WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): Identity?

    @Insert
    suspend fun insert(identity: Identity): Long

    @Update
    suspend fun update(identity: Identity)

    @Delete
    suspend fun delete(identity: Identity)

    @Query("UPDATE identities SET isActive = 0")
    suspend fun deactivateAll()
}
```

#### [MODIFY] AppDatabase.kt
- 版本 6 → 7
- 新增 `Identity` Entity
- 新增 `MIGRATION_6_7`：`CREATE TABLE identities`
- 在 `AppDatabaseCallback.onCreate()` 中预置一个"默认助手"Identity

---

### 第二层：User.json 长期记忆引擎

---

#### [NEW] UserMemoryManager.kt
`memory/UserMemoryManager.kt`

```kotlin
/**
 * 管理用户长期记忆的 JSON 文件。
 * 存储路径：app 内部存储 /files/user_memory.json
 * 
 * JSON 结构（参考 SillyTavern 的 User Persona）：
 * {
 *   "user_name": "xxx",
 *   "preferences": ["喜欢猫", "正在准备高考"],
 *   "memories": [
 *     {"key": "上次聊天话题", "value": "讨论了物理题", "timestamp": "2026-05-09"},
 *     ...
 *   ]
 * }
 */
class UserMemoryManager(private val context: Context) {

    fun read(): JsonObject { ... }      // 读取完整 JSON
    fun write(data: JsonObject) { ... } // 整体写入
    
    fun addMemory(key: String, value: String) { ... }  // 追加一条记忆
    fun removeMemory(key: String) { ... }               // 删除一条
    fun getFormattedForPrompt(): String { ... }          // 格式化为可注入 System Prompt 的文本
}
```

---

### 第三层：Tool Calls 引擎

---

#### [NEW] ToolCallHandler.kt
`api/tools/ToolCallHandler.kt`

这是 D3 的核心难点。Tool Calls 需要实现一个**请求-响应-再请求**的循环：

```
App → 发送消息（含 tools 定义）→ LLM
LLM → 返回 tool_call（函数名 + 参数）→ App
App → 执行本地函数 → 拿到结果
App → 将结果以 tool role 回传 → LLM
LLM → 根据结果生成最终回复 → App
```

注册两个工具函数：

```kotlin
sealed interface ToolDefinition {
    data object ReadUserMemory : ToolDefinition  // 读取 user_memory.json
    data object WriteUserMemory : ToolDefinition  // 写入 user_memory.json
}
```

工具定义 JSON（注入到 API 请求的 `tools` 字段）：
```json
[
  {
    "type": "function",
    "function": {
      "name": "read_user_memory",
      "description": "读取用户的长期记忆档案，了解用户的偏好和过往信息",
      "parameters": {"type": "object", "properties": {}}
    }
  },
  {
    "type": "function", 
    "function": {
      "name": "write_user_memory",
      "description": "将重要的用户信息写入长期记忆档案，以便下次对话时记住",
      "parameters": {
        "type": "object",
        "properties": {
          "key": {"type": "string", "description": "记忆条目的标识"},
          "value": {"type": "string", "description": "记忆内容"}
        },
        "required": ["key", "value"]
      }
    }
  }
]
```

---

### 第四层：ViewModel 上下文编排

---

#### [MODIFY] ChatViewModel.kt

改造 `getAIResponse()` 中 System Prompt 的构建逻辑：

```kotlin
// 旧：硬编码的固定 system prompt
// 新：动态编排
val systemParts = buildString {
    // 1. Identity 人设（如果开关开启）
    if (identityEnabled) {
        val identity = identityDao.getActive()
        if (identity != null) append(identity.systemPrompt + "\n\n")
    }
    // 2. User.json 长期记忆（如果开关开启）
    if (memoryEnabled) {
        val memoryText = userMemoryManager.getFormattedForPrompt()
        if (memoryText.isNotBlank()) {
            append("## 用户档案（长期记忆）\n$memoryText\n\n")
        }
    }
}
```

同时，如果 `memoryEnabled` 且模型支持 `feature_tools`，在请求中注入 tools 定义。

---

### 第五层：UI 设置

---

#### [MODIFY] SettingsScreen.kt

新增两个开关：
- **「AI 人设系统」**：`SharedPreferences key: identity_enabled, default: true`
- **「长期记忆」**：`SharedPreferences key: memory_enabled, default: true`

#### [NEW] IdentityScreen.kt
`ui/screens/IdentityScreen.kt`

Identity 管理页面：
- 列表展示所有人设档案（卡片式）
- 点击卡片设为当前激活
- 右上角 + 按钮创建新人设
- 长按删除
- 编辑页面：Name + System Prompt 输入框 + Greeting 输入框

---

## 实施顺序

| 阶段 | 内容 | 预计改动量 |
|------|------|-----------|
| **P1** | Identity Entity + Dao + Migration + 预置数据 | ~120 行新代码 |
| **P2** | UserMemoryManager (JSON 读写) | ~80 行新代码 |
| **P3** | SettingsScreen 开关 + SharedPreferences | ~40 行改动 |
| **P4** | ChatViewModel 上下文编排改造 | ~60 行改动 |
| **P5** | IdentityScreen UI | ~200 行新代码 |
| **P6** | ToolCallHandler + 请求循环 | ~150 行新代码（最难） |
| **P7** | 编译验证 + 集成测试 | — |

总计约 **650 行新代码 + 100 行改动**。

---

## 验证方案

### 自动验证
- `compilePureDebugKotlin` 零错误

### 手动验证（用户）
1. 设置页面开关可见且默认为开
2. 创建一个"猫娘"人设 → 切换 → AI 回复语气变化
3. 对话中告诉 AI "我叫小明" → AI 主动调用 write_user_memory 存储
4. 开启新对话 → AI 能读取 User.json 并记住用户名字

---

## D4 参考研究（仅记录，不实施）

> 基于 SillyTavern 的情感系统研究，D4 的参考架构：
> 
> **情感分析路径**：AI 回复文本 → 轻量情感分类模型（或让模型在回复末尾自报 `[emotion:happy]` tag）→ 更新 EmotionState
> 
> **陪伴唤醒路径**：Android WorkManager 定时任务 → 读取天气 API + User.json → 拼接简短 prompt → 调用模型生成关怀消息 → NotificationManager 推送
> 
> **SillyTavern 的做法**：情感不是模型内部状态，而是"外部分类器 + 前端立绘切换"的视觉效果。Necore 可以简化为"模型自报 emotion tag + 头像/动效联动"。
