package icather.pages.dev

import icather.pages.dev.db.Conversation
import icather.pages.dev.db.Message

data class ChatMessage(val text: String, val isUser: Boolean, val isHtml: Boolean = false)

data class ChatHistoryBundle(val conversations: List<Conversation>, val messages: List<Message>)
