package icather.pages.dev.sync

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * 局域网同步 — 网络工具
 *
 * 提供本机 IP 获取和 UDP 广播发现机制。
 */
object NetworkUtil {

    private const val TAG = "NetworkUtil"
    const val DISCOVERY_PORT = 19527
    private const val BROADCAST_INTERVAL_MS = 2000L
    private const val MAGIC = "NECORE_SYNC"

    /** 获取本机局域网 IPv4 地址（遍历所有网络接口） */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取本机 IP 失败", e)
        }
        return null
    }

    /**
     * 构造 UDP 广播消息
     * 格式: NECORE_SYNC|设备名|HTTP端口|App版本
     */
    fun buildBroadcastMessage(deviceName: String, httpPort: Int, appVersion: String): ByteArray =
        "$MAGIC|$deviceName|$httpPort|$appVersion".toByteArray(Charsets.UTF_8)

    /**
     * 解析 UDP 广播消息，返回 DiscoveredDevice（不含 IP，由调用方从 DatagramPacket 中获取）
     */
    fun parseBroadcastMessage(data: ByteArray, length: Int, senderIp: String): DiscoveredDevice? {
        try {
            val message = String(data, 0, length, Charsets.UTF_8)
            val parts = message.split("|")
            if (parts.size != 4 || parts[0] != MAGIC) return null
            return DiscoveredDevice(
                deviceName = parts[1],
                ipAddress = senderIp,
                httpPort = parts[2].toIntOrNull() ?: return null,
                appVersion = parts[3]
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 启动 UDP 广播发送（在后台线程运行）。
     * 每隔 2 秒向 255.255.255.255:DISCOVERY_PORT 发送设备信息。
     *
     * @return 用于停止广播的 Runnable（调用 run() 即停止）
     */
    fun startBroadcasting(
        deviceName: String,
        httpPort: Int,
        appVersion: String
    ): BroadcastHandle {
        val handle = BroadcastHandle()
        val message = buildBroadcastMessage(deviceName, httpPort, appVersion)

        Thread({
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                val broadcastAddr = InetAddress.getByName("255.255.255.255")

                while (!handle.stopped) {
                    val packet = DatagramPacket(
                        message, message.size,
                        broadcastAddr, DISCOVERY_PORT
                    )
                    try {
                        socket.send(packet)
                    } catch (e: Exception) {
                        if (!handle.stopped) Log.w(TAG, "广播发送失败", e)
                    }
                    Thread.sleep(BROADCAST_INTERVAL_MS)
                }
            } catch (e: Exception) {
                Log.e(TAG, "广播线程异常", e)
            } finally {
                socket?.close()
            }
        }, "SyncBroadcast").start()

        return handle
    }

    /**
     * 启动 UDP 广播监听（在后台线程运行）。
     * 收到有效消息后通过 callback 回调。
     */
    fun startListening(onDeviceFound: (DiscoveredDevice) -> Unit): BroadcastHandle {
        val handle = BroadcastHandle()

        Thread({
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(DISCOVERY_PORT)
                socket.soTimeout = 3000  // 3 秒超时，便于检查 stopped 标记
                val buffer = ByteArray(1024)

                while (!handle.stopped) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                        val senderIp = packet.address.hostAddress ?: continue
                        val device = parseBroadcastMessage(
                            packet.data, packet.length, senderIp
                        )
                        if (device != null) {
                            onDeviceFound(device)
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        // 正常超时，继续循环检查 stopped 标记
                    }
                }
            } catch (e: Exception) {
                if (!handle.stopped) Log.e(TAG, "监听线程异常", e)
            } finally {
                socket?.close()
            }
        }, "SyncListener").start()

        return handle
    }

    /** 广播控制句柄，调用 stop() 停止广播/监听 */
    class BroadcastHandle {
        @Volatile
        var stopped = false
            private set

        fun stop() {
            stopped = true
        }
    }
}
