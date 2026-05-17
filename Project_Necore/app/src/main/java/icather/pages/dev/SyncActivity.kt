package icather.pages.dev

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.repository.SettingsRepository
import icather.pages.dev.sync.SyncViewModel
import icather.pages.dev.ui.screens.SyncScreen
import icather.pages.dev.ui.theme.Project_NecoreTheme

/**
 * 局域网同步 — 宿主 Activity
 */
class SyncActivity : AppCompatActivity() {

    private val viewModel: SyncViewModel by viewModels {
        SyncViewModel.Factory(SettingsRepository(this, AppDatabase.getInstance(this)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Project_NecoreTheme {
                SyncScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
