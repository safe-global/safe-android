package io.gnosis.kouban.qrscanner

import android.app.Activity
import android.content.pm.PackageManager
import androidx.annotation.IntRange
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

fun handlePermissionsResultRequest(
    receivedRequestCode: Int,
    forRequestCode: Int,
    grantResults: IntArray,
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
) {
    if (receivedRequestCode == forRequestCode) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) onPermissionGranted()
        else onPermissionDenied()
    }
}

fun Activity.isPermissionGranted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Activity.requestPermissions(@IntRange(from = 0) requestCode: Int, vararg permissions: String) =
    ActivityCompat.requestPermissions(this, permissions, requestCode)

const val CAMERA_REQUEST_CODE = 0
