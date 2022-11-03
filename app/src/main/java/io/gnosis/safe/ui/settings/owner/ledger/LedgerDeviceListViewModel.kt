package io.gnosis.safe.ui.settings.owner.ledger

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeout
import okhttp3.internal.wait
import timber.log.Timber
import javax.inject.Inject

class LedgerDeviceListViewModel
@Inject constructor(
    private val ledgerController: LedgerController,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<LedgerDeviceListState>(appDispatchers) {

    private val scanResults = mutableListOf<ScanResult>()

    init {
        safeLaunch {
            ledgerController.scanResults().collect { result: ScanResult ->
                val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
                if (indexQuery != -1) { // A scan result already exists with the same address
                    scanResults[indexQuery] = result
                } else {
                    with(result.device) {
                        Timber.i("Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                    }
                    scanResults.add(result)
                }
                updateState {
                    LedgerDeviceListState(DeviceFound(scanResults.map { LedgerDeviceViewData(it.device.name ?: it.device.address) }))
                }
            }
        }
    }

    override fun initialState() = LedgerDeviceListState(ViewAction.Loading(true))

    fun handleResult(
        fragment: Fragment,
        disabledBluetoothHandler: () -> Unit,
        missingLocationPermissionHandler: () -> Unit,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        ledgerController.handleResult(fragment, disabledBluetoothHandler, missingLocationPermissionHandler, requestCode, resultCode, data)
    }

    fun handlePermissionResult(
        fragment: Fragment,
        deniedLocationPermissionHandler: () -> Unit,
        missingLocationPermissionHandler: () -> Unit,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        ledgerController.handlePermissionResult(
            fragment,
            deniedLocationPermissionHandler,
            missingLocationPermissionHandler,
            requestCode,
            permissions,
            grantResults
        )
    }

    fun requestLocationPermission(fragment: Fragment) {
        ledgerController.requestLocationPermission(fragment)
    }

    fun requestBLEPermission(fragment: Fragment) {
        ledgerController.requestBLEPermission(fragment)
    }

    fun scanForDevices(fragment: Fragment, missingLocationPermissionHandler: () -> Unit) {
        safeLaunch {
            updateState {
                LedgerDeviceListState(ViewAction.Loading(true))
            }
            scanResults.clear()
            ledgerController.startBleScan(fragment, missingLocationPermissionHandler)
            delay(LedgerController.LEDGER_OP_TIMEOUT)
            ledgerController.stopBleScan()
            updateState {
                LedgerDeviceListState(ViewAction.Loading(false))
            }
        }
    }

    fun scanError() {
        safeLaunch {
            throw DeviceScanError
        }
    }

    fun connectToDevice(context: Context, position: Int) {
        safeLaunch {
            val device = scanResults[position].device
            ledgerController.connectToDevice(context, device, object : LedgerController.DeviceConnectedCallback {
                override fun onDeviceConnected(device: BluetoothDevice) {
                    safeLaunch {
                        updateState {
                            LedgerDeviceListState(DeviceConnected(device))
                        }
                    }
                }
            })
        }
    }

    fun disconnectFromDevice() {
        ledgerController.teardownConnection()
    }
}

data class LedgerDeviceListState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class DeviceFound(
    val results: List<LedgerDeviceViewData>
) : BaseStateViewModel.ViewAction

data class DeviceConnected(
    val device: BluetoothDevice
) : BaseStateViewModel.ViewAction

object DeviceScanError : Throwable()
