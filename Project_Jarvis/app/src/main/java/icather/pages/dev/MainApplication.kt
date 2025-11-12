package icather.pages.dev

import android.app.Application

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // The database and default configs are now created in AppDatabase.kt
    }
}
