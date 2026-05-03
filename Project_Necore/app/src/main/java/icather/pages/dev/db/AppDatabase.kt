package icather.pages.dev.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import icather.pages.dev.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(entities = [Conversation::class, Message::class, ApiConfig::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun apiConfigDao(): ApiConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_database"
                ).addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                 .fallbackToDestructiveMigration(true)
                 .addCallback(AppDatabaseCallback(context))
                 .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `modelType` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `api_configs_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `provider` TEXT NOT NULL, `name` TEXT NOT NULL, `apiKey` TEXT NOT NULL, `modelName` TEXT NOT NULL DEFAULT '')")
                database.execSQL("INSERT INTO `api_configs_new` (`id`, `provider`, `name`, `apiKey`, `modelName`) SELECT `id`, `provider`, `name`, `apiKey`, `modelType` FROM `api_configs`")
                database.execSQL("DROP TABLE `api_configs`")
                database.execSQL("ALTER TABLE `api_configs_new` RENAME TO `api_configs`")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `inputTokens` INTEGER")
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `outputTokens` INTEGER")
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `cacheHitTokens` INTEGER")
            }
        }
    }

    private class AppDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Pre-population of database has been removed.
        }
    }
}
