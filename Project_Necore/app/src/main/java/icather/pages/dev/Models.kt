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
    val messageId: Long = 0  // E3: 关联 DB Message.id，用于编辑/删除定位
)

data class ChatHistoryBundle(val conversations: List<Conversation>, val messages: List<Message>)
