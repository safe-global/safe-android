package pm.gnosis.heimdall.common.utils

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.support.v4.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

interface QrCodeGenerator {
    fun generateQrCode(contents: String, width: Int = 512, height: Int = 512): Observable<Bitmap>
    fun generateQrCodeSync(contents: String, width: Int, height: Int): Bitmap
}

@Singleton
class ZxingQrCodeGenerator @Inject constructor() : QrCodeGenerator {

    override fun generateQrCode(contents: String, width: Int, height: Int) =
            Observable.fromCallable {
                generateQrCodeSync(contents, width, height)
            }
                    .subscribeOn(Schedulers.computation())

    override fun generateQrCodeSync(contents: String, width: Int, height: Int): Bitmap {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(contents, BarcodeFormat.QR_CODE, width, height)
            val bmp = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            return bmp
        } catch (e: WriterException) {
            throw e
        }
    }
}

fun Activity.scanQrCode() = ZxingIntentIntegrator(this).initiateScan(ZxingIntentIntegrator.QR_CODE_TYPES)
fun Fragment.scanQrCode() = ZxingIntentIntegrator(this).initiateScan(ZxingIntentIntegrator.QR_CODE_TYPES)
