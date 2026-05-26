package icather.pages.dev

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.db.Conversation
import icather.pages.dev.ui.screens.HistoryScreen
import icather.pages.dev.ui.theme.Project_NecoreTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var isSelectionMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isSelectionMode = intent.getBooleanExtra("is_selection_mode", false)
        db = AppDatabase.getInstance(this)

        setContent {
            Project_NecoreTheme {
                var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
                var selectedIds by remember { mutableStateOf(setOf<Long>()) }
                var searchQuery by remember { mutableStateOf("") }
                var searchJob by remember { mutableStateOf<Job?>(null) }

                // 初始加载
                LaunchedEffect(Unit) {
                    conversations = db.conversationDao().getAllConversations()
                }

                HistoryScreen(
                    conversations = conversations,
                    isSelectionMode = isSelectionMode,
                    selectedIds = selectedIds,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query ->
                        searchQuery = query
                        searchJob?.cancel()
                        searchJob = lifecycleScope.launch {
                            delay(300) // 防抖
                            conversations = if (query.isBlank()) {
                                db.conversationDao().getAllConversations()
                            } else {
                                db.conversationDao().searchConversations(query)
                            }
                            selectedIds = emptySet()
                        }
                    },
                    onConversationClick = { id ->
                        val resultIntent = Intent().apply {
                            putExtra("CONVERSATION_ID", id)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onConversationLongClick = { conversation ->
                        showDeleteDialog(conversation) {
                            lifecycleScope.launch {
                                conversations = db.conversationDao().getAllConversations()
                            }
                        }
                    },
                    onToggleSelection = { id ->
                        selectedIds = if (selectedIds.contains(id)) {
                            selectedIds - id
                        } else {
                            selectedIds + id
                        }
                    },
                    onSelectAll = {
                        selectedIds = if (selectedIds.size == conversations.size) {
                            emptySet()
                        } else {
                            conversations.map { it.id }.toSet()
                        }
                    },
                    onConfirmExport = {
                        val resultIntent = Intent().apply {
                            putExtra("selected_ids", selectedIds.toLongArray())
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onClearHistory = {
                        lifecycleScope.launch {
                            db.conversationDao().clearAll()
                            db.messageDao().clearAll()
                            conversations = emptyList()
                            selectedIds = emptySet()
                        }
                    },
                    onNavigateBack = {
                        if (isSelectionMode) {
                            setResult(Activity.RESULT_CANCELED)
                        }
                        finish()
                    }
                )
            }
        }
    }

    private fun showDeleteDialog(conversation: Conversation, onDeleted: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_api_config_title)
            .setMessage(R.string.delete_api_config_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    db.conversationDao().deleteById(conversation.id)
                    db.messageDao().deleteByConversationId(conversation.id)
                    onDeleted()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
