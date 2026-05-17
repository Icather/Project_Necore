package icather.pages.dev.sync

import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

/**
 * 局域网同步 — 接收方 HTTP 客户端
 *
 * 复用项目已有的 OkHttp 库，与 SyncServer 通信。
 */
class SyncClient {

    companion object {
        private const val TAG = "SyncClient"
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)  // 大数据量可能较慢
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var aesKey: SecretKey? = null

    /**
     * 第一步：执行 ECDH 握手
     * @return Pair(己方 SAS 验证码, 会话 ID)
     */
    fun performHandshake(
        host: String,
        port: Int,
        deviceName: String
    ): HandshakeResult? {
        try {
            // 生成客户端密钥对
            val clientKeyPair = CryptoUtil.generateKeyPair()
            val publicKeyBase64 = CryptoUtil.encodePublicKey(clientKeyPair.public)

            // 发送握手请求
            val requestJson = gson.toJson(
                mapOf("publicKey" to publicKeyBase64, "deviceName" to deviceName)
            )
            val request = Request.Builder()
                .url("http://$host:$port/sync/handshake")
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "握手失败: ${response.code}")
                return null
            }

            val body = response.body?.string() ?: return null
            val handshakeResp = gson.fromJson(body, HandshakeResponse::class.java)

            // ECDH 协商
            val serverPublicKey = CryptoUtil.decodePublicKey(handshakeResp.publicKey)
            val sharedSecret = CryptoUtil.deriveSharedSecret(
                clientKeyPair.private, serverPublicKey
            )
            this.aesKey = CryptoUtil.deriveAesKey(sharedSecret)
            val sasCode = CryptoUtil.deriveSasCode(sharedSecret)

            return HandshakeResult(
                sasCode = sasCode,
                sessionId = handshakeResp.sessionId,
                serverDeviceName = "" // 从 manifest 中获取
            )
        } catch (e: Exception) {
            Log.e(TAG, "握手异常", e)
            return null
        }
    }

    /**
     * 第二步：通知服务端接收方已确认（并轮询发送方确认状态）
     * @return "accepted" | "rejected" | "timeout" | null(异常)
     */
    fun pollConfirmation(host: String, port: Int, timeoutMs: Long = 60_000): String? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val request = Request.Builder()
                    .url("http://$host:$port/sync/status")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: continue
                val status = gson.fromJson(body, StatusResponse::class.java).status

                if (status != "pending") return status
                Thread.sleep(1000)  // 1 秒轮询间隔
            } catch (e: Exception) {
                Log.e(TAG, "轮询异常", e)
                return null
            }
        }
        return "timeout"
    }

    /**
     * 第三步：获取数据摘要
     */
    fun fetchManifest(host: String, port: Int): SyncManifest? {
        val key = aesKey ?: return null
        try {
            val request = Request.Builder()
                .url("http://$host:$port/sync/manifest")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val encrypted = response.body?.string() ?: return null
            val json = CryptoUtil.decryptString(encrypted, key)
            return gson.fromJson(json, SyncManifest::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "获取 manifest 失败", e)
            return null
        }
    }

    /**
     * 第四步：获取同步数据（解密后的 JSON 字符串）
     * @param type "chat" 或 "api"
     */
    fun fetchData(host: String, port: Int, type: String): String? {
        val key = aesKey ?: return null
        try {
            val request = Request.Builder()
                .url("http://$host:$port/sync/data?type=$type")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val encrypted = response.body?.string() ?: return null
            return CryptoUtil.decryptString(encrypted, key)
        } catch (e: Exception) {
            Log.e(TAG, "获取 $type 数据失败", e)
            return null
        }
    }

    // ———— 内部数据类 ————

    data class HandshakeResult(
        val sasCode: String,
        val sessionId: String,
        val serverDeviceName: String
    )

    private data class HandshakeResponse(
        val publicKey: String,
        val sessionId: String
    )

    private data class StatusResponse(val status: String)
}
