package dev.agentshell.app.miniapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mini_apps")
data class MiniAppEntity(
    @PrimaryKey val id: String, // UUID or agent generated ID
    val name: String,
    val description: String,
    val entryHtmlPath: String, // Absolute path to the main HTML file
    val timestamp: Long = System.currentTimeMillis()
)
