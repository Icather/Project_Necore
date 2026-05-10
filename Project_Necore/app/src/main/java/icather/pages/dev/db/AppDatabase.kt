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

@Database(entities = [Conversation::class, Message::class, ApiConfig::class, Identity::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun apiConfigDao(): ApiConfigDao
    abstract fun identityDao(): IdentityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_database"
                ).addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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

        // D3: 灵魂组件库 — Identity 人设表
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `identities` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `systemPrompt` TEXT NOT NULL,
                        `greeting` TEXT NOT NULL DEFAULT '',
                        `isActive` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                // 预置一个默认助手人设
                database.execSQL("""
                    INSERT INTO `identities` (`name`, `systemPrompt`, `greeting`, `isActive`) 
                    VALUES ('默认助手', '你是一个有用的AI助手，请用简洁、准确的方式回答用户的问题。', '你好！有什么我可以帮你的吗？', 1)
                """.trimIndent())
            }
        }
    }

    private class AppDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // 新数据库：预置默认助手人设
            db.execSQL("""
                INSERT OR IGNORE INTO `identities` (`name`, `systemPrompt`, `greeting`, `isActive`) 
                VALUES ('默认助手', '你是一个有用的AI助手，请用简洁、准确的方式回答用户的问题。', '你好！有什么我可以帮你的吗？', 1)
            """.trimIndent())
        }
    }
}
