package io.gnosis.safe.ui.settings.owner.ledger

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.owner.ledger.ble.ConnectionManager
import kotlinx.coroutines.flow.collect
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
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
                    LedgerDeviceListState(DeviceFound(scanResults.map { LedgerDeviceViewData(it.device.name) }))
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

    fun scanForDevices(fragment: Fragment, missingLocationPermissionHandler: () -> Unit) {
        safeLaunch {
            updateState {
                LedgerDeviceListState(ViewAction.Loading(true))
            }
            scanResults.clear()
            ledgerController.startBleScan(fragment, missingLocationPermissionHandler)
        }
    }

    fun scanError() {
        safeLaunch {
            throw DeviceScanError
        }
    }

    fun connectAndOpenList(context: Context, position: Int) {
        safeLaunch {
            val device = scanResults[position].device
            if (device == ledgerController.connectedDevice) {
                updateState {
                    LedgerDeviceListState(DeviceConnected(device))
                }
            } else {
                ledgerController.connectToDevice(context, device, object : LedgerController.DeviceConnectedCallback {
                    override fun onDeviceConnected(device: BluetoothDevice) {
                        ledgerController.loadDeviceCharacteristics()
                        ConnectionManager.enableNotifications(device, ledgerController.notifyCharacteristic!!)
                        safeLaunch {
                            updateState {
                                LedgerDeviceListState(DeviceConnected(device))
                            }
                        }
                    }
                })
            }
        }
    }

    fun connectToDevice(context: Context, position: Int) {
        val device = scanResults[position].device


        if (!ConnectionManager.isDeviceConnected(device)) {
            ledgerController.connectToDevice(context, scanResults[position].device, object : LedgerController.DeviceConnectedCallback {
                override fun onDeviceConnected(device: BluetoothDevice) {

                    ledgerController.loadDeviceCharacteristics()
                    ConnectionManager.enableNotifications(device, ledgerController.notifyCharacteristic!!)

                    safeLaunch {
                        val address = ledgerController.getAddress(device, "44'/60'/0'/0/0")
                        Timber.e("address received: ${address.asEthereumAddressChecksumString()}")

                    }
                }
            })
        } else {
            safeLaunch {
                val address = ledgerController.getAddress(device, "44'/60'/0'/0/0")
                Timber.e("address received: ${address.asEthereumAddressChecksumString()}")
            }
        }
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
