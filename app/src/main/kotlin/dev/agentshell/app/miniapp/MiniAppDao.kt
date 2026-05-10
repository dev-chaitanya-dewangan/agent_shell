package dev.agentshell.app.miniapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MiniAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(miniApp: MiniAppEntity)

    @Query("SELECT * FROM mini_apps ORDER BY timestamp DESC")
    fun getAll(): Flow<List<MiniAppEntity>>

    @Query("SELECT * FROM mini_apps WHERE id = :id")
    suspend fun getById(id: String): MiniAppEntity?

    @Query("DELETE FROM mini_apps WHERE id = :id")
    suspend fun deleteById(id: String)
}
