package icather.pages.dev.sync

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import icather.pages.dev.BuildConfig
import icather.pages.dev.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 局域网同步 — ViewModel
 *
 * 协调发送/接收全流程，管理 SyncServer、SyncClient、UDP 广播生命周期。
 */
class SyncViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private var syncServer: SyncServer? = null
    private var broadcastHandle: NetworkUtil.BroadcastHandle? = null
    private var listenerHandle: NetworkUtil.BroadcastHandle? = null
    private var syncClient: SyncClient? = null
    private var currentJob: Job? = null

    // 发送方选项
    private var sendChat = true
    private var sendApi = true

    // 接收方连接信息
    private var connectedHost: String? = null
    private var connectedPort: Int? = null

    private val deviceName: String = Build.MODEL

    // ════════════════════════════════════════
    //  角色选择
    // ════════════════════════════════════════

    fun selectSendRole() {
        _uiState.value = SyncUiState.SendSetup
    }

    fun selectReceiveRole() {
        _uiState.value = SyncUiState.Discovering(emptyList())
        startDiscovery()
    }

    // ════════════════════════════════════════
    //  发送方流程
    // ════════════════════════════════════════

    fun updateSendOptions(chat: Boolean, api: Boolean) {
        sendChat = chat
        sendApi = api
    }

    /** 开始发送：准备数据 → 启动服务器 → 开始广播 */
    fun startSending() {
        if (!sendChat && !sendApi) {
            _uiState.value = SyncUiState.Error("请至少选择一种数据类型")
            return
        }

        currentJob = viewModelScope.launch {
            _uiState.value = SyncUiState.SendPreparing("正在准备数据...")

            try {
                // 在 IO 线程准备数据
                val chatJson = if (sendChat) {
                    withContext(Dispatchers.IO) { repository.getAllChatHistoryJson() }
                } else null

                val apiJson = if (sendApi) {
                    withContext(Dispatchers.IO) { repository.getApiConfigsJson() }
                } else null

                if (chatJson == null && apiJson == null) {
                    _uiState.value = SyncUiState.Error("没有可同步的数据")
                    return@launch
                }

                // 获取数据统计
                val (convCount, msgCount, apiCount) = withContext(Dispatchers.IO) {
                    repository.getSyncDataCounts()
                }

                val manifest = SyncManifest(
                    deviceName = deviceName,
                    chatCount = if (sendChat) convCount else 0,
                    messageCount = if (sendChat) msgCount else 0,
                    apiConfigCount = if (sendApi) apiCount else 0,
                    appVersion = BuildConfig.VERSION_NAME
                )

                // 启动 HTTP 服务器
                val server = SyncServer(
                    chatJson = chatJson,
                    apiJson = apiJson,
                    manifest = manifest,
                    onPairRequest = { peerName, sasCode ->
                        _uiState.value = SyncUiState.SendVerify(
                            peerDeviceName = peerName,
                            sasCode = sasCode
                        )
                    }
                )
                server.start()
                syncServer = server

                val localIp = NetworkUtil.getLocalIpAddress()
                if (localIp == null) {
                    server.stop()
                    _uiState.value = SyncUiState.Error("无法获取本机 IP，请确认已连接 WiFi")
                    return@launch
                }

                val port = server.listeningPort

                // 启动 UDP 广播
                broadcastHandle = NetworkUtil.startBroadcasting(
                    deviceName = deviceName,
                    httpPort = port,
                    appVersion = BuildConfig.VERSION_NAME
                )

                _uiState.value = SyncUiState.Hosting(
                    localIp = localIp,
                    port = port,
                    hasChatData = chatJson != null,
                    hasApiData = apiJson != null
                )
            } catch (e: Exception) {
                _uiState.value = SyncUiState.Error("启动同步服务失败: ${e.message}")
            }
        }
    }

    /** 发送方确认连接 */
    fun confirmSendPairing() {
        syncServer?.senderConfirmed = true
        _uiState.value = SyncUiState.SendTransferring("等待对方接收数据...")

        // 监控数据传输完成
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                while (syncServer?.dataTransferred != true &&
                    System.currentTimeMillis() - startTime < 300_000
                ) {
                    Thread.sleep(1000)
                }
            }
            if (syncServer?.dataTransferred == true) {
                _uiState.value = SyncUiState.SendComplete("数据同步完成！")
            }
            stopSending()
        }
    }

    /** 发送方拒绝连接 */
    fun rejectSendPairing() {
        syncServer?.senderConfirmed = false
        _uiState.value = SyncUiState.Hosting(
            localIp = NetworkUtil.getLocalIpAddress() ?: "",
            port = syncServer?.listeningPort ?: 0,
            hasChatData = sendChat,
            hasApiData = sendApi
        )
    }

    /** 停止发送服务 */
    fun stopSending() {
        broadcastHandle?.stop()
        broadcastHandle = null
        syncServer?.stop()
        syncServer = null
    }

    // ════════════════════════════════════════
    //  接收方流程
    // ════════════════════════════════════════

    /** 开始扫描局域网设备 */
    private fun startDiscovery() {
        val discoveredDevices = mutableMapOf<String, DiscoveredDevice>()

        listenerHandle = NetworkUtil.startListening { device ->
            val key = "${device.ipAddress}:${device.httpPort}"
            discoveredDevices[key] = device
            // 更新 UI（过滤超时设备）
            val now = System.currentTimeMillis()
            val activeDevices = discoveredDevices.values
                .filter { now - it.timestamp < 10_000 }
                .toList()
            _uiState.value = SyncUiState.Discovering(activeDevices)
        }
    }

    /** 接收方点击设备，发起连接 */
    fun connectToDevice(device: DiscoveredDevice) {
        listenerHandle?.stop()
        listenerHandle = null
        _uiState.value = SyncUiState.Connecting

        connectedHost = device.ipAddress
        connectedPort = device.httpPort

        currentJob = viewModelScope.launch {
            val client = SyncClient()
            syncClient = client

            val result = withContext(Dispatchers.IO) {
                client.performHandshake(
                    host = device.ipAddress,
                    port = device.httpPort,
                    deviceName = deviceName
                )
            }

            if (result == null) {
                _uiState.value = SyncUiState.Error("连接失败，请确认对方设备仍在同步")
                return@launch
            }

            _uiState.value = SyncUiState.ReceiveVerify(
                peerDeviceName = device.deviceName,
                sasCode = result.sasCode
            )
        }
    }

    /** 接收方确认 SAS 验证码一致 */
    fun confirmReceivePairing() {
        val host = connectedHost ?: return
        val port = connectedPort ?: return
        val client = syncClient ?: return

        currentJob = viewModelScope.launch {
            _uiState.value = SyncUiState.Receiving("等待对方确认...")

            // 轮询发送方确认
            val status = withContext(Dispatchers.IO) {
                client.pollConfirmation(host, port)
            }

            when (status) {
                "accepted" -> {
                    // 获取 manifest
                    val manifest = withContext(Dispatchers.IO) {
                        client.fetchManifest(host, port)
                    }
                    if (manifest != null) {
                        _uiState.value = SyncUiState.ReceiveSetup(manifest)
                    } else {
                        _uiState.value = SyncUiState.Error("获取数据摘要失败")
                    }
                }
                "rejected" -> _uiState.value = SyncUiState.Error("对方已拒绝连接")
                "timeout" -> _uiState.value = SyncUiState.Error("等待确认超时")
                else -> _uiState.value = SyncUiState.Error("连接异常")
            }
        }
    }

    /** 接收方取消连接 */
    fun cancelReceivePairing() {
        currentJob?.cancel()
        _uiState.value = SyncUiState.Discovering(emptyList())
        startDiscovery()
    }

    /** 接收方开始接收数据 */
    fun startReceiving(options: SyncOptions) {
        val host = connectedHost ?: return
        val port = connectedPort ?: return
        val client = syncClient ?: return

        currentJob = viewModelScope.launch {
            _uiState.value = SyncUiState.Receiving("正在接收数据...")

            var chatResult: Pair<Int, Int>? = null
            var apiResult: Int? = null

            try {
                // 接收聊天记录
                if (options.syncChat) {
                    _uiState.value = SyncUiState.Receiving("正在接收聊天记录...")
                    val chatJson = withContext(Dispatchers.IO) {
                        client.fetchData(host, port, "chat")
                    }
                    if (chatJson != null) {
                        val result = withContext(Dispatchers.IO) {
                            repository.importChatHistoryFromJson(chatJson, options.overwrite)
                        }
                        chatResult = result.getOrNull()
                    }
                }

                // 接收 API 配置
                if (options.syncApi) {
                    _uiState.value = SyncUiState.Receiving("正在接收 API 配置...")
                    val apiJson = withContext(Dispatchers.IO) {
                        client.fetchData(host, port, "api")
                    }
                    if (apiJson != null) {
                        val result = withContext(Dispatchers.IO) {
                            repository.importApiConfigsFromJson(apiJson)
                        }
                        apiResult = result.getOrNull()
                    }
                }

                _uiState.value = SyncUiState.ReceiveComplete(
                    chatImported = chatResult,
                    apiImported = apiResult
                )
            } catch (e: Exception) {
                _uiState.value = SyncUiState.Error("接收数据失败: ${e.message}")
            }
        }
    }

    // ════════════════════════════════════════
    //  通用操作
    // ════════════════════════════════════════

    fun resetToIdle() {
        cleanup()
        _uiState.value = SyncUiState.Idle
    }

    private fun cleanup() {
        currentJob?.cancel()
        broadcastHandle?.stop()
        listenerHandle?.stop()
        syncServer?.stop()
        broadcastHandle = null
        listenerHandle = null
        syncServer = null
        syncClient = null
        connectedHost = null
        connectedPort = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SyncViewModel(repository) as T
        }
    }
}
