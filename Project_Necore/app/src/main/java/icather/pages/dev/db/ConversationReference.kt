package icather.pages.dev.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "conversation_references",
    primaryKeys = ["conversationId", "referencedConversationId"],
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["id"],
            childColumns = ["referencedConversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["referencedConversationId"])]
)
data class ConversationReference(
    val conversationId: Long,
    val referencedConversationId: Long,
    val addedTime: Long = System.currentTimeMillis()
)
