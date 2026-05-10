package icather.pages.dev.soul

/**
 * D4: 情绪状态密封类 + 情绪标签解析器
 *
 * 参考 SillyTavern 的做法：情感不是模型的内部状态，
 * 而是通过"外部分类 + 视觉反馈"实现的体验增强。
 *
 * Necore 的简化实现：
 * 让模型在回复末尾自报情绪标签 [emotion:xxx]，
 * 解析器提取标签并映射到对应的 EmotionState。
 *
 * 第零法则：密封类保证所有情绪类型在 when 中被穷举处理。
 */
sealed class EmotionState(val name: String, val emoji: String) {
    data object Neutral : EmotionState("neutral", "😐")
    data object Happy : EmotionState("happy", "😊")
    data object Excited : EmotionState("excited", "🤩")
    data object Thinking : EmotionState("thinking", "🤔")
    data object Sad : EmotionState("sad", "😢")
    data object Worried : EmotionState("worried", "😟")
    data object Angry : EmotionState("angry", "😠")
    data object Surprised : EmotionState("surprised", "😲")
    data object Shy : EmotionState("shy", "😳")
    data object Loving : EmotionState("loving", "🥰")

    companion object {
        /** 所有已注册的情绪状态（用于 UI 显示和匹配） */
        val all: List<EmotionState> = listOf(
            Neutral, Happy, Excited, Thinking, Sad,
            Worried, Angry, Surprised, Shy, Loving
        )

        /** 根据名称查找情绪状态 */
        fun fromName(name: String): EmotionState {
            return all.find { it.name.equals(name, ignoreCase = true) } ?: Neutral
        }
    }
}

/**
 * 情绪标签解析器
 *
 * 从模型回复文本中提取 [emotion:xxx] 标签。
 * 标签会被从显示文本中移除，只保留纯内容。
 */
object EmotionParser {

    private val EMOTION_PATTERN = Regex("""\[emotion:(\w+)]""", RegexOption.IGNORE_CASE)

    /**
     * 解析结果：包含清理后的文本和检测到的情绪
     */
    data class ParseResult(
        val cleanText: String,
        val emotion: EmotionState
    )

    /**
     * 从文本中解析情绪标签
     *
     * 示例输入：  "你好呀！今天天气真好 [emotion:happy]"
     * 示例输出：  ParseResult(cleanText="你好呀！今天天气真好", emotion=Happy)
     */
    fun parse(text: String): ParseResult {
        val match = EMOTION_PATTERN.find(text)
        return if (match != null) {
            val emotionName = match.groupValues[1]
            val cleanText = text.replace(match.value, "").trim()
            ParseResult(cleanText, EmotionState.fromName(emotionName))
        } else {
            ParseResult(text, EmotionState.Neutral)
        }
    }

    /**
     * 生成注入到 System Prompt 中的情绪引导指令
     * 告诉模型在回复末尾附加情绪标签
     */
    fun getEmotionPromptInjection(): String {
        val emotionList = EmotionState.all.joinToString(", ") { it.name }
        return """
            |[情绪表达指令]
            |请在每次回复的末尾添加一个情绪标签来表达你当前的情绪状态。
            |格式：[emotion:情绪名称]
            |可用情绪：$emotionList
            |示例：你好呀！今天天气真好呢~ [emotion:happy]
            |注意：标签必须放在回复的最末尾，且每次回复只能有一个标签。
        """.trimMargin()
    }
}
