package icather.pages.dev.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI 人设档案 (Identity Profile)
 * 
 * D3 灵魂组件库的核心数据模型。
 * 每个 Identity 代表一个独立的 AI 人格配置（如"默认助手"、"猫娘"、"英语老师"等）。
 * 切换 Identity 会改变注入到 System Prompt 中的人设指令。
 */
@Entity(tableName = "identities")
data class Identity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,           // 人设名称
    val systemPrompt: String,   // 核心人设提示词
    val greeting: String = "",  // 首次对话的开场白（可选）
    val isActive: Boolean = false
)
