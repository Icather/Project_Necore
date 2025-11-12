package icather.pages.dev

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import icather.pages.dev.db.Conversation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val conversations: List<Conversation>,
    private val onItemClicked: (Long) -> Unit,
    private val onItemLongClicked: (Conversation, View) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.bind(conversation)
        holder.itemView.setOnClickListener { onItemClicked(conversation.id) }
        holder.itemView.setOnLongClickListener {
            onItemLongClicked(conversation, it)
            true
        }
    }

    override fun getItemCount() = conversations.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.historyTitle)
        private val timestamp: TextView = itemView.findViewById(R.id.historyTimestamp)

        fun bind(conversation: Conversation) {
            title.text = conversation.title
            timestamp.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(conversation.startTime))
        }
    }
}