package icather.pages.dev.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = Conversation::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE
    )],
    // Add an index to the conversationId column for better performance
    indices = [Index(value = ["conversationId"])]
)
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val text: String,
    val isUser: Boolean,
    val isHtml: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
