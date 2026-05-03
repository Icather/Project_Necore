package icather.pages.dev.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import icather.pages.dev.ChatMessage
import icather.pages.dev.chat.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenDrawer: () -> Unit,
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
        topBar = {
            TopAppBar(
                title = { Text(uiState.title.ifEmpty { "Project Necore" }) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    TextButton(onClick = onModelSelectorClick) {
                        val activeConfig = uiState.activeApiConfig
                        val text = if (activeConfig != null) "${activeConfig.modelType} | ${activeConfig.name}" else "Select Model"
                        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    .padding(horizontal = 8.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.messages) { message ->
                    ChatMessageItem(message = message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Attachments Preview Area
            if (uiState.attachedImages.isNotEmpty() || uiState.attachedFiles.isNotEmpty()) {
                // Simplified attachment preview for Compose
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.attachedImages.forEach { uri ->
                        Chip(text = "Image Attached", onRemove = { viewModel.removeAttachment(uri, true) })
                    }
                    uiState.attachedFiles.forEach { uri ->
                        Chip(text = "File Attached", onRemove = { viewModel.removeAttachment(uri, false) })
                    }
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onImageUploadClick) {
                    Icon(Icons.Filled.Image, contentDescription = "Upload Image")
                }
                IconButton(onClick = onFileUploadClick) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "Upload File")
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    placeholder = { Text("Type a message...") },
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() || uiState.attachedImages.isNotEmpty()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        }
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() || uiState.attachedImages.isNotEmpty()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val backgroundColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            if (isUser) {
                Text(text = message.text, color = contentColor)
            } else {
                // If it contains <font> tag (reasoning), we might need custom parsing, 
                // but for now MarkdownText handles standard markdown beautifully.
                // We'll clean up the reasoning tags slightly for markdown if needed, 
                // or just let MarkdownText render it (it supports some HTML).
                MarkdownText(
                    markdown = message.text,
                    color = contentColor
                )
            }
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
