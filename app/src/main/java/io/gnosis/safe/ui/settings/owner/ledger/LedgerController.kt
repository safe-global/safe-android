package io.gnosis.safe.ui.settings.owner.ledger

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import timber.log.Timber

class LedgerController {


    private var bluetoothAdapter: BluetoothAdapter? = null

    private var bluetoothGatt: BluetoothGatt? = null



    private lateinit var leScanCallback: ScanCallback
    private val deviceFlow = callbackFlow {
        leScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                offer(result.device)
            }
        }
    }

    fun activateDeviceFlow() =
        deviceFlow.cancellable().conflate()


    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
            }
        }
    }

    fun connect(context: Context, address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                // connect to the GATT server on the device
                bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback)
                return true
            } catch (exception: IllegalArgumentException) {
                Timber.w("Device not found with provided address.")
                return false
            }


        } ?: run {
            Timber.w("BluetoothAdapter not initialized")
            return false
        }
    }




    private var scanning = false

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000


    fun checkBluetooth(fragment: Fragment): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            false
        } else if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            fragment.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            false
        } else {
            true
        }
    }

    suspend fun scanForDevices() = coroutineScope {
        val bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            launch {
                delay(SCAN_PERIOD)
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }
            scanning = true
            bluetoothLeScanner.startScan(leScanCallback)
        } else {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
        }
    }

    fun stopScan() {
        val bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
        scanning = false
        bluetoothLeScanner.stopScan(leScanCallback)
    }

    companion object {
        const val REQUEST_ENABLE_BT = 0
    }
}
