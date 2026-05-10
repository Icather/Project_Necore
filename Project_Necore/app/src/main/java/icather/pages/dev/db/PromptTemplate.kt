package icather.pages.dev.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * F1: Prompt 模板实体
 *
 * 用户可创建/编辑预设场景模板，快速切换对话角色。
 * 例如：翻译官、代码审查、写作助手等。
 */
@Entity(tableName = "prompt_templates")
data class PromptTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,           // 模板名称，如"翻译官"
    val icon: String = "✨",    // Emoji 图标
    val systemPrompt: String,   // System Prompt 内容
    val isBuiltIn: Boolean = false  // 是否为内置模板（不可删除）
)
