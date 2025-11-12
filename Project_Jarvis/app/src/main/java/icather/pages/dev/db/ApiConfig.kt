package icather.pages.dev.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_configs")
data class ApiConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val provider: String,
    val name: String,
    val apiKey: String,
    val modelType: String
)
