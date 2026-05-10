package dev.agentshell.app.brain

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BrainLogDao {
    @Insert
    suspend fun insert(log: BrainLogEntity)

    @Query("SELECT * FROM brain_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<BrainLogEntity>>
    
    @Query("DELETE FROM brain_logs")
    suspend fun clearAll()
}
