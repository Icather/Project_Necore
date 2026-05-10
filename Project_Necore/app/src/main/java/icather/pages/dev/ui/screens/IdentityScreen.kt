package icather.pages.dev.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import icather.pages.dev.db.Identity

/**
 * D3: 人设管理页面
 *
 * 功能：
 * - 列表展示所有人设档案（卡片式）
 * - 点击卡片设为当前激活
 * - 右上角 + 按钮创建新人设
 * - 滑动或长按删除
 * - 编辑页面：Name + System Prompt + Greeting
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityScreen(
    identities: List<Identity>,
    onNavigateBack: () -> Unit,
    onActivate: (Identity) -> Unit,
    onSave: (Identity) -> Unit,
    onDelete: (Identity) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editingIdentity by remember { mutableStateOf<Identity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 人设管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingIdentity = null
                        showEditDialog = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "创建新人设")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        if (identities.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无人设配置", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(identities, key = { it.id }) { identity ->
                    IdentityCard(
                        identity = identity,
                        onActivate = { onActivate(identity) },
                        onEdit = {
                            editingIdentity = identity
                            showEditDialog = true
                        },
                        onDelete = { onDelete(identity) }
                    )
                }
            }
        }

        if (showEditDialog) {
            IdentityEditDialog(
                identity = editingIdentity,
                onDismiss = { showEditDialog = false },
                onSave = { saved ->
                    onSave(saved)
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
private fun IdentityCard(
    identity: Identity,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = identity.isActive

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onActivate() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 激活指示器
            Icon(
                imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (isActive) "当前激活" else "未激活",
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // 人设信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = identity.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = identity.systemPrompt.take(80) + if (identity.systemPrompt.length > 80) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 操作按钮
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun IdentityEditDialog(
    identity: Identity?,
    onDismiss: () -> Unit,
    onSave: (Identity) -> Unit
) {
    var name by remember { mutableStateOf(identity?.name ?: "") }
    var systemPrompt by remember { mutableStateOf(identity?.systemPrompt ?: "") }
    var greeting by remember { mutableStateOf(identity?.greeting ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (identity == null) "创建新人设" else "编辑人设") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("人设名称") },
                    placeholder = { Text("例如：猫娘、英语老师") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("系统提示词") },
                    placeholder = { Text("定义 AI 的性格、说话方式和行为规则...") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = greeting,
                    onValueChange = { greeting = it },
                    label = { Text("开场白（可选）") },
                    placeholder = { Text("首次对话时 AI 的问候语...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && systemPrompt.isNotBlank()) {
                        onSave(
                            Identity(
                                id = identity?.id ?: 0,
                                name = name.trim(),
                                systemPrompt = systemPrompt.trim(),
                                greeting = greeting.trim(),
                                isActive = identity?.isActive ?: false
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && systemPrompt.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
