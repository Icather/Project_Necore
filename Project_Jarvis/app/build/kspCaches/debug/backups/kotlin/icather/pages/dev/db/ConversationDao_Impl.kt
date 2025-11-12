package icather.pages.dev.db

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ConversationDao_Impl(
  __db: RoomDatabase,
) : ConversationDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfConversation: EntityInsertAdapter<Conversation>

  private val __updateAdapterOfConversation: EntityDeleteOrUpdateAdapter<Conversation>
  init {
    this.__db = __db
    this.__insertAdapterOfConversation = object : EntityInsertAdapter<Conversation>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `conversations` (`id`,`startTime`,`title`) VALUES (nullif(?, 0),?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Conversation) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.startTime)
        statement.bindText(3, entity.title)
      }
    }
    this.__updateAdapterOfConversation = object : EntityDeleteOrUpdateAdapter<Conversation>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `conversations` SET `id` = ?,`startTime` = ?,`title` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Conversation) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.startTime)
        statement.bindText(3, entity.title)
        statement.bindLong(4, entity.id)
      }
    }
  }

  public override suspend fun insert(conversation: Conversation): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfConversation.insertAndReturnId(_connection, conversation)
    _result
  }

  public override suspend fun update(conversation: Conversation): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfConversation.handle(_connection, conversation)
  }

  public override suspend fun getAllConversations(): List<Conversation> {
    val _sql: String = "SELECT * FROM conversations ORDER BY startTime DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartTime: Int = getColumnIndexOrThrow(_stmt, "startTime")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _result: MutableList<Conversation> = mutableListOf()
        while (_stmt.step()) {
          val _item: Conversation
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartTime: Long
          _tmpStartTime = _stmt.getLong(_columnIndexOfStartTime)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          _item = Conversation(_tmpId,_tmpStartTime,_tmpTitle)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getConversation(conversationId: Long): Conversation? {
    val _sql: String = "SELECT * FROM conversations WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, conversationId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartTime: Int = getColumnIndexOrThrow(_stmt, "startTime")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _result: Conversation?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartTime: Long
          _tmpStartTime = _stmt.getLong(_columnIndexOfStartTime)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          _result = Conversation(_tmpId,_tmpStartTime,_tmpTitle)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAll() {
    val _sql: String = "DELETE FROM conversations"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(conversationId: Long) {
    val _sql: String = "DELETE FROM conversations WHERE id = ?"
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
