package icather.pages.dev

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_USER) R.layout.list_item_chat_user else R.layout.list_item_chat_ai
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)

        fun bind(chatMessage: ChatMessage) {
            if (chatMessage.isHtml) {
                messageTextView.text = Html.fromHtml(chatMessage.text, Html.FROM_HTML_MODE_COMPACT)
            } else {
                messageTextView.text = chatMessage.text
            }
        }
    }
}
