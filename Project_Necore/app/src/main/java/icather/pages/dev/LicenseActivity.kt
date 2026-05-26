package icather.pages.dev

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import icather.pages.dev.ui.screens.LicenseScreen
import icather.pages.dev.ui.theme.Project_NecoreTheme

class LicenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Project_NecoreTheme {
                LicenseScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
