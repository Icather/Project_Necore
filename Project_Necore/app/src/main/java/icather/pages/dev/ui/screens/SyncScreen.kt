package icather.pages.dev.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import icather.pages.dev.sync.DiscoveredDevice
import icather.pages.dev.sync.SyncManifest
import icather.pages.dev.sync.SyncOptions
import icather.pages.dev.sync.SyncUiState
import icather.pages.dev.sync.SyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    viewModel: SyncViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("局域网同步") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetToIdle()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is SyncUiState.Idle -> RoleSelectionPanel(viewModel)
                is SyncUiState.SendSetup -> SendSetupPanel(viewModel)
                is SyncUiState.SendPreparing -> LoadingPanel(state.progress)
                is SyncUiState.Hosting -> HostingPanel(state, viewModel)
                is SyncUiState.SendVerify -> VerifyPanel(state.peerDeviceName, state.sasCode, onConfirm = { viewModel.confirmSendPairing() }, onReject = { viewModel.rejectSendPairing() })
                is SyncUiState.SendTransferring -> LoadingPanel(state.progress)
                is SyncUiState.SendComplete -> CompletePanel(state.message, viewModel)
                is SyncUiState.Discovering -> DiscoveringPanel(state.devices, viewModel)
                is SyncUiState.Connecting -> LoadingPanel("正在连接...")
                is SyncUiState.ReceiveVerify -> VerifyPanel(state.peerDeviceName, state.sasCode, onConfirm = { viewModel.confirmReceivePairing() }, onReject = { viewModel.cancelReceivePairing() })
                is SyncUiState.ReceiveSetup -> ReceiveSetupPanel(state.manifest, viewModel)
                is SyncUiState.Receiving -> LoadingPanel(state.progress)
                is SyncUiState.ReceiveComplete -> ReceiveCompletePanel(state, viewModel)
                is SyncUiState.Error -> ErrorPanel(state.message, viewModel)
            }
        }
    }
}

// ════════════════════════════════════════
//  面板组件
// ════════════════════════════════════════

@Composable
private fun RoleSelectionPanel(viewModel: SyncViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.SyncAlt, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("选择同步角色", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("确保两台设备在同一 WiFi 网络下", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        OutlinedCard(
            onClick = { viewModel.selectSendRole() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("发送数据", style = MaterialTheme.typography.titleMedium)
                    Text("将本机数据分享给另一台设备", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedCard(
            onClick = { viewModel.selectReceiveRole() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("接收数据", style = MaterialTheme.typography.titleMedium)
                    Text("从另一台设备接收数据", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SendSetupPanel(viewModel: SyncViewModel) {
    var sendChat by remember { mutableStateOf(true) }
    var sendApi by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("选择要分享的数据", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = sendChat, onCheckedChange = { sendChat = it })
            Spacer(Modifier.width(8.dp))
            Text("聊天记录", style = MaterialTheme.typography.bodyLarge)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = sendApi, onCheckedChange = { sendApi = it })
            Spacer(Modifier.width(8.dp))
            Text("API 配置", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                viewModel.updateSendOptions(sendChat, sendApi)
                viewModel.startSending()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = sendChat || sendApi
        ) { Text("开始同步") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { viewModel.resetToIdle() }, modifier = Modifier.fillMaxWidth()) { Text("返回") }
    }
}

@Composable
private fun HostingPanel(state: SyncUiState.Hosting, viewModel: SyncViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
        Text("等待对方连接...", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text("本机 IP: ${state.localIp}:${state.port}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(
            buildString {
                append("正在分享: ")
                val items = mutableListOf<String>()
                if (state.hasChatData) items.add("聊天记录")
                if (state.hasApiData) items.add("API 配置")
                append(items.joinToString("、"))
            },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = { viewModel.stopSending(); viewModel.resetToIdle() }) { Text("取消") }
    }
}

@Composable
private fun VerifyPanel(peerDeviceName: String, sasCode: String, onConfirm: () -> Unit, onReject: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("安全验证", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("设备: $peerDeviceName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text("请确认两台设备显示相同的验证码：", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        // 大号验证码显示
        Card(
            modifier = Modifier.padding(horizontal = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = sasCode.toList().joinToString("  "),
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("验证码一致，确认连接") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onReject, modifier = Modifier.fillMaxWidth()) { Text("取消") }
    }
}

@Composable
private fun DiscoveringPanel(devices: List<DiscoveredDevice>, viewModel: SyncViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("正在扫描局域网设备...", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(16.dp))

        if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("暂未发现设备\n请确认对方已开始同步", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(devices, key = { "${it.ipAddress}:${it.httpPort}" }) { device ->
                    OutlinedCard(
                        onClick = { viewModel.connectToDevice(device) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(device.deviceName, style = MaterialTheme.typography.titleSmall)
                                Text("${device.ipAddress} · v${device.appVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { viewModel.resetToIdle() }, modifier = Modifier.fillMaxWidth()) { Text("返回") }
    }
}

@Composable
private fun ReceiveSetupPanel(manifest: SyncManifest, viewModel: SyncViewModel) {
    var syncChat by remember { mutableStateOf(manifest.chatCount > 0) }
    var syncApi by remember { mutableStateOf(manifest.apiConfigCount > 0) }
    var overwrite by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("数据摘要", style = MaterialTheme.typography.headlineSmall)
        Text("来自: ${manifest.deviceName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        // 数据概览
        if (manifest.chatCount > 0) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = syncChat, onCheckedChange = { syncChat = it })
                Spacer(Modifier.width(8.dp))
                Text("聊天记录 (${manifest.chatCount} 个对话, ${manifest.messageCount} 条消息)")
            }
        }
        if (manifest.apiConfigCount > 0) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = syncApi, onCheckedChange = { syncApi = it })
                Spacer(Modifier.width(8.dp))
                Text("API 配置 (${manifest.apiConfigCount} 个)")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("同步模式", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth().clickable { overwrite = false }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = !overwrite, onClick = { overwrite = false })
            Spacer(Modifier.width(8.dp))
            Column {
                Text("增量同步", style = MaterialTheme.typography.bodyLarge)
                Text("保留现有数据，仅添加新内容", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth().clickable { overwrite = true }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = overwrite, onClick = { overwrite = true })
            Spacer(Modifier.width(8.dp))
            Column {
                Text("覆盖同步", style = MaterialTheme.typography.bodyLarge)
                Text("清空现有数据，完全替换", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.startReceiving(SyncOptions(syncChat, syncApi, overwrite)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = syncChat || syncApi
        ) { Text("开始接收") }
    }
}

@Composable
private fun ReceiveCompletePanel(state: SyncUiState.ReceiveComplete, viewModel: SyncViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("同步完成！", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        state.chatImported?.let { (convs, msgs) ->
            Text("✅ 导入 $convs 个对话, $msgs 条消息", style = MaterialTheme.typography.bodyMedium)
        }
        state.apiImported?.let { count ->
            Text("✅ 导入 $count 个 API 配置", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = { viewModel.resetToIdle() }, modifier = Modifier.fillMaxWidth()) { Text("完成") }
    }
}

@Composable
private fun CompletePanel(message: String, viewModel: SyncViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { viewModel.resetToIdle() }, modifier = Modifier.fillMaxWidth()) { Text("完成") }
    }
}

@Composable
private fun LoadingPanel(message: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorPanel(message: String, viewModel: SyncViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("❌", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { viewModel.resetToIdle() }, modifier = Modifier.fillMaxWidth()) { Text("返回") }
    }
}
