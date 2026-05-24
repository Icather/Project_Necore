package icather.pages.dev

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.repository.SettingsRepository
import icather.pages.dev.settings.SettingsEvent
import icather.pages.dev.settings.SettingsViewModel
import icather.pages.dev.ui.screens.SettingsScreen
import icather.pages.dev.ui.theme.Project_NecoreTheme
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(SettingsRepository(this, AppDatabase.getInstance(this)))
    }

    private val exportApiLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.executeExportApiConfigs(it) }
    }

    private val importApiLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importApiConfigs(it) }
    }

    private val exportChatHistoryLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.executeExportChatHistory(it) }
    }

    private val importChatHistoryLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { 
            val options = arrayOf(getString(R.string.overwrite_import), getString(R.string.incremental_import))
            AlertDialog.Builder(this)
                .setTitle(R.string.import_mode)
                .setItems(options) { _, which ->
                    viewModel.importChatHistory(it, which == 0)
                }
                .show()
        }
    }

    private val selectConversationsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIds = result.data?.getLongArrayExtra("selected_ids")?.toList()
            if (selectedIds != null && selectedIds.isNotEmpty()) {
                viewModel.prepareExportSelectedChatHistory(selectedIds)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is SettingsEvent.TriggerExportApiConfigs -> exportApiLauncher.launch(event.fileName)
                    is SettingsEvent.TriggerExportChatHistory -> exportChatHistoryLauncher.launch(event.fileName)
                    else -> {} // Toast is handled in Compose
                }
            }
        }

        setContent {
            Project_NecoreTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onNavigateToApiConfig = { startActivity(Intent(this@SettingsActivity, ApiConfigActivity::class.java)) },
                    onNavigateToPlugins = {
                        startActivity(Intent(this@SettingsActivity, PluginManagerActivity::class.java))
                    },
                    onNavigateToAbout = { startActivity(Intent(this@SettingsActivity, AboutActivity::class.java)) },
                    onNavigateToIdentity = { startActivity(Intent(this@SettingsActivity, IdentityActivity::class.java)) },
                    onNavigateToUsage = { startActivity(Intent(this@SettingsActivity, UsageActivity::class.java)) },
                    onNavigateToTemplates = { startActivity(Intent(this@SettingsActivity, PromptTemplateActivity::class.java)) },
                    onNavigateToSync = { startActivity(Intent(this@SettingsActivity, SyncActivity::class.java)) },
                    onLanguageClick = { showLanguageSelectionDialog() },
                    onImportApiClick = { importApiLauncher.launch(arrayOf("application/json")) },
                    onExportChatClick = { showExportChatHistoryDialog() },
                    onImportChatClick = { importChatHistoryLauncher.launch(arrayOf("application/json")) }
                )
            }
        }
    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf("简体中文", "English", "Русский", "日本語", "Español", "Português")
        AlertDialog.Builder(this)
            .setTitle(R.string.language)
            .setItems(languages) { _, which ->
                val locale = when (which) {
                    0 -> "zh-CN"
                    1 -> "en"
                    2 -> "ru"
                    3 -> "ja"
                    4 -> "es"
                    5 -> "pt-BR"
                    else -> "en"
                }
                val appLocale = LocaleListCompat.forLanguageTags(locale)
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
            .show()
    }

    private fun showExportChatHistoryDialog() {
        val options = arrayOf(getString(R.string.export_all), getString(R.string.select_and_export))
        AlertDialog.Builder(this)
            .setTitle(R.string.export_chat_history)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.prepareExportAllChatHistory()
                    1 -> selectConversationsToExport()
                }
            }
            .show()
    }

    private fun selectConversationsToExport() {
        val intent = Intent(this, HistoryActivity::class.java)
        intent.putExtra("is_selection_mode", true)
        selectConversationsLauncher.launch(intent)
    }
}
