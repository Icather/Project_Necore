package icather.pages.dev

import android.app.Application
import android.content.Intent
import java.io.PrintWriter
import java.io.StringWriter

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, ex ->
            val intent = Intent(this, ErrorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val stringWriter = StringWriter()
            ex.printStackTrace(PrintWriter(stringWriter))
            intent.putExtra("error", stringWriter.toString())
            startActivity(intent)
            System.exit(1)
        }
    }
}
