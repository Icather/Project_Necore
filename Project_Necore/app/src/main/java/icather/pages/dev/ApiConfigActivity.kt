package icather.pages.dev

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.repository.SettingsRepository
import icather.pages.dev.ui.screens.ApiConfigScreen
import icather.pages.dev.ui.screens.ApiConfigViewModel
import icather.pages.dev.ui.theme.Project_NecoreTheme

class ApiConfigActivity : AppCompatActivity() {

    private val viewModel: ApiConfigViewModel by viewModels {
        ApiConfigViewModel.Factory(SettingsRepository(this, AppDatabase.getInstance(this)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Project_NecoreTheme {
                ApiConfigScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onNavigateToPlugins = {
                        Toast.makeText(this, "Plugin Manager is coming soon!", Toast.LENGTH_SHORT).show()
                        // In the future, we can route directly to the Plugin Download screen here.
                    }
                )
            }
        }
    }
}
