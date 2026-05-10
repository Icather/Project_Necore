package icather.pages.dev.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * F1: Prompt 模板 DAO
 */
@Dao
interface PromptTemplateDao {
    @Query("SELECT * FROM prompt_templates ORDER BY isBuiltIn DESC, id ASC")
    fun getAll(): Flow<List<PromptTemplate>>

    @Query("SELECT * FROM prompt_templates ORDER BY isBuiltIn DESC, id ASC")
    suspend fun getAllList(): List<PromptTemplate>

    @Insert
    suspend fun insert(template: PromptTemplate): Long

    @Update
    suspend fun update(template: PromptTemplate)

    @Delete
    suspend fun delete(template: PromptTemplate)

    @Query("SELECT * FROM prompt_templates WHERE id = :id")
    suspend fun getById(id: Long): PromptTemplate?
}
