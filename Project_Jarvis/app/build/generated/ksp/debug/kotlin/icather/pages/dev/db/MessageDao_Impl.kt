package icather.pages.dev.db

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MessageDao_Impl(
  __db: RoomDatabase,
) : MessageDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMessage: EntityInsertAdapter<Message>
  init {
    this.__db = __db
    this.__insertAdapterOfMessage = object : EntityInsertAdapter<Message>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `messages` (`id`,`conversationId`,`text`,`isUser`,`isHtml`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Message) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.conversationId)
        statement.bindText(3, entity.text)
        val _tmp: Int = if (entity.isUser) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        val _tmp_1: Int = if (entity.isHtml) 1 else 0
        statement.bindLong(5, _tmp_1.toLong())
        statement.bindLong(6, entity.timestamp)
      }
    }
  }

  public override suspend fun insert(message: Message): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfMessage.insertAndReturnId(_connection, message)
    _result
  }

  public override suspend fun getMessagesForConversation(conversationId: Long): List<Message> {
    val _sql: String = "SELECT * FROM messages WHERE conversationId = ? ORDER BY timestamp ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, conversationId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfConversationId: Int = getColumnIndexOrThrow(_stmt, "conversationId")
        val _columnIndexOfText: Int = getColumnIndexOrThrow(_stmt, "text")
        val _columnIndexOfIsUser: Int = getColumnIndexOrThrow(_stmt, "isUser")
        val _columnIndexOfIsHtml: Int = getColumnIndexOrThrow(_stmt, "isHtml")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<Message> = mutableListOf()
        while (_stmt.step()) {
          val _item: Message
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpConversationId: Long
          _tmpConversationId = _stmt.getLong(_columnIndexOfConversationId)
          val _tmpText: String
          _tmpText = _stmt.getText(_columnIndexOfText)
          val _tmpIsUser: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsUser).toInt()
          _tmpIsUser = _tmp != 0
          val _tmpIsHtml: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsHtml).toInt()
          _tmpIsHtml = _tmp_1 != 0
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = Message(_tmpId,_tmpConversationId,_tmpText,_tmpIsUser,_tmpIsHtml,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAll() {
    val _sql: String = "DELETE FROM messages"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByConversationId(conversationId: Long) {
    val _sql: String = "DELETE FROM messages WHERE conversationId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, conversationId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
