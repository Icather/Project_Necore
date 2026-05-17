package icather.pages.dev.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    onNavigateToSync: () -> Unit = {},
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
                    // Handled back in Activity
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
                title = {
                    Text(
                        text = stringResource(id = R.string.settings),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ═══════════════════════════════════════
                // 第一组：模型与服务
                // ═══════════════════════════════════════
                item {
                    SettingsGroup(title = "模型与服务") {
                        SettingsNavItem(
                            icon = Icons.Filled.Settings,
                            title = stringResource(id = R.string.api_configuration),
                            onClick = onNavigateToApiConfig
                        )
                        GroupDivider()
                        SettingsNavItem(
                            icon = Icons.Filled.Extension,
                            title = "提供商协议插件",
                            onClick = onNavigateToPlugins
                        )
                        GroupDivider()
                        SettingsNavItem(
                            icon = Icons.Filled.AutoFixHigh,
                            title = "Prompt 模板",
                            onClick = onNavigateToTemplates
                        )
                    }
                }

                // ═══════════════════════════════════════
                // 第二组：对话体验
                // ═══════════════════════════════════════
                item {
                    SettingsGroup(title = "对话体验") {
                        SettingsToggleItem(
                            icon = Icons.Filled.Image,
                            title = "图片自动压缩",
                            subtitle = if (uiState.isImageCompressionEnabled)
                                "已开启 · 自动压缩至安全分辨率"
                            else
                                "已关闭 · 限制总大小 20MB",
                            checked = uiState.isImageCompressionEnabled,
                            onCheckedChange = { viewModel.setImageCompressionEnabled(it) }
                        )
                        GroupDivider()
                        SettingsToggleItem(
                            icon = Icons.Filled.SwapHoriz,
                            title = "模型自动降级",
                            subtitle = if (uiState.isFallbackEnabled)
                                "已开启 · 主模型失败自动切换备选"
                            else
                                "已关闭 · 仅使用当前模型",
                            checked = uiState.isFallbackEnabled,
                            onCheckedChange = { viewModel.setFallbackEnabled(it) }
                        )
                    }
                }

                // ═══════════════════════════════════════
                // 第三组：灵魂引擎
                // ═══════════════════════════════════════
                item {
                    SettingsGroup(title = "灵魂引擎") {
                        // AI 人设系统主开关
                        SettingsToggleItem(
                            icon = Icons.Filled.Person,
                            title = "AI 人设系统",
                            subtitle = if (uiState.isIdentityEnabled)
                                "已开启 · 可切换不同AI人格"
                            else
                                "已关闭 · 使用默认行为",
                            checked = uiState.isIdentityEnabled,
                            onCheckedChange = { viewModel.setIdentityEnabled(it) }
                        )
                        // 从属项：管理 AI 人设（仅在人设系统开启时可用）
                        AnimatedVisibility(
                            visible = uiState.isIdentityEnabled,
                            enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
                        ) {
                            Column {
                                GroupDivider(indented = true)
                                SettingsNavItem(
                                    icon = Icons.Filled.ManageAccounts,
                                    title = "管理 AI 人设",
                                    onClick = onNavigateToIdentity,
                                    indented = true
                                )
                            }
                        }
                        GroupDivider()
                        // 长期记忆
                        SettingsToggleItem(
                            icon = Icons.Filled.Psychology,
                            title = "长期记忆",
                            subtitle = if (uiState.isMemoryEnabled)
                                "已开启 · AI会记住你的偏好"
                            else
                                "已关闭 · 每次对话独立",
                            checked = uiState.isMemoryEnabled,
                            onCheckedChange = { viewModel.setMemoryEnabled(it) }
                        )
                        GroupDivider()
                        // 情绪感知
                        SettingsToggleItem(
                            icon = Icons.Filled.Favorite,
                            title = "情绪感知",
                            subtitle = if (uiState.isEmotionEnabled)
                                "已开启 · AI会表达情绪变化"
                            else
                                "已关闭 · 纯理性模式",
                            checked = uiState.isEmotionEnabled,
                            onCheckedChange = { viewModel.setEmotionEnabled(it) }
                        )
                    }
                }

                // ═══════════════════════════════════════
                // 第四组：数据管理
                // ═══════════════════════════════════════
                item {
                    SettingsGroup(title = "数据管理") {
                        SettingsNavItem(
                            icon = Icons.Filled.BarChart,
                            title = "用量统计",
                            onClick = onNavigateToUsage
                        )
                        GroupDivider()
                        SettingsNavItem(
                            icon = Icons.Filled.SyncAlt,
                            title = "局域网同步",
                            onClick = onNavigateToSync
                        )
                        GroupDivider()
                        SettingsNavItem(
                            icon = Icons.Filled.Upload,
                            title = stringResource(id = R.string.export_api_configs),
                            onClick = { viewModel.prepareExportApiConfigs() }
                        )
                        GroupDivider()
                        SettingsNavItem(
                            icon = Icons.Filled.Download,
                            title = stringResource(id = R.string.import_api_configs),
                            onClick = onImportApiClick
                        )
                        GroupDivider()
                        SettingsNavItem(
                            icon = Icons.Filled.Backup,
                            title = stringResource(id = R.string.export_chat_history),
                            onClick = onExportChatClick
                        )
                        GroupDivider()
                        SettingsNavItem(
                            icon = Icons.Filled.Restore,
                            title = stringResource(id = R.string.import_chat_history),
                            onClick = onImportChatClick
                        )
                    }
                }

                // ═══════════════════════════════════════
                // 第五组：通用
                // ═══════════════════════════════════════
                item {
                    SettingsGroup(title = "通用") {
                        SettingsNavItem(
                            icon = Icons.Filled.Language,
                            title = stringResource(id = R.string.language),
                            onClick = onLanguageClick
                        )
                        GroupDivider()
                        SettingsNavItem(
                            icon = Icons.Filled.Info,
                            title = stringResource(id = R.string.about),
                            onClick = onNavigateToAbout
                        )
                    }
                }

                // 底部留白
                item {
                    Spacer(modifier = Modifier.height(24.dp))
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

// ════════════════════════════════════════════════════════════════
// 分组容器：圆角卡片 + 标题
// ════════════════════════════════════════════════════════════════

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        // 分组标题
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        // 卡片容器
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 导航型设置项（点击跳转，带右箭头）
// ════════════════════════════════════════════════════════════════

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    indented: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = if (indented) 32.dp else 16.dp,
                end = 12.dp,
                top = 14.dp,
                bottom = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (indented)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════════
// 开关型设置项（带副标题和 Switch）
// ════════════════════════════════════════════════════════════════

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
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
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// ════════════════════════════════════════════════════════════════
// 组内分割线
// ════════════════════════════════════════════════════════════════

@Composable
private fun GroupDivider(indented: Boolean = false) {
    HorizontalDivider(
        modifier = Modifier.padding(start = if (indented) 62.dp else 52.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

// ════════════════════════════════════════════════════════════════
// 旧版公开组件保留（外部文件可能引用）
// ════════════════════════════════════════════════════════════════

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
