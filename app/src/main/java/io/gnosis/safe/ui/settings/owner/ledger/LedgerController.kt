package io.gnosis.safe.ui.settings.owner.ledger

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.gnosis.safe.ui.settings.owner.ledger.ble.ConnectionEventListener
import io.gnosis.safe.ui.settings.owner.ledger.ble.ConnectionManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import pm.gnosis.model.Solidity
import pm.gnosis.utils.toHexString
import timber.log.Timber
import java.util.*

class LedgerController(val context: Context) {

    var connectedDevice: BluetoothDevice? = null
        private set

    private var deviceConnectedCallback: DeviceConnectedCallback? = null

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val connectionEventListener by lazy {

        ConnectionEventListener().apply {

            onConnectionSetupComplete = { gatt ->
                Timber.d("Connected to ${gatt.device.name}")
                connectedDevice = gatt.device
                deviceConnectedCallback?.onDeviceConnected(connectedDevice!!)
            }

            onDisconnect = {
                connectedDevice = null
            }

            onCharacteristicRead = { _, characteristic ->
                Timber.d("Read from ${characteristic.uuid}: ${characteristic.value.toHexString()}")
            }

            onCharacteristicWrite = { _, characteristic ->
                Timber.d("Wrote to ${characteristic.uuid}")
            }

            onMtuChanged = { _, mtu ->
                Timber.d("MTU updated to $mtu")
            }

            onCharacteristicChanged = { _, characteristic ->
                Timber.d("Value changed on ${characteristic.uuid}: ${characteristic.value.toHexString()}")
            }

            onNotificationsEnabled = { _, characteristic ->
                Timber.d("Enabled notifications on ${characteristic.uuid}")
                notifyingCharacteristics.add(characteristic.uuid)
            }

            onNotificationsDisabled = { _, characteristic ->
                Timber.d("Disabled notifications on ${characteristic.uuid}")
                notifyingCharacteristics.remove(characteristic.uuid)
            }
        }
    }

    init {
        ConnectionManager.registerListener(connectionEventListener)
    }

    private var notifyingCharacteristics = mutableListOf<UUID>()

    private lateinit var scanCallback: ScanCallback
    private val scanResultFlow = callbackFlow {
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                offer(result)
            }
        }
        awaitClose { stopBleScan() }
    }

    fun scanResults() =
        scanResultFlow.conflate()

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(LEDGER_SERVICE_DATA_UUID))
        .build()

    private var isScanning = false


    fun startBleScan(fragment: Fragment, missingLocationPermissionHandler: () -> Unit) {
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth(fragment)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                missingLocationPermissionHandler.invoke()
            } else {
                bleScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
                isScanning = true
            }
        }
    }

    fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    fun connectToDevice(context: Context, device: BluetoothDevice, callback: DeviceConnectedCallback) {
        if (isScanning) {
            stopBleScan()
        }
        deviceConnectedCallback = callback
        ConnectionManager.connect(device, context)
    }

    fun teardownConnection() {
        ConnectionManager.unregisterListener(connectionEventListener)
        connectedDevice?.let {  ConnectionManager.teardownConnection(it) }
    }

    suspend fun getAddresses(baseDerivationPath: String, startIndex: Int, endIndex: Int): List<Solidity.Address> {
        //TODO
        return listOf()
    }

    private fun promptEnableBluetooth(fragment: Fragment) {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            fragment.startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE_BLUETOOTH)
        }
    }

    fun requestLocationPermission(fragment: Fragment) {
        fragment.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION_PERMISSION)
    }

    fun handleResult(
        fragment: Fragment,
        disabledBluetoothHandler: () -> Unit,
        missingLocationPermissionHandler: () -> Unit,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        when (requestCode) {
            REQUEST_CODE_ENABLE_BLUETOOTH -> {
                if (resultCode != Activity.RESULT_OK) {
                    disabledBluetoothHandler.invoke()
                } else {
                    startBleScan(fragment, missingLocationPermissionHandler)
                }
            }
        }
    }

    fun handlePermissionResult(
        fragment: Fragment,
        deniedLocationPermissionHandler: () -> Unit,
        missingLocationPermissionHandler: () -> Unit,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_LOCATION_PERMISSION -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    deniedLocationPermissionHandler.invoke()
                } else {
                    startBleScan(fragment, missingLocationPermissionHandler)
                }
            }
        }
    }

    interface DeviceConnectedCallback {
        fun onDeviceConnected(device: BluetoothDevice)
    }

    companion object {
        val LEDGER_SERVICE_DATA_UUID = UUID.fromString("13d63400-2c97-0004-0000-4c6564676572")
        private const val REQUEST_CODE_ENABLE_BLUETOOTH = 1
        private const val REQUEST_CODE_LOCATION_PERMISSION = 2
    }
}

private fun Context.hasPermission(permissionType: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permissionType) ==
            PackageManager.PERMISSION_GRANTED
}
