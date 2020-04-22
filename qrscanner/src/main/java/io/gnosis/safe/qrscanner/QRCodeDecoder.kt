package io.gnosis.kouban.qrscanner

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/*
 * Check https://github.com/walleth/walleth/tree/master/app/src/main/java/org/walleth/activities/qrscan
 */
class QRCodeDecoder {
    private val reader = QRCodeReader()

    private fun LuminanceSource.decode() =
        reader.decode(BinaryBitmap(HybridBinarizer(this))).text

    fun decode(data: ByteArray, width: Int, height: Int): String {
        val centerX = width / 2
        val centerY = height / 2

        var size = width.coerceAtMost(height)
        size = (size.toDouble() * ReticleView.FRAME_SCALE).toInt()

        val halfSize = size / 2

        val left = centerX - halfSize
        val top = centerY - halfSize

        val source = PlanarYUVLuminanceSource(data, width, height, left, top, size, size, false)
        return nullOnThrow { source.decode() } ?: source.invert().decode()
    }
}
