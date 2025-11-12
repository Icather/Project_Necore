package icather.pages.dev.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiConfigDao {

    @Insert
    suspend fun insert(apiConfig: ApiConfig)

    @Insert
    suspend fun insertAll(apiConfigs: List<ApiConfig>)

    @Query("SELECT * FROM api_configs")
    fun getAll(): Flow<List<ApiConfig>>

    @Query("SELECT * FROM api_configs")
    suspend fun getAllOnce(): List<ApiConfig>

    @Query("SELECT * FROM api_configs WHERE id = :id")
    suspend fun getById(id: Long): ApiConfig?

    @Query("DELETE FROM api_configs")
    suspend fun deleteAll()

    @Query("DELETE FROM api_configs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
