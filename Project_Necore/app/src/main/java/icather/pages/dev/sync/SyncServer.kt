package icather.pages.dev.sync

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import java.security.KeyPair
import javax.crypto.SecretKey

/**
 * 局域网同步 — 发送方 HTTP 服务器
 *
 * 基于 NanoHTTPD 的临时嵌入式服务器，提供以下端点：
 * - POST /sync/handshake — ECDH 密钥交换
 * - GET  /sync/status    — 查询发送方确认状态
 * - GET  /sync/manifest  — 获取数据摘要（加密）
 * - GET  /sync/data?type=chat|api — 获取同步数据（加密）
 */
class SyncServer(
    private val chatJson: String?,
    private val apiJson: String?,
    private val manifest: SyncManifest,
    private val onPairRequest: (peerDeviceName: String, sasCode: String) -> Unit
) : NanoHTTPD(0) {  // 端口 0 = 系统随机分配

    companion object {
        private const val TAG = "SyncServer"
    }

    private val gson = Gson()
    private var keyPair: KeyPair? = null
    private var aesKey: SecretKey? = null
    private var currentSession: SyncSession? = null

    /** 外部（ViewModel）调用：发送方确认/拒绝连接 */
    @Volatile
    var senderConfirmed: Boolean? = null

    /** 数据是否已被接收方拉取过 */
    @Volatile
    var dataTransferred: Boolean = false
        private set

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return try {
            when {
                method == Method.POST && uri == "/sync/handshake" -> handleHandshake(session)
                method == Method.GET && uri == "/sync/status" -> handleStatus()
                method == Method.GET && uri == "/sync/manifest" -> handleManifest()
                method == Method.GET && uri == "/sync/data" -> handleData(session)
                else -> newFixedLengthResponse(
                    Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求处理异常: $uri", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}"
            )
        }
    }

    /**
     * POST /sync/handshake
     * 请求体: {"publicKey": "base64...", "deviceName": "设备名"}
     * 响应: {"publicKey": "base64...", "sessionId": "uuid"}
     */
    private fun handleHandshake(session: IHTTPSession): Response {
        // 读取请求体
        val bodyMap = HashMap<String, String>()
        session.parseBody(bodyMap)
        val body = bodyMap["postData"] ?: return errorResponse("请求体为空")

        val request = try {
            gson.fromJson(body, HandshakeRequest::class.java)
        } catch (e: Exception) {
            return errorResponse("请求格式错误")
        }

        // 生成服务端密钥对
        val serverKeyPair = CryptoUtil.generateKeyPair()
        this.keyPair = serverKeyPair

        // ECDH 协商
        val peerPublicKey = try {
            CryptoUtil.decodePublicKey(request.publicKey)
        } catch (e: Exception) {
            return errorResponse("公钥格式无效")
        }

        val sharedSecret = CryptoUtil.deriveSharedSecret(
            serverKeyPair.private, peerPublicKey
        )
        this.aesKey = CryptoUtil.deriveAesKey(sharedSecret)
        val sasCode = CryptoUtil.deriveSasCode(sharedSecret)

        // 创建会话
        val sessionId = java.util.UUID.randomUUID().toString()
        this.currentSession = SyncSession(
            sessionId = sessionId,
            peerPublicKey = request.publicKey,
            sasCode = sasCode
        )

        // 重置确认状态
        senderConfirmed = null

        // 通知 UI 层显示验证码
        onPairRequest(request.deviceName, sasCode)

        val response = HandshakeResponse(
            publicKey = CryptoUtil.encodePublicKey(serverKeyPair.public),
            sessionId = sessionId
        )
        return newFixedLengthResponse(
            Response.Status.OK, "application/json", gson.toJson(response)
        )
    }

    /**
     * GET /sync/status
     * 接收方轮询此端点，等待发送方确认。
     * 响应: {"status": "pending"|"accepted"|"rejected"}
     */
    private fun handleStatus(): Response {
        val status = when (senderConfirmed) {
            null -> "pending"
            true -> "accepted"
            false -> "rejected"
        }
        return newFixedLengthResponse(
            Response.Status.OK, "application/json",
            """{"status":"$status"}"""
        )
    }

    /**
     * GET /sync/manifest
     * 返回加密的数据摘要。必须在双方确认后调用。
     */
    private fun handleManifest(): Response {
        if (senderConfirmed != true) {
            return errorResponse("未授权", Response.Status.FORBIDDEN)
        }
        val key = aesKey ?: return errorResponse("会话未建立")
        val json = gson.toJson(manifest)
        val encrypted = CryptoUtil.encryptString(json, key)
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, encrypted)
    }

    /**
     * GET /sync/data?type=chat|api
     * 返回加密的同步数据。必须在双方确认后调用。
     */
    private fun handleData(session: IHTTPSession): Response {
        if (senderConfirmed != true) {
            return errorResponse("未授权", Response.Status.FORBIDDEN)
        }
        val key = aesKey ?: return errorResponse("会话未建立")
        val type = session.parms["type"] ?: return errorResponse("缺少 type 参数")

        val json = when (type) {
            "chat" -> chatJson ?: return errorResponse("未提供聊天数据")
            "api" -> apiJson ?: return errorResponse("未提供 API 数据")
            else -> return errorResponse("未知数据类型: $type")
        }

        val encrypted = CryptoUtil.encryptString(json, key)
        dataTransferred = true
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, encrypted)
    }

    private fun errorResponse(
        message: String,
        status: Response.Status = Response.Status.BAD_REQUEST
    ): Response {
        return newFixedLengthResponse(status, "application/json", """{"error":"$message"}""")
    }

    // ———— 内部 JSON 数据类 ————

    private data class HandshakeRequest(
        val publicKey: String,
        val deviceName: String
    )

    private data class HandshakeResponse(
        val publicKey: String,
        val sessionId: String
    )
}
