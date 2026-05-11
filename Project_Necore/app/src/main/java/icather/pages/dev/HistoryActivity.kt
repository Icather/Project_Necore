package icather.pages.dev

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.db.Conversation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var db: AppDatabase
    private lateinit var historyAdapter: HistoryAdapter
    private var isSelectionMode = false
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        isSelectionMode = intent.getBooleanExtra("is_selection_mode", false)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (isSelectionMode) {
            toolbar.title = getString(R.string.select_conversations_to_export)
        }

        recyclerView = findViewById(R.id.historyRecyclerView)
        db = AppDatabase.getInstance(this)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // E5: 搜索功能
        val searchEditText = findViewById<TextInputEditText>(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                // 300ms 防抖
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    if (query.isEmpty()) {
                        loadHistory()
                    } else {
                        searchHistory(query)
                    }
                }
            }
        })

        loadHistory()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val selectAllItem = menu?.findItem(R.id.action_select_all)
        val confirmExportItem = menu?.findItem(R.id.action_confirm_export)
        val clearHistoryItem = menu?.findItem(R.id.action_clear_history)

        if (isSelectionMode) {
            selectAllItem?.isVisible = true
            confirmExportItem?.isVisible = true
            clearHistoryItem?.isVisible = false
            updateSelectAllButtonTitle()
        } else {
            selectAllItem?.isVisible = false
            confirmExportItem?.isVisible = false
            clearHistoryItem?.isVisible = true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (isSelectionMode) {
                    setResult(Activity.RESULT_CANCELED)
                }
                finish()
                true
            }
            R.id.action_clear_history -> {
                clearHistory()
                true
            }
            R.id.action_select_all -> {
                toggleSelectAll()
                true
            }
            R.id.action_confirm_export -> {
                confirmExport()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val conversations = db.conversationDao().getAllConversations()
            if (::historyAdapter.isInitialized) {
                historyAdapter.updateData(conversations)
            } else {
                historyAdapter = HistoryAdapter(conversations, isSelectionMode, {
                    if (!isSelectionMode) {
                        val resultIntent = Intent().apply {
                            putExtra("CONVERSATION_ID", it)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                }, { conversation, view ->
                    if (!isSelectionMode) {
                        showDeleteMenu(conversation, view)
                    }
                }) { // onSelectionChanged callback
                    updateSelectAllButtonTitle()
                }
                recyclerView.adapter = historyAdapter
            }
            updateSelectAllButtonTitle()
        }
    }

    private fun showDeleteMenu(conversation: Conversation, view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(getString(R.string.delete)).setOnMenuItemClickListener {
            deleteConversation(conversation)
            true
        }
        popup.show()
    }

    private fun deleteConversation(conversation: Conversation) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_api_config_title)
            .setMessage(R.string.delete_api_config_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    db.conversationDao().deleteById(conversation.id)
                    db.messageDao().deleteByConversationId(conversation.id)
                    loadHistory()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun clearHistory() {
        lifecycleScope.launch {
            db.conversationDao().clearAll()
            db.messageDao().clearAll()
            loadHistory()
        }
    }

    private fun toggleSelectAll() {
        if (historyAdapter.getSelectedItemCount() == historyAdapter.itemCount) {
            historyAdapter.deselectAll()
        } else {
            historyAdapter.selectAll()
        }
    }

    private fun confirmExport() {
        val selectedIds = historyAdapter.getSelectedItems()
        val resultIntent = Intent().apply {
            putExtra("selected_ids", selectedIds)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun updateSelectAllButtonTitle() {
        val selectAllItem = toolbar.menu.findItem(R.id.action_select_all)
        if (selectAllItem != null) {
            if (historyAdapter.getSelectedItemCount() == historyAdapter.itemCount && historyAdapter.itemCount > 0) {
                selectAllItem.title = getString(R.string.deselect_all)
            } else {
                selectAllItem.title = getString(R.string.select_all)
            }
        }
    }

    // E5: 搜索历史对话（标题 + 消息内容全文检索）
    private fun searchHistory(query: String) {
        lifecycleScope.launch {
            val results = db.conversationDao().searchConversations(query)
            if (::historyAdapter.isInitialized) {
                historyAdapter.updateData(results)
            }
        }
    }
}
