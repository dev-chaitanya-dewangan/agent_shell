# agentShell Data Schema

## 1. Room Database (AppDatabase)

The SQLite database is managed via Room and stores chat histories, vector embeddings, dynamic apps, and scheduler tasks.

### 1.1 Chat Sessions
```kotlin
@Entity("chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val llmProvider: String,
    val agentTier: String,
    val isCompacted: Boolean,
    val compressedData: ByteArray,   // GZIP JSON of turns
    val turnCount: Int,
    val summaryEmbedding: FloatArray?,
    @ColumnInfo(name = "summary_text") val summaryText: String?
)
```
*Note: Sessions compress JSON blobs of actual turns to save DB space.*

### 1.2 Vector Chunks (RAG)
```kotlin
@Entity("vector_chunks")
data class VectorChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceFile: String,          // e.g. "memory/context.md"
    val chunkText: String,
    val embedding: FloatArray,       // 384-dim MiniLM-L6 embedding
    val chunkIndex: Int,
    val updatedAt: Long
)
```

### 1.3 Mini Apps
```kotlin
@Entity("mini_apps")
data class MiniAppEntity(
    @PrimaryKey val id: String,
    val title: String,
    val iconEmoji: String,
    val screenFilePath: String,
    val createdAt: Long,
    val lastUsed: Long,
    val isActive: Boolean,
    val createdByTier: String,
    val agentSessionId: String
)
```

## 2. Key-Value Storage (DataStore)

`SettingsDataStore.kt` handles all user preferences. API keys are handled securely via `EncryptedSharedPreferences`.

**AppSettings data includes:**
*   `activeProvider`: LOCAL_GEMMA, OPENROUTER, GOOGLE_GEMINI, SELF_HOSTED_OLLAMA, SELF_HOSTED_LMSTUDIO.
*   LLM configurations (paths, temperatures, URLs).
*   Agent configurations (tier default, max steps).
*   Terminal settings (font size, mirrors).

## 3. Memory Filesystem

The agent maintains "memory" by writing and reading from persistent `.md` files on the device filesystem (`/files/memory/`).

*   **`context.md`:** User profile, accumulated knowledge, session summaries.
*   **`mistakes.md`:** Error log and "cures" to prevent the agent from repeating mistakes.
*   **`apps.md`:** Mini-app registry for the ScreenWatcher to parse.
*   **`calendar.md`:** Scheduled system tasks.

## 4. Mini-App JSON DSL

Apps are stored as JSON files in `/files/screens/` and conform to a specific schema (`agentshell-screen/1.0`).
*   Contains data sources (`local_kv`).
*   Layout schema (column, row, panel).
*   Widget types (big_number, progress_bar, radar_chart).
*   Notifications/Alarms.
