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

@Database(entities = [Conversation::class, Message::class, ApiConfig::class, Identity::class, PromptTemplate::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun apiConfigDao(): ApiConfigDao
    abstract fun identityDao(): IdentityDao
    abstract fun promptTemplateDao(): PromptTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_database"
                ).addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
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

        // F1: Prompt 模板表
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `prompt_templates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon` TEXT NOT NULL DEFAULT '✨',
                        `systemPrompt` TEXT NOT NULL,
                        `isBuiltIn` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                // 预置内置模板
                database.execSQL("INSERT INTO `prompt_templates` (`name`, `icon`, `systemPrompt`, `isBuiltIn`) VALUES ('翻译助手', '🌐', '你是一位专业的翻译官。将用户输入的任何语言翻译成中文，如果输入是中文则翻译成英文。保持原文的语气和风格，不添加额外解释。', 1)")
                database.execSQL("INSERT INTO `prompt_templates` (`name`, `icon`, `systemPrompt`, `isBuiltIn`) VALUES ('代码审查', '🔍', '你是一位资深代码审查专家。分析用户提供的代码，指出潜在的Bug、性能问题、安全隐患和代码风格问题。给出改进建议和最佳实践。', 1)")
                database.execSQL("INSERT INTO `prompt_templates` (`name`, `icon`, `systemPrompt`, `isBuiltIn`) VALUES ('写作助手', '✍️', '你是一位出色的写作顾问。帮助用户润色文章、修改语法、优化表达。保持用户原有的写作风格，让文字更流畅、更有说服力。', 1)")
                database.execSQL("INSERT INTO `prompt_templates` (`name`, `icon`, `systemPrompt`, `isBuiltIn`) VALUES ('学习导师', '📚', '你是一位耐心的学习导师。用通俗易懂的语言解释复杂概念，善用类比和例子。当学生困惑时，从不同角度重新解释，直到他们理解为止。', 1)")
                database.execSQL("INSERT INTO `prompt_templates` (`name`, `icon`, `systemPrompt`, `isBuiltIn`) VALUES ('创意头脑风暴', '💡', '你是一位富有创造力的头脑风暴伙伴。针对用户提出的主题，快速产出多个新颖、有趣的创意方向。不要自我审查，鼓励大胆的想法。', 1)")
            }
        }

        // H1: 侧边栏重构 — 对话置顶功能
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `conversations` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0")
            }
        }

        // 消息版本分支 — 支持编辑消息后保留历史版本
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `parentId` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `branchIndex` INTEGER NOT NULL DEFAULT 0")
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
