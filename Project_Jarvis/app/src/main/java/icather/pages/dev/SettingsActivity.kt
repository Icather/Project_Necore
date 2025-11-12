package icather.pages.dev

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import icather.pages.dev.db.ApiConfig
import icather.pages.dev.db.AppDatabase
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    private val exportApiLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportApiConfigsToUri(it) }
    }

    private val importApiLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importApiConfigsFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        db = AppDatabase.getInstance(this)

        val apiConfig = findViewById<TextView>(R.id.apiConfig)
        apiConfig.setOnClickListener {
            val intent = Intent(this, ApiConfigActivity::class.java)
            startActivity(intent)
        }

        val exportApiConfigs = findViewById<TextView>(R.id.exportApiConfigs)
        exportApiConfigs.setOnClickListener {
            lifecycleScope.launch {
                val apiConfigs = db.apiConfigDao().getAllOnce()
                if (apiConfigs.isEmpty()) {
                    Toast.makeText(this@SettingsActivity, "No API configurations to export.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val json = Gson().toJson(apiConfigs)
                val hash = sha256(json).substring(0, 8) // Use a shorter hash for the filename
                val fileName = "配置API_${hash}.json"
                exportApiLauncher.launch(fileName)
            }
        }

        val importApiConfigs = findViewById<TextView>(R.id.importApiConfigs)
        importApiConfigs.setOnClickListener {
            importApiLauncher.launch(arrayOf("application/json"))
        }
    }

    private fun exportApiConfigsToUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val apiConfigs = db.apiConfigDao().getAllOnce()
                val json = Gson().toJson(apiConfigs)
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.writer().use {
                        it.write(json)
                    }
                }
                Toast.makeText(this@SettingsActivity, "Export successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importApiConfigsFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val json = contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                } ?: throw IllegalStateException("Could not read file")

                val type = object : TypeToken<List<ApiConfig>>() {}.type
                val importedConfigs: List<ApiConfig> = Gson().fromJson(json, type)

                val configsToInsert = importedConfigs.map { it.copy(id = 0) }

                // db.apiConfigDao().deleteAll() // This line is commented out to perform an incremental import
                db.apiConfigDao().insertAll(configsToInsert)

                Toast.makeText(this@SettingsActivity, "Import successful. ${configsToInsert.size} configurations imported.", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}
