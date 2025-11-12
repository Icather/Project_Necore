package icather.pages.dev

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import icather.pages.dev.db.ApiConfig

class ApiConfigAdapter(
    private var apiConfigs: List<ApiConfig>,
    private var activeConfigId: Long,
    private val onItemClicked: (ApiConfig) -> Unit,
    private val onItemLongClicked: (ApiConfig, View) -> Unit
) : RecyclerView.Adapter<ApiConfigAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_api_config, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val apiConfig = apiConfigs[position]
        holder.bind(apiConfig, apiConfig.id == activeConfigId)
        holder.itemView.setOnClickListener { onItemClicked(apiConfig) }
        holder.itemView.setOnLongClickListener {
            onItemLongClicked(apiConfig, it)
            true
        }
    }

    override fun getItemCount() = apiConfigs.size

    fun updateData(newApiConfigs: List<ApiConfig>, newActiveConfigId: Long) {
        apiConfigs = newApiConfigs
        activeConfigId = newActiveConfigId
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val apiNameTextView: TextView = view.findViewById(R.id.apiNameTextView)
        private val apiProviderTextView: TextView = view.findViewById(R.id.apiProviderTextView)
        private val modelTypeTextView: TextView = view.findViewById(R.id.modelTypeTextView)
        private val apiKeyTextView: TextView = view.findViewById(R.id.apiKeyTextView)
        private val checkIcon: ImageView = view.findViewById(R.id.checkIcon)

        fun bind(apiConfig: ApiConfig, isActive: Boolean) {
            val context = itemView.context
            apiNameTextView.text = apiConfig.name
            apiProviderTextView.text = "${context.getString(R.string.provider)}: ${apiConfig.provider}"
            modelTypeTextView.text = "${context.getString(R.string.type)}: ${apiConfig.modelType}"
            apiKeyTextView.text = when {
                apiConfig.apiKey.isNotEmpty() -> "${context.getString(R.string.api_key_display)}: ...${apiConfig.apiKey.takeLast(4)}"
                else -> "${context.getString(R.string.api_key_display)}: ${context.getString(R.string.not_set)}"
            }
            checkIcon.visibility = if (isActive) View.VISIBLE else View.GONE
        }
    }
}
