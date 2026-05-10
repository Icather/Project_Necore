package icather.pages.dev

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.db.PromptTemplate
import icather.pages.dev.ui.screens.PromptTemplateScreen
import icather.pages.dev.ui.theme.Project_NecoreTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * F1: Prompt 模板管理宿主 Activity
 *
 * 用户可浏览、新建、编辑、删除模板。
 * 点击"应用"时将 systemPrompt 通过 Intent.EXTRA_TEXT 回传给 ChatActivity。
 */
class PromptTemplateActivity : AppCompatActivity() {

    companion object {
        const val RESULT_EXTRA_SYSTEM_PROMPT = "extra_system_prompt"
        const val RESULT_EXTRA_TEMPLATE_NAME = "extra_template_name"
    }

    private val db by lazy { AppDatabase.getInstance(this) }
    private val dao by lazy { db.promptTemplateDao() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Project_NecoreTheme {
                var templates by remember { mutableStateOf<List<PromptTemplate>>(emptyList()) }

                // 加载数据
                LaunchedEffect(Unit) {
                    templates = withContext(Dispatchers.IO) { dao.getAllList() }
                }

                PromptTemplateScreen(
                    templates = templates,
                    onNavigateBack = { finish() },
                    onApplyTemplate = { template ->
                        // 将选中的模板回传
                        val resultIntent = Intent().apply {
                            putExtra(RESULT_EXTRA_SYSTEM_PROMPT, template.systemPrompt)
                            putExtra(RESULT_EXTRA_TEMPLATE_NAME, template.name)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        Toast.makeText(this, "已应用「${template.name}」", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onSaveTemplate = { template ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                if (template.id == 0L) {
                                    dao.insert(template)
                                } else {
                                    dao.update(template)
                                }
                            }
                            // 重新加载
                            templates = withContext(Dispatchers.IO) { dao.getAllList() }
                        }
                    },
                    onDeleteTemplate = { template ->
                        scope.launch {
                            withContext(Dispatchers.IO) { dao.delete(template) }
                            templates = withContext(Dispatchers.IO) { dao.getAllList() }
                        }
                    }
                )
            }
        }
    }
}
