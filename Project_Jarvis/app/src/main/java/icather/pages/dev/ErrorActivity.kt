package icather.pages.dev

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)

        val errorTextView = findViewById<TextView>(R.id.errorTextView)
        val copyButton = findViewById<Button>(R.id.copyButton)

        val error = intent.getStringExtra("error")
        errorTextView.text = error

        copyButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("error", error)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Error copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
}
