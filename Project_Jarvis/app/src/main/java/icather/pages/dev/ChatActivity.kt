package icather.pages.dev

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.text.htmlEncode
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import icather.pages.dev.api.ApiService
import icather.pages.dev.api.ApiServiceFactory
import icather.pages.dev.db.ApiConfig
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.db.Conversation
import icather.pages.dev.db.Message
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var toolbar: MaterialToolbar
    private lateinit var modelSelectorTextView: TextView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var attachmentScrollView: HorizontalScrollView
    private lateinit var attachmentsContainer: LinearLayout
    private lateinit var attachmentDivider: View
    private lateinit var imageUploadButton: ImageButton
    private lateinit var fileUploadButton: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private val attachedFiles = mutableListOf<Uri>()

    private lateinit var db: AppDatabase
    private var currentConversationId: Long? = null
    private val messages = mutableListOf<ChatMessage>()

    private var activeApiConfig: ApiConfig? = null
    private lateinit var apiService: ApiService
    private val apiConfigs = mutableListOf<ApiConfig>()

    private val historyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val conversationId = result.data?.getLongExtra("CONVERSATION_ID", -1L) ?: -1L
                if (conversationId != -1L) {
                    loadConversation(conversationId)
                }
            }
        }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        updateAttachments(uris)
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        updateAttachments(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_chat)

        db = AppDatabase.getInstance(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        val params = navigationView.layoutParams
        val displayMetrics = resources.displayMetrics
        params.width = (displayMetrics.widthPixels * 0.5).toInt()
        navigationView.layoutParams = params

        toolbar.setNavigationOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_new_chat -> {
                    startNewChat()
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.action_history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    historyLauncher.launch(intent)
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }

        val backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                backPressedCallback.isEnabled = true
            }

            override fun onDrawerClosed(drawerView: View) {
                backPressedCallback.isEnabled = false
            }
        })

        recyclerView = findViewById(R.id.recyclerView)
        editText = findViewById(R.id.editText)
        sendButton = findViewById(R.id.sendButton)
        modelSelectorTextView = findViewById(R.id.modelSelectorTextView)
        attachmentScrollView = findViewById(R.id.attachment_scroll_view)
        attachmentsContainer = findViewById(R.id.attachments_container)
        attachmentDivider = findViewById(R.id.attachmentDivider)
        imageUploadButton = findViewById(R.id.imageUploadButton)
        fileUploadButton = findViewById(R.id.fileUploadButton)

        setupRecyclerView()
        setupSendButton()
        setupModelSelector()
        setupAttachmentButtons()

        val conversationId = intent.getLongExtra("CONVERSATION_ID", -1)
        if (conversationId != -1L) {
            loadConversation(conversationId)
        } else {
            startNewChat()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val messageText = editText.text.toString().trim()
            if (messageText.isNotEmpty() || attachedFiles.isNotEmpty()) {
                sendMessage(messageText)
                editText.text.clear()
                resetAttachments()
            }
        }
    }

    private fun setupAttachmentButtons() {
        imageUploadButton.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        fileUploadButton.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }
    }

    private fun updateAttachments(uris: List<Uri>) {
        attachedFiles.addAll(uris)
        renderAttachments()
    }

    private fun renderAttachments() {
        attachmentsContainer.removeAllViews()
        if (attachedFiles.isEmpty()) {
            attachmentScrollView.visibility = View.GONE
            attachmentDivider.visibility = View.GONE
        } else {
            attachmentScrollView.visibility = View.VISIBLE
            attachmentDivider.visibility = View.VISIBLE
            attachedFiles.forEach { uri ->
                addAttachmentToView(uri)
            }
        }
    }

    private fun addAttachmentToView(uri: Uri) {
        val inflater = LayoutInflater.from(this)
        val previewView = inflater.inflate(R.layout.item_attachment_preview, attachmentsContainer, false)

        val imageView = previewView.findViewById<ImageView>(R.id.preview_image)
        val fileNameView = previewView.findViewById<TextView>(R.id.file_name_text)
        val removeButton = previewView.findViewById<ImageButton>(R.id.remove_attachment_button)

        if (contentResolver.getType(uri)?.startsWith("image/") == true) {
            imageView.setImageURI(uri)
            imageView.visibility = View.VISIBLE
            fileNameView.visibility = View.GONE
        } else {
            fileNameView.text = getFileName(uri)
            imageView.visibility = View.GONE
            fileNameView.visibility = View.VISIBLE
        }

        removeButton.setOnClickListener {
            attachedFiles.remove(uri)
            renderAttachments()
        }

        attachmentsContainer.addView(previewView)
    }

    private fun getFileName(uri: Uri): String {
        var name = ""
        contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun resetAttachments() {
        attachedFiles.clear()
        renderAttachments()
    }

    private fun setupModelSelector() {
        modelSelectorTextView.setOnClickListener { view ->
            val wrapper = ContextThemeWrapper(this, R.style.Widget_App_PopupMenu)
            val popupMenu = PopupMenu(wrapper, view)
            apiConfigs.forEach { model ->
                popupMenu.menu.add(Menu.NONE, model.id.toInt(), Menu.NONE, model.name)
            }
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedModel = apiConfigs.find { it.id == menuItem.itemId.toLong() }
                if (selectedModel != null) {
                    onModelSelected(selectedModel)
                }
                true
            }
            popupMenu.show()
        }

        lifecycleScope.launch {
            val prefs = getSharedPreferences("api_prefs", MODE_PRIVATE)
            var activeId = prefs.getLong("active_api_id", ApiConfigActivity.DEFAULT_API_ID)

            db.apiConfigDao().getAll().collect { configs ->
                if (configs.isEmpty()) {
                    return@collect
                }

                apiConfigs.clear()
                apiConfigs.addAll(configs)

                var currentActive = configs.find { it.id == activeId }
                if (currentActive == null) {
                    currentActive = configs.first()
                    activeId = currentActive.id
                    prefs.edit {
                        putLong("active_api_id", activeId)
                    }
                }
                onModelSelected(currentActive)
            }
        }
    }

    private fun onModelSelected(selectedModel: ApiConfig) {
        activeApiConfig = selectedModel
        val modelTypeText = selectedModel.modelType
        val modelNameText = selectedModel.name
        val fullText = "$modelTypeText | $modelNameText"
        val spannable = SpannableString(fullText)
        val separatorColor = ContextCompat.getColor(this, R.color.grey_500)
        val separatorIndex = fullText.indexOf('|')
        if (separatorIndex != -1) {
            spannable.setSpan(ForegroundColorSpan(separatorColor), separatorIndex, separatorIndex + 1, SpannableString.SPAN_INCLUSIVE_INCLUSIVE)
        }
        modelSelectorTextView.text = spannable

        getSharedPreferences("api_prefs", MODE_PRIVATE).edit {
            putLong("active_api_id", selectedModel.id)
        }

        lifecycleScope.launch {
            try {
                apiService = ApiServiceFactory.create(selectedModel.provider)
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this@ChatActivity, e.message ?: "发生未知错误", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendMessage(text: String) {
        if (activeApiConfig?.modelType == "OCR" && attachedFiles.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_attach_image_for_ocr), Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            if (activeApiConfig?.modelType == "OCR") {
                val imageUri = attachedFiles.first()
                val userMessage = ChatMessage("Image: ${getFileName(imageUri)}", true)
                addMessageToView(userMessage)
                val conversationId = ensureConversationExists(getFileName(imageUri))
                withContext(Dispatchers.IO) {
                    db.messageDao()
                        .insert(Message(conversationId = conversationId, text = "Image: ${getFileName(imageUri)}", isUser = true))
                }
                getOcrResponse(conversationId, imageUri)
            } else {
                val userMessage = ChatMessage(text, true)
                addMessageToView(userMessage)
                val conversationId = ensureConversationExists(text)
                withContext(Dispatchers.IO) {
                    db.messageDao()
                        .insert(Message(conversationId = conversationId, text = text, isUser = true))
                }
                getAIResponse(conversationId)
            }
        }
    }

    private suspend fun ensureConversationExists(firstMessage: String): Long {
        if (currentConversationId == null) {
            val newConversation = Conversation(title = firstMessage.take(30))
            val newId = withContext(Dispatchers.IO) { db.conversationDao().insert(newConversation) }
            currentConversationId = newId
            withContext(Dispatchers.Main) { toolbar.title = newConversation.title }
        }
        return currentConversationId!!
    }

    private fun getOcrResponse(conversationId: Long, imageUri: Uri) {
        val apiKey = activeApiConfig?.apiKey
        if (apiKey.isNullOrEmpty()) {
            addMessageToView(ChatMessage(getString(R.string.api_key_not_set), false))
            return
        }

        lifecycleScope.launch {
            try {
                val ocrText = apiService.performOcr(imageUri, apiKey)
                addMessageToView(ChatMessage(ocrText, false))
                db.messageDao().insert(Message(conversationId = conversationId, text = ocrText, isUser = false))
            } catch (e: Exception) {
                val errorText = if (e is IOException) getString(R.string.network_error, e.message) else getString(R.string.error, e.message)
                addMessageToView(ChatMessage(errorText, false))
            }
        }
    }

    private fun getAIResponse(conversationId: Long) {
        val apiKey = activeApiConfig?.apiKey
        if (apiKey.isNullOrEmpty()) {
            addMessageToView(ChatMessage(getString(R.string.api_key_not_set), false))
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val dbMessages = db.messageDao().getMessagesForConversation(conversationId)
            val apiMessages = dbMessages.map { 
                val role = if (it.isUser) "user" else "assistant"
                val content = it.text.replace(Regex("<font.*?</font>"), "")
                ApiService.ApiMessage(role, content)
            }

            val aiMessageIndex = withContext(Dispatchers.Main) {
                val newMessage = ChatMessage("", false, isHtml = true)
                messages.add(newMessage)
                val index = messages.size - 1
                chatAdapter.notifyItemInserted(index)
                recyclerView.scrollToPosition(index)
                index
            }

            val finalContent = StringBuilder()
            val finalReasoning = StringBuilder()

            apiService.getCompletion(apiMessages, apiKey)
                .catch { e ->
                    withContext(Dispatchers.Main) {
                        val errorText = if (e is IOException) getString(R.string.network_error, e.message) else getString(R.string.error, e.message)
                        addMessageToView(ChatMessage(errorText, false))
                    }
                }
                .collect { chunk ->
                    chunk.content?.let { finalContent.append(it) }
                    chunk.reasoning?.let { finalReasoning.append(it) }

                    val reasoningText = if (finalReasoning.isNotEmpty()) "<font color='#999999'>${finalReasoning.toString().htmlEncode()}</font><br>" else ""
                    val messageText = finalContent.toString().htmlEncode()
                    val displayText = reasoningText + messageText

                    withContext(Dispatchers.Main) {
                        val isAtBottom = !recyclerView.canScrollVertically(1)
                        messages[aiMessageIndex] = messages[aiMessageIndex].copy(text = displayText)
                        chatAdapter.notifyItemChanged(aiMessageIndex)
                        if (isAtBottom) {
                            recyclerView.scrollToPosition(aiMessageIndex)
                        }
                    }
                }

            val dbMessageText = if (finalReasoning.isNotEmpty()) {
                "<font color='#999999'>${finalReasoning.toString().htmlEncode()}</font><br>${finalContent.toString().htmlEncode()}"
            } else {
                finalContent.toString()
            }

            db.messageDao().insert(
                Message(
                    conversationId = conversationId,
                    text = dbMessageText,
                    isUser = false,
                    isHtml = true
                )
            )
        }
    }

    private fun addMessageToView(message: ChatMessage) {
        messages.add(message)
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun startNewChat() {
        currentConversationId = null
        val oldSize = messages.size
        messages.clear()
        chatAdapter.notifyItemRangeRemoved(0, oldSize)
        toolbar.title = ""
        resetAttachments()
    }

    private fun loadConversation(conversationId: Long) {
        this.currentConversationId = conversationId
        lifecycleScope.launch {
            val conversation =
                withContext(Dispatchers.IO) { db.conversationDao().getConversation(conversationId) }
            toolbar.title = conversation?.title ?: "Chat"

            val dbMessages =
                withContext(Dispatchers.IO) {
                    db.messageDao().getMessagesForConversation(conversationId)
                }
            val oldSize = messages.size
            messages.clear()
            chatAdapter.notifyItemRangeRemoved(0, oldSize)

            val newItems = dbMessages.map { message ->
                ChatMessage(message.text, message.isUser, message.isHtml)
            }
            messages.addAll(newItems)
            chatAdapter.notifyItemRangeInserted(0, newItems.size)

            recyclerView.scrollToPosition(messages.size - 1)
            resetAttachments()
        }
    }
}

data class ChatMessage(val text: String, val isUser: Boolean, val isHtml: Boolean = false)
