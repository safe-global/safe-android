package io.gnosis.safe.ui.settings.owner.ledger

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
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
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.chunkDataAPDU
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.commandGetAddress
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.commandSignMessage
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.commandSignTx
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.parseGetAddress
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.parseSignMessage
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.unwrapAPDU
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.wrapAPDU
import io.gnosis.safe.ui.settings.owner.ledger.ble.ConnectionEventListener
import io.gnosis.safe.ui.settings.owner.ledger.ble.ConnectionManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.nullOnThrow
import timber.log.Timber
import java.util.LinkedList
import java.util.Queue
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LedgerController(val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    var connectedDevice: BluetoothDevice? = null
        private set

    private var deviceConnectedCallback: DeviceConnectedCallback? = null
    private var addressContinuations: Queue<Continuation<Solidity.Address>> = LinkedList()
    private var signContinuations: Queue<Continuation<String>> = LinkedList()

    var writeCharacteristic: BluetoothGattCharacteristic? = null
    var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private var mtu: Int = 20

    private fun loadDeviceCharacteristics() {
        val characteristic = connectedDevice?.let {
            ConnectionManager.servicesOnDevice(it)?.flatMap { service ->
                service.characteristics ?: listOf()
            }
        } ?: listOf()
        writeCharacteristic = characteristic.find { it.uuid == UUID.fromString("13d63400-2c97-0004-0002-4c6564676572") }
        notifyCharacteristic = characteristic.find { it.uuid == UUID.fromString("13d63400-2c97-0004-0001-4c6564676572") }
    }

    private val connectionEventListener by lazy {

        ConnectionEventListener().apply {

            onConnectionSetupComplete = { gatt ->
                Timber.d("Connected to ${gatt.device.name}")
                connectedDevice = gatt.device
                loadDeviceCharacteristics()
                ConnectionManager.enableNotifications(gatt.device, notifyCharacteristic!!)
                deviceConnectedCallback?.onDeviceConnected(connectedDevice!!)
            }

            onDisconnect = {
                Timber.d("onDisconnect()")
            }

            onCharacteristicRead = { _, characteristic ->
                Timber.d("onCharacteristicRead()")
            }

            onCharacteristicWrite = { _, characteristic ->
                Timber.d("onCharacteristicWrite()")
            }

            onCharacteristicWriteError = { _, _, error ->
                val addressContinuation = nullOnThrow {
                    addressContinuations.remove()
                }
                addressContinuation?.let {
                    it.resumeWithException(error)
                }
            }

            onMtuChanged = { _, mtu ->
                this@LedgerController.mtu = mtu
            }

            onCharacteristicChanged = { _, characteristic ->

                val addressContinuation = nullOnThrow {
                    addressContinuations.remove()
                }
                addressContinuation?.let {
                    try {
                        val unwrappedResponse = unwrapAPDU(characteristic.value)
                        val address = parseGetAddress(unwrappedResponse)
                        Timber.d("onCharacteristicChanged() | Parsed address: $address")
                        it.resumeWith(Result.success(address.asEthereumAddress()!!))
                    } catch (e: Throwable) {
                        Timber.e(e)
                        it.resumeWithException(e)
                    }
                }

                val signContinuation = nullOnThrow {
                    signContinuations.remove()
                }
                signContinuation?.let {
                    try {
                        val unwrappedResponse = unwrapAPDU(characteristic.value)
                        val signature = parseSignMessage(unwrappedResponse)
                        Timber.d("onCharacteristicChanged() | Parsed signature: $signature")
                        it.resumeWith(Result.success(signature))
                    } catch (e: Exception) {
                        Timber.e(e)
                        it.resumeWithException(e)
                    }
                }
            }

            onNotificationsEnabled = { _, characteristic ->
                notifyingCharacteristics.add(characteristic.uuid)
            }

            onNotificationsDisabled = { _, characteristic ->
                notifyingCharacteristics.remove(characteristic.uuid)
            }
        }
    }

    private var notifyingCharacteristics = mutableListOf<UUID>()
    private lateinit var scanCallback: ScanCallback
    private val scanResultFlow = callbackFlow<ScanResult> {
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Timber.d("$callbackType, result: ${result.device.address}/${result.device.name}")
                trySend(result)
            }
        }
        awaitClose { stopBleScan() }
    }

    fun scanResults() =
        scanResultFlow.conflate()

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()

    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(LEDGER_SERVICE_DATA_UUID))
        .build()

    private var isScanning = false

    fun startBleScan(fragment: Fragment, missingPermissionHandler: () -> Unit) {
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth(fragment)
        } else {
            if (locationPermissionMissing() || blePermissionMissing()) {
                missingPermissionHandler.invoke()
            } else {
                if (isScanning) {
                    stopBleScan()
                }
                bleScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
                isScanning = true
            }
        }
    }

    private fun locationPermissionMissing() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            && (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))

    private fun blePermissionMissing() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && (!context.hasPermission(Manifest.permission.BLUETOOTH_SCAN) || !context.hasPermission(Manifest.permission.BLUETOOTH_CONNECT))

    fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    fun connectToDevice(context: Context, device: BluetoothDevice, callback: DeviceConnectedCallback) {
        ConnectionManager.registerListener(connectionEventListener)
        if (isScanning) {
            stopBleScan()
        }
        Timber.d("Attempting to connect to ${device.name}")
        deviceConnectedCallback = callback
        ConnectionManager.connect(device, context)
    }

    fun isConnected(): Boolean = connectedDevice?.let {
        return ConnectionManager.isDeviceConnected(connectedDevice!!)
    } ?: false

    fun teardownConnection() {
        connectedDevice?.let { ConnectionManager.teardownConnection(it) }
        connectedDevice = null
        ConnectionManager.unregisterListener(connectionEventListener)
    }

    suspend fun getTxSignature(path: String, encodedTx: String): String = suspendCoroutine { continuation ->
        val payload = commandSignTx(path, encodedTx)
        val chunks = chunkDataAPDU(payload, 150)
        chunks.forEach {
            ConnectionManager.writeCharacteristic(connectedDevice!!, writeCharacteristic!!, it)
        }
        signContinuations.add(continuation)
    }

    suspend fun getSignature(path: String, message: String): String = suspendCoroutine { continuation ->
        ConnectionManager.writeCharacteristic(connectedDevice!!, writeCharacteristic!!, wrapAPDU(commandSignMessage(path, message)))
        signContinuations.add(continuation)
    }

    private suspend fun getAddress(device: BluetoothDevice, path: String): Solidity.Address = suspendCancellableCoroutine { continuation ->
        ConnectionManager.writeCharacteristic(device, writeCharacteristic!!, wrapAPDU(commandGetAddress(path)))
        addressContinuations.add(continuation)
    }

    suspend fun addressesForPage(derivationPath: String, start: Long, pageSize: Int): List<Solidity.Address> {
        val addressPage = mutableListOf<Solidity.Address>()
        kotlin.runCatching {
            withTimeout(LEDGER_OP_TIMEOUT) {
                Timber.d("addressesForPage() |  connectedDevice: $connectedDevice")
                for (i in start until start + pageSize) {
                    val pathWithIndex = derivationPath.replace("{index}", i.toString())
                    val address = getAddress(connectedDevice!!, pathWithIndex)

                    Timber.d(
                        "---> addressesForPage() |  received address: ${
                            Solidity.Address(address.value).asEthereumAddressChecksumString()
                        } pathWithIndex: $pathWithIndex"
                    )
                    addressPage.add(address)
                }
            }
        }.onFailure {
            addressContinuations.clear()
            throw it
        }
        return addressPage
    }

    private fun promptEnableBluetooth(fragment: Fragment) {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            fragment.startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE_BLUETOOTH)
        }
    }

    fun requestPermissionForBLE(fragment: Fragment) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            fragment.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_BLE_PERMISSION)
        } else {
            fragment.requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), REQUEST_CODE_BLE_PERMISSION)
        }
    }

    fun handleResult(
        fragment: Fragment,
        disabledBluetoothHandler: () -> Unit,
        missingPermissionHandler: () -> Unit,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        when (requestCode) {
            REQUEST_CODE_ENABLE_BLUETOOTH -> {
                if (resultCode != Activity.RESULT_OK) {
                    disabledBluetoothHandler.invoke()
                } else {
                    startBleScan(fragment, missingPermissionHandler)
                }
            }
        }
    }

    fun handlePermissionResult(
        fragment: Fragment,
        deniedLocationPermissionHandler: () -> Unit,
        missingPermissionHandler: () -> Unit,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_BLE_PERMISSION -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    deniedLocationPermissionHandler.invoke()
                } else {
                    startBleScan(fragment, missingPermissionHandler)
                }
            }
        }
    }

    interface DeviceConnectedCallback {
        fun onDeviceConnected(device: BluetoothDevice)
    }

    companion object {
        const val LEDGER_OP_TIMEOUT = 10000L
        const val LEDGER_LIVE_PATH = "44'/60'/{index}'/0/0"
        const val LEDGER_PATH = "44'/60'/0'/{index}"
        val LEDGER_SERVICE_DATA_UUID = UUID.fromString("13d63400-2c97-0004-0000-4c6564676572")
        private const val REQUEST_CODE_ENABLE_BLUETOOTH = 1
        private const val REQUEST_CODE_BLE_PERMISSION = 3
    }
}

private fun Context.hasPermission(permissionType: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permissionType) ==
            PackageManager.PERMISSION_GRANTED
}
