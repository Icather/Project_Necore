package icather.pages.dev

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import icather.pages.dev.api.plugin.PluginManagerViewModel
import icather.pages.dev.ui.screens.PluginManagerScreen
import icather.pages.dev.ui.theme.Project_NecoreTheme

/**
 * 协议插件管理页面宿主 Activity
 *
 * 从 GitHub 仓库浏览、下载和管理协议插件 JSON 文件。
 * 下载的插件存储在 app 私有目录，由 ProtocolRegistry 在 reload 时加载。
 */
class PluginManagerActivity : AppCompatActivity() {

    private val viewModel: PluginManagerViewModel by viewModels {
        PluginManagerViewModel.Factory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Project_NecoreTheme {
                PluginManagerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
