package icather.pages.dev

import android.app.Application
import icather.pages.dev.api.plugin.ProtocolRegistry
import icather.pages.dev.soul.HeartbeatWorker

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize dynamic protocols from assets
        ProtocolRegistry.init(this)

        // D4: 启动心跳关怀引擎（仅在情绪感知开关开启时生效）
        val prefs = getSharedPreferences("api_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("emotion_enabled", true)) {
            HeartbeatWorker.schedule(this)
        }
    }
}
