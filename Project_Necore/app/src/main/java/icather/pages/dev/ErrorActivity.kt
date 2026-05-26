package icather.pages.dev

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import icather.pages.dev.ui.screens.ErrorScreen
import icather.pages.dev.ui.theme.Project_NecoreTheme

class ErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val error = intent.getStringExtra("error") ?: "Unknown error"

        setContent {
            Project_NecoreTheme {
                ErrorScreen(
                    errorText = error,
                    onNavigateBack = { finish() },
                    onCopyClick = {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("error", error)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "Error copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
