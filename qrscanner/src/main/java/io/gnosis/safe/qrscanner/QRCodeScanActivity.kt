package io.gnosis.kouban.qrscanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.screen_scan.*

/*
 * Check https://github.com/walleth/walleth/tree/master/app/src/main/java/org/walleth/activities/qrscan
 */
class QRCodeScanActivity : AppCompatActivity() {

    private lateinit var videographer: Videographer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videographer = Videographer(this, QRCodeDecoder())
        videographer.onSuccessfulScan = ::finishWithResult
        setContentView(R.layout.screen_scan)

        intent?.extras?.getString(DESCRIPTION_EXTRA)?.let {
            scan_description.text = it
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isPermissionGranted(Manifest.permission.CAMERA)) {
            requestPermissions(CAMERA_REQUEST_CODE, Manifest.permission.CAMERA)
        } else {
            videographer.open(scan_view_finder)
        }
    }

    override fun onPause() {
        super.onPause()
        if (videographer.isOpen) videographer.close()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        handlePermissionsResultRequest(requestCode, CAMERA_REQUEST_CODE, grantResults, onPermissionDenied = { finish() })
    }

    // TODO: We can use Rx to listen for successful events and finish instead of passing a callback
    private fun finishWithResult(value: String) {
        val result = Intent().apply { putExtra(RESULT_EXTRA, value) }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    companion object {
        const val RESULT_EXTRA = "extra.string.scan_result"
        private const val REQUEST_CODE = 0
        private const val DESCRIPTION_EXTRA = "extra.string.description"

        fun startForResult(activity: Activity, description: String? = null) =
            activity.startActivityForResult(createIntent(activity, description), REQUEST_CODE)

        fun startForResult(fragment: Fragment, description: String? = null) =
            fragment.startActivityForResult(createIntent(fragment.requireContext(), description), REQUEST_CODE)

        fun createIntent(context: Context?, description: String? = null) = Intent(context, QRCodeScanActivity::class.java).apply {
            description?.let { putExtra(DESCRIPTION_EXTRA, it) }
        }

        fun handleResult(
            requestCode: Int, resultCode: Int, data: Intent?, onCancelledResult: (() -> Unit)? = null, onQrCodeResult: (String) -> Unit
        ): Boolean {
            if (requestCode == REQUEST_CODE) {
                if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(RESULT_EXTRA)) {
                    onQrCodeResult(data.getStringExtra(RESULT_EXTRA)!!)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    onCancelledResult?.invoke()
                }
                return true
            }
            return false
        }
    }
}

fun <T> nullOnThrow(func: () -> T): T? = try {
    func.invoke()
} catch (e: Exception) {
    null
}
