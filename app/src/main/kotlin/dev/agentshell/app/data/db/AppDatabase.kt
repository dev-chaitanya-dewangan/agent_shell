package dev.agentshell.app.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

/**
 * agentShell local Room database.
 *
 * Stores chat sessions and messages for persistent history.
 * Version bump required when schema changes (add migration).
 */
@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agentshell.db"
                )
                    .fallbackToDestructiveMigration() // dev mode — replace with migrations in prod
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
