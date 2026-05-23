package icather.pages.dev.branch

/**
 * 分页数据集字段：代表一个对话分支节点的具体某一页（对应网页版的分页版本）
 */
data class BranchPage(
    val pageIndex: Int,              // 分页序号 (0-based，对应 branchIndex)
    val userMessageId: Long,         // 该页对应的用户消息在 DB 中的唯一 ID
    val userText: String,            // 该页对应的原始提问文本（高保真，不截断）
    val aiMessageId: Long?,          // 该页对应的 AI 回复在 DB 中的唯一 ID
    val aiText: String,              // 该页对应的 AI 回复文本内容
    val isStreaming: Boolean         // 该回复是否正处于流式生成状态
)

/**
 * 单节点数据字段：代表一轮完整的树状分支对话轮次
 */
data class BranchNode(
    val rootId: Long,                // 唯一提问 ID（即该对话轮次的分支根消息 ID）
    val originalText: String,        // 原始提问文本（用于快速提取和展示）
    val pages: List<BranchPage>,     // 回复分页数据集（包含该轮次的所有版本）
    val currentPageIndex: Int,       // 当前激活页序号 (0-based，对应 activeBranchMap 中的序号)
    val totalPageCount: Int,         // 回复总页数 (即该轮总版本数)
    val isActive: Boolean            // 节点激活状态（是否为当前主对话区正在聚焦的对话轮次）
)

/**
 * 顶层主题分支树：归集当前主题下的所有分支对话轮次
 */
data class TopicBranchTree(
    val conversationId: Long,        // 顶层索引：唯一对话主题 ID
    val nodes: List<BranchNode>,     // 归属于该主题的全部对话轮次节点列表（按时间正序排列）
    val activeNodeId: Long?          // 当前激活节点的 rootId
)
