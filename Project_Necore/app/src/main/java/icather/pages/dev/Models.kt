package icather.pages.dev

import icather.pages.dev.db.Conversation
import icather.pages.dev.db.Message

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isHtml: Boolean = false,
    val isStreaming: Boolean = false,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val cacheHitTokens: Int? = null,
    val messageId: Long = 0,  // E3: 关联 DB Message.id，用于编辑/删除定位
    val reasoningText: String = "",  // H2: 独立思考链文本，与正文分开渲染
    val modelName: String? = null,  // 该条回复使用的模型名称
    // 消息版本分支
    val siblingCount: Int = 1,    // 该槽位的总分支数
    val siblingIndex: Int = 0,    // 当前显示的分支序号 (0-based)
    val parentId: Long? = null    // 分支根消息 ID
)

data class ChatHistoryBundle(val conversations: List<Conversation>, val messages: List<Message>)

data class SettingsBackupBundle(
    val version: Int = 1,
    val apiConfigs: List<icather.pages.dev.db.ApiConfig>,
    val activeApiId: Long,
    val imageCompressionEnabled: Boolean,
    val identityEnabled: Boolean,
    val memoryEnabled: Boolean,
    val emotionEnabled: Boolean,
    val fallbackEnabled: Boolean,
    val languageSelected: Boolean
)
