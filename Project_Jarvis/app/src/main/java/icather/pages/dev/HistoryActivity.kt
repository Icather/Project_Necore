package icather.pages.dev

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.db.Conversation
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var db: AppDatabase
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.historyRecyclerView)
        db = AppDatabase.getInstance(this)

        recyclerView.layoutManager = LinearLayoutManager(this)

        loadHistory()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_clear_history -> {
                clearHistory()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val conversations = db.conversationDao().getAllConversations()
            historyAdapter = HistoryAdapter(conversations, {
                val resultIntent = Intent().apply {
                    putExtra("CONVERSATION_ID", it)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }, { conversation, view ->
                showDeleteMenu(conversation, view)
            })
            recyclerView.adapter = historyAdapter
        }
    }

    private fun showDeleteMenu(conversation: Conversation, view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Delete").setOnMenuItemClickListener {
            deleteConversation(conversation)
            true
        }
        popup.show()
    }

    private fun deleteConversation(conversation: Conversation) {
        AlertDialog.Builder(this)
            .setTitle("Delete Conversation")
            .setMessage("Are you sure you want to delete this conversation?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.conversationDao().deleteById(conversation.id)
                    db.messageDao().deleteByConversationId(conversation.id)
                    loadHistory()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearHistory() {
        lifecycleScope.launch {
            db.conversationDao().clearAll()
            db.messageDao().clearAll()
            loadHistory()
        }
    }
}
