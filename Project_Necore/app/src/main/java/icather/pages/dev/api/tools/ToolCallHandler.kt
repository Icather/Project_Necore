package icather.pages.dev.api.tools

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import icather.pages.dev.memory.UserMemoryManager

/**
 * D3: Tool Calls 处理引擎
 *
 * 实现 OpenAI 格式的 Tool Calls 循环：
 * 1. App → 发送消息（含 tools 定义）→ LLM
 * 2. LLM → 返回 tool_call（函数名 + 参数）→ App
 * 3. App → 执行本地函数 → 拿到结果
 * 4. App → 将结果以 tool role 回传 → LLM
 * 5. LLM → 根据结果生成最终回复
 *
 * 第零法则：使用密封接口穷举所有已注册的工具，编译期保证不遗漏。
 */
class ToolCallHandler(private val memoryManager: UserMemoryManager) {

    /**
     * 已注册的工具定义密封类
     */
    sealed interface RegisteredTool {
        val functionName: String
        data object ReadUserMemory : RegisteredTool {
            override val functionName = "read_user_memory"
        }
        data object WriteUserMemory : RegisteredTool {
            override val functionName = "write_user_memory"
        }
    }

    /**
     * 生成注入到 API 请求中的 tools 定义 JSON 数组
     */
    fun getToolDefinitions(): JsonArray {
        return JsonArray().apply {
            add(JsonObject().apply {
                addProperty("type", "function")
                add("function", JsonObject().apply {
                    addProperty("name", "read_user_memory")
                    addProperty("description", "读取用户的长期记忆档案，了解用户的偏好、习惯和过往对话中提到的重要信息。在你想更好地了解用户时调用。")
                    add("parameters", JsonObject().apply {
                        addProperty("type", "object")
                        add("properties", JsonObject())
                    })
                })
            })
            add(JsonObject().apply {
                addProperty("type", "function")
                add("function", JsonObject().apply {
                    addProperty("name", "write_user_memory")
                    addProperty("description", "将用户的重要信息写入长期记忆档案，以便在未来的对话中记住。当用户提到关于自己的重要信息（如名字、喜好、近况、重要事件）时主动调用。")
                    add("parameters", JsonObject().apply {
                        addProperty("type", "object")
                        add("properties", JsonObject().apply {
                            add("key", JsonObject().apply {
                                addProperty("type", "string")
                                addProperty("description", "记忆条目的标识名（如'姓名'、'喜好'、'近况'）")
                            })
                            add("value", JsonObject().apply {
                                addProperty("type", "string")
                                addProperty("description", "记忆内容的具体描述")
                            })
                        })
                        add("required", JsonArray().apply {
                            add("key")
                            add("value")
                        })
                    })
                })
            })
        }
    }

    /**
     * 执行指定的 tool call 并返回结果字符串
     *
     * @param functionName 函数名
     * @param arguments JSON 格式的参数字符串
     * @return 执行结果（将作为 tool role 消息回传给 LLM）
     */
    fun executeToolCall(functionName: String, arguments: String): String {
        return when (functionName) {
            RegisteredTool.ReadUserMemory.functionName -> {
                val memoryJson = memoryManager.toJsonString()
                if (memoryJson.isBlank() || memoryJson == "{}") {
                    "用户记忆档案为空，暂无任何已记录的信息。"
                } else {
                    memoryJson
                }
            }
            RegisteredTool.WriteUserMemory.functionName -> {
                try {
                    val args = JsonParser.parseString(arguments).asJsonObject
                    val key = args.get("key")?.asString ?: return "错误：缺少 key 参数"
                    val value = args.get("value")?.asString ?: return "错误：缺少 value 参数"
                    memoryManager.addMemory(key, value)
                    "已成功将 '$key' 写入用户记忆档案。"
                } catch (e: Exception) {
                    "写入失败：${e.message}"
                }
            }
            else -> "未知的工具函数：$functionName"
        }
    }

    /**
     * 数据类：表示 LLM 返回的一个 tool_call 请求
     */
    data class ToolCallRequest(
        val id: String,
        val functionName: String,
        val arguments: String
    )

    /**
     * 从 OpenAI 格式的非流式响应中解析 tool_calls
     */
    fun parseToolCalls(responseJson: JsonObject): List<ToolCallRequest> {
        val choices = responseJson.getAsJsonArray("choices") ?: return emptyList()
        val firstChoice = choices.firstOrNull()?.asJsonObject ?: return emptyList()
        val message = firstChoice.getAsJsonObject("message") ?: return emptyList()
        val toolCalls = message.getAsJsonArray("tool_calls") ?: return emptyList()

        return toolCalls.map { tc ->
            val tcObj = tc.asJsonObject
            val function = tcObj.getAsJsonObject("function")
            ToolCallRequest(
                id = tcObj.get("id")?.asString ?: "",
                functionName = function.get("name")?.asString ?: "",
                arguments = function.get("arguments")?.asString ?: "{}"
            )
        }
    }
}
