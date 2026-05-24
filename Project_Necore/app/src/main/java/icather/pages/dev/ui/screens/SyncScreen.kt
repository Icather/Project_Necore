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
import androidx.compose.ui.res.stringResource
import icather.pages.dev.R
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
                title = { Text(stringResource(R.string.sync_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetToIdle()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                is SyncUiState.Connecting -> LoadingPanel(stringResource(R.string.sync_connecting))
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
        Text(stringResource(R.string.sync_choose_role), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.sync_same_wifi), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        OutlinedCard(
            onClick = { viewModel.selectSendRole() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.sync_send_data), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.sync_send_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(stringResource(R.string.sync_receive_data), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.sync_receive_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text(stringResource(R.string.sync_choose_data), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = sendChat, onCheckedChange = { sendChat = it })
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.sync_chat_records), style = MaterialTheme.typography.bodyLarge)
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
        ) { Text(stringResource(R.string.sync_start)) }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { viewModel.resetToIdle() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun HostingPanel(state: SyncUiState.Hosting, viewModel: SyncViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.sync_waiting), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.sync_local_ip, state.localIp, state.port), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        val sharingLabel = stringResource(R.string.sync_sharing_label)
        val chatLabel = stringResource(R.string.sync_chat_records)
        val apiLabel = stringResource(R.string.api_configuration)
        Text(
            buildString {
                append(sharingLabel)
                val items = mutableListOf<String>()
                if (state.hasChatData) items.add(chatLabel)
                if (state.hasApiData) items.add(apiLabel)
                append(items.joinToString(", "))
            },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = { viewModel.stopSending(); viewModel.resetToIdle() }) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
private fun VerifyPanel(peerDeviceName: String, sasCode: String, onConfirm: () -> Unit, onReject: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.sync_security_verify), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.sync_device_label, peerDeviceName), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.sync_confirm_code), style = MaterialTheme.typography.bodyMedium)
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
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sync_code_match)) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onReject, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
private fun DiscoveringPanel(devices: List<DiscoveredDevice>, viewModel: SyncViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.sync_scanning), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(16.dp))

        if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.sync_no_devices), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        OutlinedButton(onClick = { viewModel.resetToIdle() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun ReceiveSetupPanel(manifest: SyncManifest, viewModel: SyncViewModel) {
    var syncChat by remember { mutableStateOf(manifest.chatCount > 0) }
    var syncApi by remember { mutableStateOf(manifest.apiConfigCount > 0) }
    var overwrite by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.sync_data_summary), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.sync_from_device, manifest.deviceName), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        // 数据概览
        if (manifest.chatCount > 0) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = syncChat, onCheckedChange = { syncChat = it })
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sync_chat_detail, manifest.chatCount, manifest.messageCount))
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
        Text(stringResource(R.string.sync_mode), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth().clickable { overwrite = false }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = !overwrite, onClick = { overwrite = false })
            Spacer(Modifier.width(8.dp))
            Column {
                Text(stringResource(R.string.sync_incremental), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.sync_incremental_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth().clickable { overwrite = true }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = overwrite, onClick = { overwrite = true })
            Spacer(Modifier.width(8.dp))
            Column {
                Text(stringResource(R.string.sync_overwrite), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.sync_overwrite_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.startReceiving(SyncOptions(syncChat, syncApi, overwrite)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = syncChat || syncApi
        ) { Text(stringResource(R.string.sync_start_receive)) }
    }
}

@Composable
private fun ReceiveCompletePanel(state: SyncUiState.ReceiveComplete, viewModel: SyncViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.sync_complete), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        state.chatImported?.let { (convs, msgs) ->
            Text("✅ 导入 $convs 个对话, $msgs 条消息", style = MaterialTheme.typography.bodyMedium)
        }
        state.apiImported?.let { count ->
            Text("✅ 导入 $count 个 API 配置", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = { viewModel.resetToIdle() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sync_done)) }
    }
}

@Composable
private fun CompletePanel(message: String, viewModel: SyncViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { viewModel.resetToIdle() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sync_done)) }
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
        Button(onClick = { viewModel.resetToIdle() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
    }
}
