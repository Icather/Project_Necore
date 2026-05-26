package icather.pages.dev

import android.app.Application
import icather.pages.dev.api.plugin.ProtocolRegistry
import icather.pages.dev.soul.HeartbeatWorker

import android.app.Activity
import android.os.Bundle

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

        // 异形屏全局适配（允许横屏沉浸到刘海/水滴区域）
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val attributes = activity.window.attributes
                    attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    activity.window.attributes = attributes
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
