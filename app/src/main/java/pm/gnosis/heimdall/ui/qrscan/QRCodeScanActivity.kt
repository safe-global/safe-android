package pm.gnosis.heimdall.ui.qrscan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import kotlinx.android.synthetic.main.layout_scan.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.CAMERA_REQUEST_CODE
import pm.gnosis.heimdall.utils.handlePermissionsResultRequest
import pm.gnosis.heimdall.utils.isPermissionGranted
import pm.gnosis.heimdall.utils.requestPermissions

/*
 * Check https://github.com/walleth/walleth/tree/master/app/src/main/java/org/walleth/activities/qrscan
 */
class QRCodeScanActivity : BaseActivity() {
    override fun screenId() = ScreenId.QR_SCAN

    private val videographer = Videographer(this).also { it.onSuccessfulScan = this::finishWithResult }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_scan)

        intent?.extras?.getString(DESCRIPTION_EXTRA)?.let {
            layout_scan_description.text = it
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isPermissionGranted(Manifest.permission.CAMERA)) {
            requestPermissions(CAMERA_REQUEST_CODE, Manifest.permission.CAMERA)
        } else {
            videographer.open(layout_scan_view_finder)
        }
    }

    override fun onPause() {
        super.onPause()
        if (videographer.isOpen) videographer.close()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        handlePermissionsResultRequest(requestCode, CAMERA_REQUEST_CODE, grantResults, onPermissionDenied = { finish() })
    }

    private fun finishWithResult(value: String) {
        val result = Intent().apply { putExtra(RESULT_EXTRA, value) }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    companion object {
        const val REQUEST_CODE = 0
        const val RESULT_EXTRA = "extra.string.scan_result"
        private const val DESCRIPTION_EXTRA = "extra.string.description"

        fun startForResult(activity: Activity, description: String? = null) =
            activity.startActivityForResult(QRCodeScanActivity.createIntent(activity, description), QRCodeScanActivity.REQUEST_CODE)

        fun startForResult(fragment: Fragment, description: String? = null) =
            fragment.startActivityForResult(QRCodeScanActivity.createIntent(fragment.requireContext(), description), QRCodeScanActivity.REQUEST_CODE)

        fun createIntent(context: Context, description: String? = null) = Intent(context, QRCodeScanActivity::class.java).apply {
            description?.let { putExtra(DESCRIPTION_EXTRA, it) }
        }
    }
}
