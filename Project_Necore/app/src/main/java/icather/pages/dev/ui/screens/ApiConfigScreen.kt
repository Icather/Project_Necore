package icather.pages.dev.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import icather.pages.dev.api.plugin.ProtocolRegistry
import icather.pages.dev.db.ApiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigScreen(
    viewModel: ApiConfigViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlugins: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Configurations") },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.setShowAddDialog(true) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add API")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.configs) { config ->
                ApiConfigItem(
                    config = config,
                    isActive = config.id == uiState.activeConfigId,
                    onClick = { viewModel.setActiveConfig(config.id) },
                    onEdit = { viewModel.setConfigToEdit(config) },
                    onDelete = { viewModel.deleteConfig(config) }
                )
            }
        }

        if (uiState.showAddDialog) {
            AddApiDialog(
                configToEdit = uiState.configToEdit,
                onDismiss = { viewModel.setShowAddDialog(false) },
                onSave = { provider, model, name, key ->
                    if (uiState.configToEdit != null) {
                        viewModel.updateConfig(uiState.configToEdit!!.copy(
                            provider = provider,
                            modelName = model,
                            name = name,
                            apiKey = key
                        ))
                    } else {
                        viewModel.addConfig(provider, model, name, key)
                    }
                },
                onNavigateToPlugins = onNavigateToPlugins
            )
        }
    }
}

@Composable
fun ApiConfigItem(
    config: ApiConfig,
    isActive: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${config.provider} | ${config.modelName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            if (isActive) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddApiDialog(
    configToEdit: ApiConfig?,
    onDismiss: () -> Unit,
    onSave: (provider: String, model: String, name: String, key: String) -> Unit,
    onNavigateToPlugins: () -> Unit
) {
    // I1: 提供商分组 — 按 base_url 聚合，一个提供商只出现一次
    val providerGroups = remember { ProtocolRegistry.getProviderGroups() }

    // 编辑模式时，从现有 provider(pluginId) 反查到对应的提供商组
    val initialGroup = remember(configToEdit) {
        if (configToEdit != null) {
            providerGroups.find { configToEdit.provider in it.pluginIds }
        } else null
    }

    var selectedGroup by remember { mutableStateOf(initialGroup ?: providerGroups.firstOrNull()) }
    var providerExpanded by remember { mutableStateOf(false) }

    var modelName by remember { mutableStateOf(configToEdit?.modelName ?: "") }
    var displayName by remember { mutableStateOf(configToEdit?.name ?: "") }
    var apiKey by remember { mutableStateOf(configToEdit?.apiKey ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(if (configToEdit != null) "Edit API Configuration" else "Add API Configuration", style = MaterialTheme.typography.headlineSmall)

                // ===== 提供商选择下拉（按 base_url 分组，约 8 个选项） =====
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = !providerExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedGroup?.displayName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider (提供商)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false }
                    ) {
                        providerGroups.forEach { group ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(group.displayName, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            "${group.availableModels.size} 个模型可用",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                onClick = {
                                    selectedGroup = group
                                    providerExpanded = false
                                    // 切换提供商时自动选中第一个模型
                                    modelName = group.availableModels.firstOrNull() ?: ""
                                }
                            )
                        }
                    }
                }

                // Plugin Download Hint
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onNavigateToPlugins()
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Extension, contentDescription = "Plugins", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Need another provider? Download plugins here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // ===== 模型选择：预设下拉 + 手动输入 =====
                val availableModels = selectedGroup?.availableModels ?: emptyList()
                var modelExpanded by remember { mutableStateOf(false) }
                var isCustomModel by remember { mutableStateOf(
                    configToEdit != null && configToEdit.modelName !in availableModels
                ) }

                if (!isCustomModel && availableModels.isNotEmpty()) {
                    // 预设模型下拉
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = !modelExpanded }
                    ) {
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Model Name (模型名称)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        modelName = model
                                        modelExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("✏️ 手动输入模型名称") },
                                onClick = {
                                    isCustomModel = true
                                    modelName = ""
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                } else {
                    // 手动输入模式
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Model Name (手动输入模型名称)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (availableModels.isNotEmpty()) {
                        TextButton(onClick = { isCustomModel = false; modelName = availableModels.firstOrNull() ?: "" }) {
                            Text("← 返回预设列表")
                        }
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name (显示名称)") },
                    placeholder = { Text(modelName.ifBlank { "未选择模型" }) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (API密钥)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedGroup != null && modelName.isNotBlank() && apiKey.isNotBlank()) {
                                // I1: 根据提供商组 + 模型名反查插件 id，保证 DB 中 provider 字段有效
                                val pluginId = ProtocolRegistry.findPluginIdForProvider(selectedGroup!!.baseUrl, modelName)
                                    ?: selectedGroup!!.pluginIds.firstOrNull()
                                    ?: ""
                                // 显示名称留空时，默认使用模型名称
                                val finalDisplayName = displayName.ifBlank { modelName }
                                onSave(pluginId, modelName, finalDisplayName, apiKey)
                            }
                        },
                        enabled = selectedGroup != null && modelName.isNotBlank() && apiKey.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
