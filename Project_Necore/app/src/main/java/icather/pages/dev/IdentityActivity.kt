package icather.pages.dev

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.db.Identity
import icather.pages.dev.ui.screens.IdentityScreen
import icather.pages.dev.ui.theme.Project_NecoreTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * D3: 人设管理页面宿主 Activity
 *
 * 直接使用 Room DAO 操作数据库，无需额外 ViewModel。
 * 人设管理是低频操作，简单架构即可。
 */
class IdentityActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.getInstance(this) }
    private val dao by lazy { db.identityDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Project_NecoreTheme {
                var identities by remember { mutableStateOf<List<Identity>>(emptyList()) }

                // 初始加载
                LaunchedEffect(Unit) {
                    identities = withContext(Dispatchers.IO) { dao.getAllList() }
                }

                IdentityScreen(
                    identities = identities,
                    onNavigateBack = { finish() },
                    onActivate = { identity ->
                        CoroutineScope(Dispatchers.IO).launch {
                            // 先停用所有，再激活选中的
                            dao.deactivateAll()
                            dao.update(identity.copy(isActive = true))
                            val refreshed = dao.getAllList()
                            withContext(Dispatchers.Main) { identities = refreshed }
                        }
                    },
                    onSave = { identity ->
                        CoroutineScope(Dispatchers.IO).launch {
                            if (identity.id == 0L) {
                                dao.insert(identity)
                            } else {
                                dao.update(identity)
                            }
                            val refreshed = dao.getAllList()
                            withContext(Dispatchers.Main) { identities = refreshed }
                        }
                    },
                    onDelete = { identity ->
                        CoroutineScope(Dispatchers.IO).launch {
                            dao.delete(identity)
                            val refreshed = dao.getAllList()
                            withContext(Dispatchers.Main) { identities = refreshed }
                        }
                    }
                )
            }
        }
    }
}
