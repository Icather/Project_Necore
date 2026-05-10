package icather.pages.dev

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.ui.screens.UsageScreen
import icather.pages.dev.ui.screens.UsageStats
import icather.pages.dev.ui.theme.Project_NecoreTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * F4: 用量统计看板宿主 Activity
 *
 * 直接查 Room DAO 聚合数据，无需额外 ViewModel。
 */
class UsageActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.getInstance(this) }
    private val dao by lazy { db.messageDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Project_NecoreTheme {
                var stats by remember { mutableStateOf(UsageStats()) }

                LaunchedEffect(Unit) {
                    stats = withContext(Dispatchers.IO) {
                        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                        UsageStats(
                            totalInputTokens = dao.getTotalInputTokens(),
                            totalOutputTokens = dao.getTotalOutputTokens(),
                            totalCacheHitTokens = dao.getTotalCacheHitTokens(),
                            totalConversations = dao.getTotalConversationCount(),
                            totalAiMessages = dao.getTotalAiMessageCount(),
                            dailyStats = dao.getDailyTokenStats(sevenDaysAgo)
                        )
                    }
                }

                UsageScreen(
                    stats = stats,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
