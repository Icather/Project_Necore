package icather.pages.dev

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import icather.pages.dev.ui.screens.AboutScreen
import icather.pages.dev.ui.theme.Project_NecoreTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class AboutActivity : AppCompatActivity() {

    private var downloadId: Long = -1
    private lateinit var downloadManager: DownloadManager

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                val downloadedFileUri = downloadManager.getUriForDownloadedFile(downloadId)
                if (downloadedFileUri != null) {
                    installApk(downloadedFileUri)
                } else {
                    Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        } catch (e: PackageManager.NameNotFoundException) {
            "?"
        }

        setContent {
            Project_NecoreTheme {
                AboutScreen(
                    versionName = versionName,
                    onNavigateBack = { finish() },
                    onProjectHomeClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Icather/Project_Necore".toUri())
                        startActivity(intent)
                    },
                    onOpenSourceLicensesClick = {
                        startActivity(Intent(this@AboutActivity, LicenseActivity::class.java))
                    },
                    onCheckForUpdatesClick = {
                        Toast.makeText(this@AboutActivity, R.string.checking_for_updates, Toast.LENGTH_SHORT).show()
                        checkForUpdates()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }

    /**
     * 通过 GitHub Releases API 检查最新版本。
     * 从 tag_name 获取版本号，从 assets 获取 APK 下载链接。
     */
    private fun checkForUpdates() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = URL(RELEASES_API_URL).readText()
                val release = Gson().fromJson(response, GitHubRelease::class.java)

                val latestVersion = release.tagName.removePrefix("v")
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val currentVersion = packageInfo.versionName

                withContext(Dispatchers.Main) {
                    if (currentVersion != null && isNewerVersion(latestVersion, currentVersion)) {
                        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                        if (apkAsset != null) {
                            showUpdateDialog(latestVersion, release.body, apkAsset.downloadUrl)
                        } else {
                            Toast.makeText(this@AboutActivity, "新版本暂无可用安装包", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@AboutActivity, R.string.latest_version, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AboutActivity, R.string.check_for_updates_failed, Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    /**
     * 展示更新确认对话框，包含 Release Notes。
     */
    private fun showUpdateDialog(version: String, releaseNotes: String?, downloadUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("发现新版本 v$version")
            .setMessage(releaseNotes ?: "有新版本可用，是否立即更新？")
            .setPositiveButton("立即更新") { _, _ ->
                downloadAndInstallUpdate(downloadUrl)
            }
            .setNegativeButton("稍后", null)
            .show()
    }

    private fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
        val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }

        val commonLength = minOf(latestParts.size, currentParts.size)

        for (i in 0 until commonLength) {
            if (latestParts[i] > currentParts[i]) {
                return true
            }
            if (latestParts[i] < currentParts[i]) {
                return false
            }
        }

        return latestParts.size > currentParts.size
    }

    private fun downloadAndInstallUpdate(apkUrl: String) {
        val destination = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "necore-update.apk")
        // 清理旧的下载文件
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(apkUrl.toUri())
            .setTitle(getString(R.string.updating_necore))
            .setDescription(getString(R.string.downloading_update))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        downloadId = downloadManager.enqueue(request)
    }

    private fun installApk(uri: Uri) {
        val contentUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", File(uri.path!!))
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    // ===== GitHub Releases API 数据模型 =====

    private data class GitHubRelease(
        @SerializedName("tag_name") val tagName: String,
        val name: String?,
        val body: String?,
        val assets: List<GitHubAsset>
    )

    private data class GitHubAsset(
        val name: String,
        @SerializedName("browser_download_url") val downloadUrl: String,
        val size: Long
    )

    companion object {
        private const val RELEASES_API_URL = "https://api.github.com/repos/Icather/Project_Necore/releases/latest"
    }
}
