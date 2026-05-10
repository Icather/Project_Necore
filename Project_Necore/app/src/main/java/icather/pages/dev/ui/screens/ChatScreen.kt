package icather.pages.dev.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import icather.pages.dev.ChatMessage
import icather.pages.dev.chat.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onModelSelectorClick: () -> Unit,
    onImageUploadClick: () -> Unit,
    onFileUploadClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    // Scroll to bottom when messages change
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.title.ifEmpty { "Project Necore" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground) 
                        val activeConfig = uiState.activeApiConfig
                        if (activeConfig != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(activeConfig.modelName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.AddCircleOutline, contentDescription = "New Chat/Settings") // Using add icon as per screenshot top right
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                itemsIndexed(uiState.messages) { index, message ->
                    val isLastAiMessage = !message.isUser && index == uiState.messages.lastIndex
                    ChatMessageItem(
                        message = message,
                        protocol = uiState.activeProtocol,
                        index = index,
                        isLastAiMessage = isLastAiMessage,
                        viewModel = viewModel
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // DeepSeek Style Floating Input Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .windowInsetsPadding(WindowInsets.ime),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                ) {
                    // Attachments row
                    if (uiState.attachedImages.isNotEmpty() || uiState.attachedFiles.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.attachedImages.forEach { uri ->
                                Chip(text = "Image", onRemove = { viewModel.removeAttachment(uri, true) })
                            }
                            uiState.attachedFiles.forEach { uri ->
                                Chip(text = "File", onRemove = { viewModel.removeAttachment(uri, false) })
                            }
                        }
                    }

                    // Input Field (Top part of card)
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp, max = 150.dp)
                            .padding(vertical = 8.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text("发消息或按住说话", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyLarge)
                            }
                            innerTextField()
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Row: Pills (Left) and Actions (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Mode Pills
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (uiState.activeProtocol?.featureReasoning?.supported == true) {
                                val isThinking = uiState.isThinkingModeEnabled
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isThinking) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isThinking) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.height(32.dp).clickable { viewModel.toggleThinkingMode(!isThinking) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                                        Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("深度思考", style = MaterialTheme.typography.labelMedium, color = if (isThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.height(32.dp).clickable { onModelSelectorClick() }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(uiState.activeApiConfig?.provider ?: "模型", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Right: Actions (+, Voice/Send)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface),
                                color = Color.Transparent,
                                modifier = Modifier.size(28.dp).clickable { onImageUploadClick() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            if (inputText.isNotBlank() || uiState.attachedImages.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp).clickable {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Send", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface),
                                    color = Color.Transparent,
                                    modifier = Modifier.size(28.dp).clickable { /* Voice Input Placeholder */ }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Mic, contentDescription = "Voice", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    protocol: icather.pages.dev.api.plugin.ProtocolPluginJson?,
    index: Int = 0,
    isLastAiMessage: Boolean = false,
    viewModel: ChatViewModel? = null
) {
    val isUser = message.isUser
    val context = LocalContext.current
    
    // E3: 编辑对话框状态
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    
    if (isUser) {
        // === 用户消息 ===
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 280.dp),
                shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                shadowElevation = 0.dp
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = message.text, 
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface, 
                        style = MaterialTheme.typography.bodyLarge
                    )
                    // E3: 编辑按钮
                    if (viewModel != null && !message.isStreaming) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "编辑",
                            modifier = Modifier
                                .size(28.dp)
                                .padding(top = 8.dp, end = 8.dp)
                                .clickable {
                                    editText = message.text
                                    showEditDialog = true
                                },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
        
        // E3: 编辑对话框
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("编辑消息") },
                text = {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 8
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEditDialog = false
                            viewModel?.editAndResend(index, editText.trim())
                        },
                        enabled = editText.isNotBlank()
                    ) { Text("重新发送") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) { Text("取消") }
                }
            )
        }
    } else {
        // === AI 消息 ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = "AI",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // D1: 惰性渲染 — 流式用 Text()，完成后切换 MarkdownText()
                if (message.isStreaming) {
                    Text(
                        text = message.text,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    // E2: Markdown 增强渲染
                    MarkdownText(
                        markdown = message.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 24.sp
                        )
                    )
                }
                
                // Token 用量信息
                if (message.inputTokens != null || message.outputTokens != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val formatTokens: (Int) -> String = { count ->
                        when {
                            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
                            count >= 1_000 -> String.format("%.1fK", count / 1000.0)
                            else -> count.toString()
                        }
                    }
                    
                    val inputStr = message.inputTokens?.let { "上下文 ${formatTokens(it)}" } ?: ""
                    val outputStr = message.outputTokens?.let { "输出 ${formatTokens(it)}" } ?: ""
                    val separator = if (inputStr.isNotEmpty() && outputStr.isNotEmpty()) " · " else ""
                    
                    val billing = protocol?.billingMetadata
                    var costStr = ""
                    if (billing != null && message.inputTokens != null && message.outputTokens != null) {
                        val cacheCount = message.cacheHitTokens ?: 0
                        val inputCount = message.inputTokens - cacheCount
                        val outputCount = message.outputTokens
                        val cost = (inputCount * billing.inputPricePer1m + cacheCount * billing.cacheHitPricePer1m + outputCount * billing.outputPricePer1m) / 1_000_000.0
                        if (cost > 0) costStr = String.format(" · ￥%.4f", cost)
                    }
                    
                    Text(
                        text = "$inputStr$separator$outputStr$costStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                
                // E4 + E1: 操作按钮行（仅非流式 AI 消息显示）
                if (!message.isStreaming && message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 复制按钮
                        MessageActionButton(
                            icon = Icons.Filled.ContentCopy,
                            label = "复制",
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                // 剥离 HTML 标签，只复制纯文本
                                val plainText = message.text
                                    .replace(Regex("<[^>]*>"), "")
                                    .replace("&lt;", "<").replace("&gt;", ">")
                                    .replace("&amp;", "&")
                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Response", plainText))
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            }
                        )
                        // 分享按钮
                        MessageActionButton(
                            icon = Icons.Filled.Share,
                            label = "分享",
                            onClick = {
                                val plainText = message.text
                                    .replace(Regex("<[^>]*>"), "")
                                    .replace("&lt;", "<").replace("&gt;", ">")
                                    .replace("&amp;", "&")
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, plainText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "分享回复"))
                            }
                        )
                        // E1: 重新生成按钮（仅最后一条 AI 消息）
                        if (isLastAiMessage && viewModel != null) {
                            MessageActionButton(
                                icon = Icons.Filled.Refresh,
                                label = "重新生成",
                                onClick = { viewModel.regenerateLastResponse() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** E4: 消息操作小按钮组件 */
@Composable
private fun MessageActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier
            .height(28.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun Chip(text: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove",
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onRemove() }
            )
        }
    }
}
