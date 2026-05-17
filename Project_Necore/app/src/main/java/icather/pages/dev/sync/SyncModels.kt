package icather.pages.dev.sync

/**
 * 局域网同步 — 数据模型
 * 
 * 定义同步协议中使用的所有数据结构。
 */

/** 通过 UDP 广播发现的可用设备 */
data class DiscoveredDevice(
    val deviceName: String,
    val ipAddress: String,
    val httpPort: Int,
    val appVersion: String,
    val timestamp: Long = System.currentTimeMillis()
)

/** 发送方向接收方公布的数据摘要 */
data class SyncManifest(
    val deviceName: String,
    val chatCount: Int,
    val messageCount: Int,
    val apiConfigCount: Int,
    val appVersion: String
)

/** 同步过程中的会话状态 */
data class SyncSession(
    val sessionId: String,
    val peerPublicKey: String,     // Base64 编码的对方公钥
    val sasCode: String,           // 4 位验证码
    var senderConfirmed: Boolean? = null  // null=等待, true=接受, false=拒绝
)

/** 接收方选择的同步选项 */
data class SyncOptions(
    val syncChat: Boolean = true,
    val syncApi: Boolean = true,
    val overwrite: Boolean = false  // true=覆盖, false=增量
)

/** 同步 UI 状态机 */
sealed interface SyncUiState {
    /** 初始：选择角色（发送/接收） */
    data object Idle : SyncUiState

    // ———— 发送方状态 ————

    /** 发送方：选择要分享的数据类型 */
    data object SendSetup : SyncUiState

    /** 发送方：正在准备数据 */
    data class SendPreparing(val progress: String) : SyncUiState

    /** 发送方：广播中，等待对方连接 */
    data class Hosting(
        val localIp: String,
        val port: Int,
        val hasChatData: Boolean,
        val hasApiData: Boolean
    ) : SyncUiState

    /** 发送方：收到连接请求，显示 SAS 验证码等待确认 */
    data class SendVerify(
        val peerDeviceName: String,
        val sasCode: String
    ) : SyncUiState

    /** 发送方：数据传输中 */
    data class SendTransferring(val progress: String) : SyncUiState

    /** 发送方：传输完成 */
    data class SendComplete(val message: String) : SyncUiState

    // ———— 接收方状态 ————

    /** 接收方：正在扫描局域网设备 */
    data class Discovering(val devices: List<DiscoveredDevice>) : SyncUiState

    /** 接收方：正在连接并握手 */
    data object Connecting : SyncUiState

    /** 接收方：显示 SAS 验证码等待确认 */
    data class ReceiveVerify(
        val peerDeviceName: String,
        val sasCode: String
    ) : SyncUiState

    /** 接收方：已连接，选择要接收的数据和同步模式 */
    data class ReceiveSetup(val manifest: SyncManifest) : SyncUiState

    /** 接收方：正在接收数据 */
    data class Receiving(val progress: String) : SyncUiState

    /** 接收方：接收完成 */
    data class ReceiveComplete(
        val chatImported: Pair<Int, Int>?,  // (会话数, 消息数)
        val apiImported: Int?                // API 配置数
    ) : SyncUiState

    /** 错误状态 */
    data class Error(val message: String) : SyncUiState
}
