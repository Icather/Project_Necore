package icather.pages.dev.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityDao {

    @Query("SELECT * FROM identities ORDER BY id ASC")
    fun getAll(): Flow<List<Identity>>

    @Query("SELECT * FROM identities WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): Identity?

    @Insert
    suspend fun insert(identity: Identity): Long

    @Update
    suspend fun update(identity: Identity)

    @Delete
    suspend fun delete(identity: Identity)

    @Query("UPDATE identities SET isActive = 0")
    suspend fun deactivateAll()
}
