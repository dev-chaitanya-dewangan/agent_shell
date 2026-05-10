package dev.agentshell.app.brain

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogType { 
    TASK_START, 
    TOOL_CALL, 
    TOOL_RESULT, 
    AGENT_THOUGHT, 
    ERROR, 
    MINI_APP_CREATED 
}

@Entity(tableName = "brain_logs")
data class BrainLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String,   // LogType enum name
    val tag: String,
    val content: String
)
