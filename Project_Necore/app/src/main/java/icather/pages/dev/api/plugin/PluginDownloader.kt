package icather.pages.dev.api.plugin

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 协议插件网络下载器
 *
 * 仅从项目自身的 GitHub 仓库下载插件 JSON 文件。
 * 下载后存储在 app 私有目录 filesDir/plugins/ 中，
 * 由 ProtocolRegistry 在 init 时一并扫描加载。
 */
class PluginDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /** 本地外部插件存储目录 */
    val pluginsDir: File
        get() = File(context.filesDir, "plugins").also { if (!it.exists()) it.mkdirs() }

    // ===== GitHub Contents API 数据模型 =====

    data class GitHubFileEntry(
        val name: String,
        val path: String,
        val sha: String,
        val size: Long,
        @SerializedName("download_url") val downloadUrl: String?
    )

    /**
     * 从 GitHub API 获取 protocol_plugins/ 目录下的所有 JSON 文件列表。
     */
    suspend fun fetchRemotePluginList(): List<GitHubFileEntry> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(CONTENTS_API_URL)
            .addHeader("Accept", "application/vnd.github.v3+json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("GitHub API 请求失败: ${response.code} ${response.message}")
        }

        val body = response.body?.string() ?: throw Exception("响应体为空")
        val entries = gson.fromJson(body, Array<GitHubFileEntry>::class.java).toList()

        // 只返回 JSON 文件
        entries.filter { it.name.endsWith(".json") && it.downloadUrl != null }
    }

    /**
     * 下载单个插件文件到本地 plugins/ 目录。
     * 下载前会校验 JSON 结构合法性（必须包含 provider_info.id）。
     *
     * @return 解析出的 ProtocolPluginJson（已校验）
     */
    suspend fun downloadPlugin(entry: GitHubFileEntry): ProtocolPluginJson = withContext(Dispatchers.IO) {
        val downloadUrl = entry.downloadUrl
            ?: throw Exception("${entry.name} 无下载链接")

        val request = Request.Builder().url(downloadUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("下载失败: ${response.code} ${response.message}")
        }

        val jsonContent = response.body?.string() ?: throw Exception("下载内容为空")

        // 校验 JSON 结构：必须包含有效的 provider_info.id
        val config = try {
            gson.fromJson(jsonContent, ProtocolPluginJson::class.java)
        } catch (e: Exception) {
            throw Exception("JSON 解析失败: ${e.message}")
        }

        if (config?.providerInfo?.id.isNullOrBlank()) {
            throw Exception("无效的插件文件：缺少 provider_info.id")
        }
        if (config?.providerInfo?.baseUrl.isNullOrBlank()) {
            throw Exception("无效的插件文件：缺少 provider_info.base_url")
        }

        // 写入本地
        val targetFile = File(pluginsDir, entry.name)
        targetFile.writeText(jsonContent, Charsets.UTF_8)

        config
    }

    /**
     * 删除本地已下载的插件文件。
     */
    fun deletePlugin(fileName: String): Boolean {
        val file = File(pluginsDir, fileName)
        return if (file.exists()) file.delete() else false
    }

    /**
     * 获取本地已下载的插件文件名列表。
     */
    fun getLocalPluginFileNames(): Set<String> {
        return pluginsDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
    }

    /**
     * 获取 APK 内置 assets 中的插件文件名列表。
     */
    fun getBuiltInPluginFileNames(): Set<String> {
        return try {
            context.assets.list("")
                ?.filter { it.endsWith(".json") }
                ?.toSet()
                ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    companion object {
        private const val CONTENTS_API_URL =
            "https://api.github.com/repos/Icather/Project_Necore/contents/protocol_plugins"
    }
}
