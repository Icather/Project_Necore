package icather.pages.dev

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import icather.pages.dev.db.ApiConfig
import icather.pages.dev.db.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ApiConfigActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var apiConfigAdapter: ApiConfigAdapter
    private lateinit var prefs: SharedPreferences

    companion object {
        const val DEFAULT_API_ID = 1L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_config)

        db = AppDatabase.getInstance(this)
        prefs = getSharedPreferences("api_prefs", Context.MODE_PRIVATE)

        val recyclerView = findViewById<RecyclerView>(R.id.apiRecyclerView)
        val activeConfigId = prefs.getLong("active_api_id", DEFAULT_API_ID)

        apiConfigAdapter = ApiConfigAdapter(emptyList(), activeConfigId, {
            prefs.edit().putLong("active_api_id", it.id).apply()
            loadApiConfigs()
        }, { apiConfig, _ ->
            deleteApiConfig(apiConfig)
        })

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = apiConfigAdapter

        val addApiButton = findViewById<FloatingActionButton>(R.id.addApiButton)
        addApiButton.setOnClickListener {
            val intent = Intent(this, NewApiActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadApiConfigs()
    }

    private fun loadApiConfigs() {
        lifecycleScope.launch {
            db.apiConfigDao().getAll().collectLatest { configs ->
                val activeId = prefs.getLong("active_api_id", DEFAULT_API_ID)
                apiConfigAdapter.updateData(configs, activeId)
            }
        }
    }

    private fun deleteApiConfig(apiConfig: ApiConfig) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_api_config_title)
            .setMessage(R.string.delete_api_config_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    db.apiConfigDao().deleteById(apiConfig.id)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
