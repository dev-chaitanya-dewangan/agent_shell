package dev.agentshell.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Persisted chat message row.
 *
 * Linked to a [ChatSessionEntity] via foreign key.
 * [role] is stored as a string matching [dev.agentshell.app.chat.MessageRole] name.
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE   // deleting a session removes all its messages
        )
    ],
    indices = [Index("sessionId")]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: String,           // MessageRole.name
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val stepIndex: Int? = null
)
