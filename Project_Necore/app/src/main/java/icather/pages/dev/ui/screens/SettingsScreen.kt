package icather.pages.dev.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import icather.pages.dev.R
import icather.pages.dev.settings.SettingsEvent
import icather.pages.dev.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToApiConfig: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToIdentity: () -> Unit = {},
    onNavigateToUsage: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onLanguageClick: () -> Unit,
    onImportApiClick: () -> Unit,
    onExportChatClick: () -> Unit,
    onImportChatClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.TriggerExportApiConfigs -> {
                    // This is usually handled back in Activity for launching Intent, 
                    // but we can trigger a callback here if needed.
                }
                is SettingsEvent.TriggerExportChatHistory -> {
                    // Handled back in Activity
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    SettingsItem(
                        icon = Icons.Filled.Language,
                        title = stringResource(id = R.string.language),
                        onClick = onLanguageClick
                    )
                    Divider()
                }
                item {
                    SettingsItem(
                        icon = Icons.Filled.Settings,
                        title = stringResource(id = R.string.api_configuration),
                        onClick = onNavigateToApiConfig
                    )
                    HorizontalDivider()
                }
                item {
                    SettingsItem(
                        icon = Icons.Filled.Extension,
                        title = "提供商协议插件",
                        onClick = onNavigateToPlugins
                    )
                    HorizontalDivider()
                }
                // F4: 用量统计入口
                item {
                    SettingsItem(
                        icon = Icons.Filled.BarChart,
                        title = "用量统计",
                        onClick = onNavigateToUsage
                    )
                    HorizontalDivider()
                }
                // F1: Prompt 模板入口
                item {
                    SettingsItem(
                        icon = Icons.Filled.AutoFixHigh,
                        title = "Prompt 模板",
                        onClick = onNavigateToTemplates
                    )
                    HorizontalDivider()
                }
                // D2: 图片压缩开关
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "图片自动压缩",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.isImageCompressionEnabled) "已开启 · 自动压缩至安全分辨率" else "已关闭 · 限制总大小 20MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isImageCompressionEnabled,
                            onCheckedChange = { viewModel.setImageCompressionEnabled(it) }
                        )
                    }
                    HorizontalDivider()
                }
                // D3: AI 人设系统开关
                item {
                    SoulToggleItem(
                        icon = Icons.Filled.Person,
                        title = "AI 人设系统",
                        subtitle = if (uiState.isIdentityEnabled) "已开启 · 可切换不同AI人格" else "已关闭 · 使用默认行为",
                        checked = uiState.isIdentityEnabled,
                        onCheckedChange = { viewModel.setIdentityEnabled(it) }
                    )
                    HorizontalDivider()
                }
                // D3: 人设管理入口
                item {
                    SettingsItem(
                        icon = Icons.Filled.ManageAccounts,
                        title = "管理 AI 人设",
                        onClick = onNavigateToIdentity
                    )
                    HorizontalDivider()
                }
                // D3: 长期记忆开关
                item {
                    SoulToggleItem(
                        icon = Icons.Filled.Psychology,
                        title = "长期记忆",
                        subtitle = if (uiState.isMemoryEnabled) "已开启 · AI会记住你的偏好" else "已关闭 · 每次对话独立",
                        checked = uiState.isMemoryEnabled,
                        onCheckedChange = { viewModel.setMemoryEnabled(it) }
                    )
                    HorizontalDivider()
                }
                // D4: 情绪感知开关
                item {
                    SoulToggleItem(
                        icon = Icons.Filled.Favorite,
                        title = "情绪感知",
                        subtitle = if (uiState.isEmotionEnabled) "已开启 · AI会表达情绪变化" else "已关闭 · 纯理性模式",
                        checked = uiState.isEmotionEnabled,
                        onCheckedChange = { viewModel.setEmotionEnabled(it) }
                    )
                    HorizontalDivider()
                }
                item {
                    SettingsItem(
                        icon = Icons.Filled.Upload,
                        title = stringResource(id = R.string.export_api_configs),
                        onClick = { viewModel.prepareExportApiConfigs() }
                    )
                    Divider()
                }
                item {
                    SettingsItem(
                        icon = Icons.Filled.Download,
                        title = stringResource(id = R.string.import_api_configs),
                        onClick = onImportApiClick
                    )
                    Divider()
                }
                item {
                    SettingsItem(
                        icon = Icons.Filled.Backup,
                        title = stringResource(id = R.string.export_chat_history),
                        onClick = onExportChatClick
                    )
                    Divider()
                }
                item {
                    SettingsItem(
                        icon = Icons.Filled.Restore,
                        title = stringResource(id = R.string.import_chat_history),
                        onClick = onImportChatClick
                    )
                    Divider()
                }
                item {
                    SettingsItem(
                        icon = Icons.Filled.Info,
                        title = stringResource(id = R.string.about),
                        onClick = onNavigateToAbout
                    )
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * D3/D4 灵魂引擎专用开关组件
 * 复用图片压缩开关的布局风格，保持设置页 UI 一致性
 */
@Composable
fun SoulToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
