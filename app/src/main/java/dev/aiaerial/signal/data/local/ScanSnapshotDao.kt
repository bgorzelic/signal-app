package dev.aiaerial.signal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSnapshotDao {

    @Insert
    suspend fun insert(snapshot: ScanSnapshot): Long

    @Query("SELECT * FROM scan_snapshots ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ScanSnapshot>>

    @Query("SELECT * FROM scan_snapshots ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<ScanSnapshot>>

    @Query("SELECT * FROM scan_snapshots WHERE id = :id")
    suspend fun getById(id: Long): ScanSnapshot?

    @Query("DELETE FROM scan_snapshots WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM scan_snapshots")
    suspend fun count(): Int
}
