package icather.pages.dev.memory

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * D3: 用户长期记忆管理器
 *
 * 管理存储在 app 内部文件 user_memory.json 中的用户长期记忆。
 * 参考 SillyTavern 的 User Persona + Lorebook 机制：
 * - 模型通过 Tool Calls 主动写入用户信息
 * - 每次对话开始时自动注入到 System Prompt
 *
 * JSON 结构：
 * {
 *   "user_name": "xxx",
 *   "memories": [
 *     {"key": "喜好", "value": "喜欢猫和编程", "timestamp": "2026-05-09"},
 *     {"key": "近况", "value": "正在准备高考", "timestamp": "2026-05-09"}
 *   ]
 * }
 */
class UserMemoryManager(private val context: Context) {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file: File get() = File(context.filesDir, "user_memory.json")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // 修复 #7: 线程安全锁 — 防止 HeartbeatWorker 和 ChatViewModel 并发读写导致数据损坏
    private val lock = Any()

    /**
     * 读取完整的用户记忆 JSON
     */
    fun read(): JsonObject = synchronized(lock) {
        try {
            if (file.exists()) {
                JsonParser.parseString(file.readText(Charsets.UTF_8)).asJsonObject
            } else {
                createDefault()
            }
        } catch (e: Exception) {
            createDefault()
        }
    }

    /**
     * 整体写入用户记忆
     */
    fun write(data: JsonObject) = synchronized(lock) {
        file.writer(Charsets.UTF_8).use { it.write(gson.toJson(data)) }
    }

    /**
     * 追加一条记忆（如果 key 已存在则更新）
     */
    fun addMemory(key: String, value: String) {
        val data = read()
        val memories = data.getAsJsonArray("memories") ?: JsonArray()

        // 查找是否已存在同名 key
        val existingIndex = (0 until memories.size()).firstOrNull { i ->
            memories[i].asJsonObject.get("key")?.asString == key
        }

        val entry = JsonObject().apply {
            addProperty("key", key)
            addProperty("value", value)
            addProperty("timestamp", dateFormat.format(Date()))
        }

        if (existingIndex != null) {
            memories.set(existingIndex, entry)
        } else {
            memories.add(entry)
        }

        data.add("memories", memories)
        write(data)
    }

    /**
     * 删除一条记忆
     */
    fun removeMemory(key: String) {
        val data = read()
        val memories = data.getAsJsonArray("memories") ?: return
        val filtered = JsonArray()
        for (i in 0 until memories.size()) {
            val entry = memories[i].asJsonObject
            if (entry.get("key")?.asString != key) {
                filtered.add(entry)
            }
        }
        data.add("memories", filtered)
        write(data)
    }

    /**
     * 设置用户名
     */
    fun setUserName(name: String) {
        val data = read()
        data.addProperty("user_name", name)
        write(data)
    }

    /**
     * 格式化为可注入 System Prompt 的文本
     * 输出示例：
     * [用户档案]
     * 用户名：小明
     * - 喜好：喜欢猫和编程 (2026-05-09)
     * - 近况：正在准备高考 (2026-05-09)
     */
    fun getFormattedForPrompt(): String {
        val data = read()
        val sb = StringBuilder()

        val userName = data.get("user_name")?.asString
        if (!userName.isNullOrBlank()) {
            sb.appendLine("用户名：$userName")
        }

        val memories = data.getAsJsonArray("memories")
        if (memories != null && memories.size() > 0) {
            for (i in 0 until memories.size()) {
                val entry = memories[i].asJsonObject
                val key = entry.get("key")?.asString ?: "未知"
                val value = entry.get("value")?.asString ?: ""
                val timestamp = entry.get("timestamp")?.asString ?: ""
                sb.appendLine("- $key：$value ($timestamp)")
            }
        }

        return sb.toString().trimEnd()
    }

    /**
     * 以 JSON 字符串形式返回（供 Tool Calls 的 read_user_memory 使用）
     */
    fun toJsonString(): String {
        return gson.toJson(read())
    }

    private fun createDefault(): JsonObject {
        val default = JsonObject().apply {
            addProperty("user_name", "")
            add("memories", JsonArray())
        }
        write(default)
        return default
    }
}
