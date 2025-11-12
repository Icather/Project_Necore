package icather.pages.dev

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import icather.pages.dev.db.ApiConfig
import icather.pages.dev.db.AppDatabase
import kotlinx.coroutines.launch

class NewApiActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_api)

        db = AppDatabase.getInstance(this)

        val apiProviderSpinner = findViewById<Spinner>(R.id.apiProviderSpinner)
        val modelTypeSpinner = findViewById<Spinner>(R.id.modelTypeSpinner)
        val apiNameEditText = findViewById<EditText>(R.id.apiNameEditText)
        val apiKeyEditText = findViewById<EditText>(R.id.apiKeyEditText)
        val saveApiButton = findViewById<Button>(R.id.saveApiButton)

        val providers = arrayOf("SiliconFlow", "DeepSeek")
        val providerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, providers)
        providerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        apiProviderSpinner.adapter = providerAdapter

        val modelTypes = arrayOf("Dialogue", "OCR", "Other")
        val modelTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelTypes)
        modelTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelTypeSpinner.adapter = modelTypeAdapter

        saveApiButton.setOnClickListener {
            val provider = apiProviderSpinner.selectedItem.toString()
            val modelType = modelTypeSpinner.selectedItem.toString()
            val name = apiNameEditText.text.toString()
            val key = apiKeyEditText.text.toString()

            if (name.isNotEmpty() && key.isNotEmpty()) {
                lifecycleScope.launch {
                    db.apiConfigDao().insert(ApiConfig(provider = provider, name = name, apiKey = key, modelType = modelType))
                    Toast.makeText(this@NewApiActivity, getString(R.string.api_saved), Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                Toast.makeText(this, getString(R.string.name_and_key_cannot_be_empty), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
