package icather.pages.dev.soul

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import icather.pages.dev.R
import icather.pages.dev.memory.UserMemoryManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * D4: 心跳关怀引擎 (Heartbeat Worker)
 *
 * 基于 Android WorkManager 实现 AI 后台静默唤醒：
 * - 根据时段（早/午/晚）推送不同的关怀消息
 * - 读取 UserMemoryManager 中的用户信息实现个性化关怀
 * - 无需网络调用模型，使用预设模板 + 用户名注入
 *   （未来可接入轻量 API 调用生成动态消息）
 *
 * 参考 SillyTavern 的陪伴机制：持续性 + 个性化 = 沉浸感
 */
class HeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "necore_heartbeat"
        const val NOTIFICATION_ID = 7749
        const val WORK_NAME = "necore_heartbeat_work"

        /**
         * 注册周期性后台任务（最短间隔 15 分钟，WorkManager 限制）
         * 实际推送频率通过内部逻辑控制为每天 2-3 次
         */
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<HeartbeatWorker>(
                6, TimeUnit.HOURS  // 每 6 小时检查一次
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * 取消后台任务
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("api_prefs", Context.MODE_PRIVATE)

        // 检查情绪感知开关是否开启
        if (!prefs.getBoolean("emotion_enabled", true)) {
            return Result.success()
        }

        // 读取用户记忆获取个性化信息
        val memoryManager = UserMemoryManager(applicationContext)
        val userData = memoryManager.read()
        val userName = userData.get("user_name")?.asString?.takeIf { it.isNotBlank() }

        // 根据时段选择关怀消息
        val message = generateCareMessage(userName)

        // 创建通知
        createNotificationChannel()
        showNotification(message)

        return Result.success()
    }

    private fun generateCareMessage(userName: String?): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = if (userName != null) "$userName，" else ""

        return when (hour) {
            in 6..10 -> {
                val morningMessages = listOf(
                    "${greeting}早上好！新的一天开始了，今天也要加油哦 ☀️",
                    "${greeting}早安！记得吃早餐，元气满满地开始新的一天 🍞",
                    "${greeting}早上好呀～希望今天的你心情愉快 🌸"
                )
                morningMessages.random()
            }
            in 12..14 -> {
                val noonMessages = listOf(
                    "${greeting}中午好！记得休息一下，别太累了 🍱",
                    "${greeting}午安～适当休息才能更高效哦 💤",
                    "${greeting}中午了！吃饱了没？注意营养均衡哦 🥗"
                )
                noonMessages.random()
            }
            in 21..23 -> {
                val nightMessages = listOf(
                    "${greeting}夜深了，早点休息吧，明天又是美好的一天 🌙",
                    "${greeting}晚安～好好睡一觉，我会一直在的 ⭐",
                    "${greeting}该休息啦！放下手机，做个好梦 🌜"
                )
                nightMessages.random()
            }
            else -> {
                val defaultMessages = listOf(
                    "${greeting}想你了，来聊聊天吧 💭",
                    "${greeting}在忙什么呢？有什么我能帮到你的吗？ 😊",
                    "${greeting}无聊的时候来找我呀，我一直都在 🎈"
                )
                defaultMessages.random()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI 关怀提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "来自 Necore AI 的温暖关怀"
            }
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(message: String) {
        // 点击通知跳转到主界面
        val intent = applicationContext.packageManager.getLaunchIntentForPackage(
            applicationContext.packageName
        )?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("✨ Necore")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
