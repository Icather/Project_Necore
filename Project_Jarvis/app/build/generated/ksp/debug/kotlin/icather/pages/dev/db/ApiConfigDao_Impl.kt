package icather.pages.dev.db

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
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
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ApiConfigDao_Impl(
  __db: RoomDatabase,
) : ApiConfigDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfApiConfig: EntityInsertAdapter<ApiConfig>
  init {
    this.__db = __db
    this.__insertAdapterOfApiConfig = object : EntityInsertAdapter<ApiConfig>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `api_configs` (`id`,`provider`,`name`,`apiKey`,`modelType`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ApiConfig) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.provider)
        statement.bindText(3, entity.name)
        statement.bindText(4, entity.apiKey)
        statement.bindText(5, entity.modelType)
      }
    }
  }

  public override suspend fun insert(apiConfig: ApiConfig): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfApiConfig.insert(_connection, apiConfig)
  }

  public override suspend fun insertAll(apiConfigs: List<ApiConfig>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfApiConfig.insert(_connection, apiConfigs)
  }

  public override fun getAll(): Flow<List<ApiConfig>> {
    val _sql: String = "SELECT * FROM api_configs"
    return createFlow(__db, false, arrayOf("api_configs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfProvider: Int = getColumnIndexOrThrow(_stmt, "provider")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfApiKey: Int = getColumnIndexOrThrow(_stmt, "apiKey")
        val _columnIndexOfModelType: Int = getColumnIndexOrThrow(_stmt, "modelType")
        val _result: MutableList<ApiConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: ApiConfig
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpProvider: String
          _tmpProvider = _stmt.getText(_columnIndexOfProvider)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpApiKey: String
          _tmpApiKey = _stmt.getText(_columnIndexOfApiKey)
          val _tmpModelType: String
          _tmpModelType = _stmt.getText(_columnIndexOfModelType)
          _item = ApiConfig(_tmpId,_tmpProvider,_tmpName,_tmpApiKey,_tmpModelType)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllOnce(): List<ApiConfig> {
    val _sql: String = "SELECT * FROM api_configs"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfProvider: Int = getColumnIndexOrThrow(_stmt, "provider")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfApiKey: Int = getColumnIndexOrThrow(_stmt, "apiKey")
        val _columnIndexOfModelType: Int = getColumnIndexOrThrow(_stmt, "modelType")
        val _result: MutableList<ApiConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: ApiConfig
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpProvider: String
          _tmpProvider = _stmt.getText(_columnIndexOfProvider)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpApiKey: String
          _tmpApiKey = _stmt.getText(_columnIndexOfApiKey)
          val _tmpModelType: String
          _tmpModelType = _stmt.getText(_columnIndexOfModelType)
          _item = ApiConfig(_tmpId,_tmpProvider,_tmpName,_tmpApiKey,_tmpModelType)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): ApiConfig? {
    val _sql: String = "SELECT * FROM api_configs WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfProvider: Int = getColumnIndexOrThrow(_stmt, "provider")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfApiKey: Int = getColumnIndexOrThrow(_stmt, "apiKey")
        val _columnIndexOfModelType: Int = getColumnIndexOrThrow(_stmt, "modelType")
        val _result: ApiConfig?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpProvider: String
          _tmpProvider = _stmt.getText(_columnIndexOfProvider)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpApiKey: String
          _tmpApiKey = _stmt.getText(_columnIndexOfApiKey)
          val _tmpModelType: String
          _tmpModelType = _stmt.getText(_columnIndexOfModelType)
          _result = ApiConfig(_tmpId,_tmpProvider,_tmpName,_tmpApiKey,_tmpModelType)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM api_configs"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM api_configs WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
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
