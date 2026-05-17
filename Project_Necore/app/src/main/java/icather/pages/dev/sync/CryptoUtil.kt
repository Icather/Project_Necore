package icather.pages.dev.sync

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 局域网同步 — 密码学工具
 *
 * 实现 ECDH 密钥协商 + AES-256-GCM 加密 + SAS 短认证码。
 * 全部使用 Android/Java 标准库 API，零第三方依赖。
 */
object CryptoUtil {

    private const val EC_CURVE = "secp256r1"
    private const val AES_ALGO = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12

    // HKDF 域分离标签，防止密钥用途混淆
    private val AES_KEY_LABEL = "NECORE_SYNC_AES_KEY_V1".toByteArray()
    private val SAS_LABEL = "NECORE_SYNC_SAS_V1".toByteArray()

    // ──────────── 密钥对生成与序列化 ────────────

    /** 生成临时 ECDH 密钥对（P-256 曲线） */
    fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance("EC")
        gen.initialize(ECGenParameterSpec(EC_CURVE))
        return gen.generateKeyPair()
    }

    /** 将公钥编码为 Base64 字符串，用于网络传输 */
    fun encodePublicKey(key: PublicKey): String =
        Base64.encodeToString(key.encoded, Base64.NO_WRAP)

    /** 从 Base64 字符串解码公钥 */
    fun decodePublicKey(encoded: String): PublicKey {
        val spec = X509EncodedKeySpec(Base64.decode(encoded, Base64.NO_WRAP))
        return KeyFactory.getInstance("EC").generatePublic(spec)
    }

    // ──────────── ECDH 密钥协商 ────────────

    /** 执行 ECDH 密钥协商，返回原始共享密钥 */
    fun deriveSharedSecret(myPrivateKey: PrivateKey, peerPublicKey: PublicKey): ByteArray {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(myPrivateKey)
        agreement.doPhase(peerPublicKey, true)
        return agreement.generateSecret()
    }

    // ──────────── 密钥派生（简化 HKDF） ────────────

    /**
     * 从 ECDH 共享密钥派生 AES-256 密钥。
     * 使用 SHA-256(label || sharedSecret) 作为简化 HKDF。
     */
    fun deriveAesKey(sharedSecret: ByteArray): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(AES_KEY_LABEL)
        val keyBytes = digest.digest(sharedSecret)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * 从 ECDH 共享密钥派生 4 位 SAS 确认码。
     * 双方独立计算，结果一致则证明密钥协商未被中间人篡改。
     */
    fun deriveSasCode(sharedSecret: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(SAS_LABEL)
        val hash = digest.digest(sharedSecret)
        // 取前 2 字节作为无符号整数，模 10000 得到 4 位数字
        val value = ((hash[0].toInt() and 0xFF) shl 8) or (hash[1].toInt() and 0xFF)
        return (value % 10000).toString().padStart(4, '0')
    }

    // ──────────── AES-256-GCM 加解密 ────────────

    /** 加密数据，返回 IV + 密文（IV 固定 12 字节前缀） */
    fun encrypt(plaintext: ByteArray, key: SecretKey): ByteArray {
        val iv = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    /** 解密数据，输入格式为 IV(12) + 密文 */
    fun decrypt(data: ByteArray, key: SecretKey): ByteArray {
        require(data.size > GCM_IV_BYTES) { "密文数据太短" }
        val iv = data.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = data.copyOfRange(GCM_IV_BYTES, data.size)
        val cipher = Cipher.getInstance(AES_ALGO)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    /** 加密字符串的便捷方法，返回 Base64 编码的密文 */
    fun encryptString(plaintext: String, key: SecretKey): String {
        val encrypted = encrypt(plaintext.toByteArray(Charsets.UTF_8), key)
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /** 解密 Base64 编码密文为字符串 */
    fun decryptString(encryptedBase64: String, key: SecretKey): String {
        val data = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        return String(decrypt(data, key), Charsets.UTF_8)
    }
}
