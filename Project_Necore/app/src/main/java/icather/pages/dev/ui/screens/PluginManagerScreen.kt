package icather.pages.dev.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import icather.pages.dev.api.plugin.PluginManagerViewModel
import icather.pages.dev.api.plugin.PluginManagerViewModel.PluginStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginManagerScreen(
    viewModel: PluginManagerViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 初始加载
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // Toast 消息消费
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("提供商协议插件") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.plugins.isEmpty()) {
                // 首次加载
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.errorMessage != null && uiState.plugins.isEmpty()) {
                // 错误状态
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.refresh() }) {
                        Text("重试")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // 顶部说明
                    item {
                        Text(
                            text = "从 GitHub 仓库下载协议插件，无需重新安装即可扩展模型支持。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // 分组标题：已安装
                    val installedPlugins = uiState.plugins.filter {
                        it.status is PluginStatus.BuiltIn || it.status is PluginStatus.Downloaded
                    }
                    if (installedPlugins.isNotEmpty()) {
                        item {
                            SectionHeader("已安装 (${installedPlugins.size})")
                        }
                        items(installedPlugins, key = { "installed_${it.fileName}" }) { plugin ->
                            PluginCard(
                                plugin = plugin,
                                onDownload = { viewModel.downloadPlugin(plugin.fileName) },
                                onDelete = { viewModel.deletePlugin(plugin.fileName) }
                            )
                        }
                    }

                    // 分组标题：可下载
                    val remotePlugins = uiState.plugins.filter {
                        it.status is PluginStatus.Remote
                    }
                    if (remotePlugins.isNotEmpty()) {
                        item {
                            SectionHeader("可下载 (${remotePlugins.size})")
                        }
                        items(remotePlugins, key = { "remote_${it.fileName}" }) { plugin ->
                            PluginCard(
                                plugin = plugin,
                                onDownload = { viewModel.downloadPlugin(plugin.fileName) },
                                onDelete = { viewModel.deletePlugin(plugin.fileName) }
                            )
                        }
                    }
                }

                // 顶部加载指示器（刷新时）
                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun PluginCard(
    plugin: PluginManagerViewModel.PluginItem,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标
            Icon(
                imageVector = when (plugin.status) {
                    is PluginStatus.BuiltIn -> Icons.Filled.Inventory2
                    is PluginStatus.Downloaded -> Icons.Filled.CheckCircle
                    is PluginStatus.Remote -> Icons.Filled.Extension
                },
                contentDescription = null,
                tint = when (plugin.status) {
                    is PluginStatus.BuiltIn -> MaterialTheme.colorScheme.primary
                    is PluginStatus.Downloaded -> MaterialTheme.colorScheme.tertiary
                    is PluginStatus.Remote -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 中间信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (plugin.status) {
                        is PluginStatus.BuiltIn -> "内置 · ${plugin.fileName}"
                        is PluginStatus.Downloaded -> "已下载 · ${plugin.fileName}"
                        is PluginStatus.Remote -> plugin.fileName
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 右侧操作按钮
            when (plugin.status) {
                is PluginStatus.BuiltIn -> {
                    // 内置插件不可操作
                    Text(
                        text = "内置",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is PluginStatus.Downloaded -> {
                    // 已下载：显示删除按钮
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is PluginStatus.Remote -> {
                    // 远程：显示下载按钮或加载中
                    if (plugin.isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onDownload) {
                            Icon(
                                Icons.Filled.CloudDownload,
                                contentDescription = "下载",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
