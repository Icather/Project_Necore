package icather.pages.dev

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import icather.pages.dev.chat.ChatViewModel
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.repository.ChatRepository
import icather.pages.dev.ui.screens.ChatScreen
import icather.pages.dev.ui.theme.Project_NecoreTheme

class ChatActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.Factory(ChatRepository(this, AppDatabase.getInstance(this)))
    }

    private val historyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val conversationId = result.data?.getLongExtra("CONVERSATION_ID", -1L) ?: -1L
                if (conversationId != -1L) {
                    viewModel.loadConversation(conversationId)
                }
            }
        }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        viewModel.addAttachments(uris, isImage = true)
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        viewModel.addAttachments(uris, isImage = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        val conversationId = intent.getLongExtra("CONVERSATION_ID", -1)
        if (conversationId != -1L) {
            viewModel.loadConversation(conversationId)
        } else {
            viewModel.startNewChat()
        }

        // D4: Android 13+ 通知权限运行时请求（HeartbeatWorker 关怀通知需要）
        requestNotificationPermissionIfNeeded()

        setContent {
            Project_NecoreTheme {
                ChatScreen(
                    viewModel = viewModel,
                    onOpenDrawer = { 
                        // Simplified drawer for Compose, or you can integrate standard ModalNavigationDrawer
                        // Here we just navigate to history for demonstration
                        val intent = Intent(this, HistoryActivity::class.java)
                        historyLauncher.launch(intent)
                    },
                    onNavigateToSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onModelSelectorClick = {
                        showModelSelector()
                    },
                    onImageUploadClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    onFileUploadClick = {
                        filePickerLauncher.launch("*/*")
                    }
                )
            }
        }
    }

    private fun showModelSelector() {
        // Find a view anchor for PopupMenu, or better yet, migrate PopupMenu to Compose DropdownMenu
        // Since we are in setContent, we can create a simple Compose implementation in ChatScreen, 
        // but to keep the scope of this refactor clean, we'll keep the basic concept.
        // Actually, let's just trigger a standard Android AlertDialog for simplicity here
        val configs = viewModel.uiState.value.apiConfigs
        if (configs.isEmpty()) return
        
        val names = configs.map { "${it.provider} | ${it.modelName}" }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Model")
            .setItems(names) { _, which ->
                viewModel.onModelSelected(configs[which])
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * D4: Android 13+ (API 33) 需要运行时请求通知权限
     * 首次打开 App 时弹出系统权限对话框
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}
