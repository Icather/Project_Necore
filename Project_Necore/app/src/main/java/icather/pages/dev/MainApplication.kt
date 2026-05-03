package icather.pages.dev

import android.app.Application
import icather.pages.dev.api.plugin.ProtocolRegistry

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize dynamic protocols from assets
        ProtocolRegistry.init(this)
    }
}
