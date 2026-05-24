package icather.pages.dev.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import icather.pages.dev.R
import icather.pages.dev.db.PromptTemplate

/**
 * F1: Prompt 模板管理界面
 *
 * 支持浏览/新建/编辑/删除 Prompt 模板。
 * 点击模板可应用到当前对话。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptTemplateScreen(
    templates: List<PromptTemplate>,
    onNavigateBack: () -> Unit,
    onApplyTemplate: (PromptTemplate) -> Unit,
    onSaveTemplate: (PromptTemplate) -> Unit,
    onDeleteTemplate: (PromptTemplate) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<PromptTemplate?>(null) }
    var editName by remember { mutableStateOf("") }
    var editIcon by remember { mutableStateOf("✨") }
    var editPrompt by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prompt_templates)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingTemplate = null
                        editName = ""
                        editIcon = "✨"
                        editPrompt = ""
                        showEditDialog = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_template))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.empty_templates),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.empty_templates_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 内置模板分组
                val builtIn = templates.filter { it.isBuiltIn }
                val custom = templates.filter { !it.isBuiltIn }

                if (builtIn.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.built_in_templates),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(builtIn) { template ->
                        TemplateCard(
                            template = template,
                            onApply = { onApplyTemplate(template) },
                            onEdit = null, // 内置不可编辑
                            onDelete = null // 内置不可删除
                        )
                    }
                }

                if (custom.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.custom_templates),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(custom) { template ->
                        TemplateCard(
                            template = template,
                            onApply = { onApplyTemplate(template) },
                            onEdit = {
                                editingTemplate = template
                                editName = template.name
                                editIcon = template.icon
                                editPrompt = template.systemPrompt
                                showEditDialog = true
                            },
                            onDelete = { onDeleteTemplate(template) }
                        )
                    }
                }
            }
        }

        // 编辑/新建对话框
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text(if (editingTemplate == null) stringResource(R.string.new_template) else stringResource(R.string.edit_template)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editIcon,
                                onValueChange = { if (it.length <= 2) editIcon = it },
                                label = { Text(stringResource(R.string.label_icon)) },
                                modifier = Modifier.width(72.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text(stringResource(R.string.label_name)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = editPrompt,
                            onValueChange = { editPrompt = it },
                            label = { Text("System Prompt") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            minLines = 4,
                            maxLines = 10
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val template = PromptTemplate(
                                id = editingTemplate?.id ?: 0,
                                name = editName.trim(),
                                icon = editIcon.ifBlank { "✨" },
                                systemPrompt = editPrompt.trim(),
                                isBuiltIn = false
                            )
                            onSaveTemplate(template)
                            showEditDialog = false
                        },
                        enabled = editName.isNotBlank() && editPrompt.isNotBlank()
                    ) { Text(stringResource(R.string.save)) }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

/** 模板卡片 */
@Composable
private fun TemplateCard(
    template: PromptTemplate,
    onApply: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onApply() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = template.icon,
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = template.systemPrompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // 操作按钮
            if (onEdit != null || onDelete != null) {
                Column {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.label_edit), modifier = Modifier.size(16.dp))
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
