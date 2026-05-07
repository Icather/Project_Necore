package icather.pages.dev.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * D2: Base64 多模态 OOM 防爆墙
 * 图片压缩引擎 — 将高分辨率手机照片压缩到安全的内存红线内。
 * 
 * 设计约束：
 * - 长边不超过 MAX_LONG_EDGE (1568px)，与 OpenAI vision detail=high 最大有效分辨率对齐
 * - JPEG 质量 75%，单张压缩后 Base64 体积约 200KB~500KB
 */
object ImageCompressor {

    private const val MAX_LONG_EDGE = 1568
    private const val JPEG_QUALITY = 75

    /**
     * 压缩图片 Uri，返回压缩后的 ByteArray。
     * 如果原始图片已经足够小，则不做处理直接返回原始字节。
     */
    fun compress(context: Context, uri: Uri): ByteArray {
        // Step 1: 只读取尺寸信息，不加载像素
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        // Step 2: 计算降采样比例
        var inSampleSize = 1
        val longEdge = maxOf(originalWidth, originalHeight)
        if (longEdge > MAX_LONG_EDGE) {
            // inSampleSize 必须是 2 的幂次
            while (longEdge / inSampleSize > MAX_LONG_EDGE * 2) {
                inSampleSize *= 2
            }
        }

        // Step 3: 降采样加载
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IllegalStateException("Failed to decode image from Uri: $uri")

        // Step 4: 如果降采样后仍然超出，则精确缩放
        val scaledBitmap = if (maxOf(bitmap.width, bitmap.height) > MAX_LONG_EDGE) {
            val scale = MAX_LONG_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height)
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            if (scaled !== bitmap) bitmap.recycle()
            scaled
        } else {
            bitmap
        }

        // Step 5: JPEG 压缩输出
        val output = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        scaledBitmap.recycle()

        return output.toByteArray()
    }

    /**
     * 获取指定 Uri 图片的原始文件大小（字节）。
     * 用于"未开启压缩"模式下的总大小校验。
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /** 最大允许的单次附件总大小（未压缩模式），默认 20MB */
    const val MAX_TOTAL_RAW_SIZE_BYTES = 20L * 1024 * 1024
}
