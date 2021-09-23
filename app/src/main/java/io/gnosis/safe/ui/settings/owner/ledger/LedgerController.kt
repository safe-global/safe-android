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
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.parseGetAddress
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.parseSignMessage
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.splitPath
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.unwrapAPDU
import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper.wrapAPDU
import io.gnosis.safe.ui.settings.owner.ledger.ble.ConnectionEventListener
import io.gnosis.safe.ui.settings.owner.ledger.ble.ConnectionManager
import io.gnosis.safe.ui.settings.owner.ledger.transport.LedgerException
import io.gnosis.safe.ui.settings.owner.ledger.transport.SerializeHelper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.toHexString
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.*
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
    private var addressContinuation: Continuation<Solidity.Address>? = null
    private var signContinuation: Continuation<String>? = null

    var writeCharacteristic: BluetoothGattCharacteristic? = null
    var notifyCharacteristic: BluetoothGattCharacteristic? = null

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
                connectedDevice = null
            }

            onCharacteristicRead = { _, characteristic -> }

            onCharacteristicWrite = { _, characteristic -> }

            onMtuChanged = { _, mtu -> }

            onCharacteristicChanged = { _, characteristic ->
                val unwrappedResponse = unwrapAPDU(characteristic.value)

                addressContinuation?.let {
                    val address = parseGetAddress(unwrappedResponse)
                    Timber.d("onCharacteristicChanged() | Parsed address: $address")
                    it.resumeWith(Result.success(address.asEthereumAddress()!!))
                    addressContinuation = null
                }

                signContinuation?.let {
                    try {
                        val signature = parseSignMessage(unwrappedResponse)
                        Timber.d("onCharacteristicChanged() | Parsed signature: $signature")
                        it.resumeWith(Result.success(signature))
                    } catch (e: Exception) {
                        Timber.e(e)
                        it.resumeWithException(e)
                    }
                    signContinuation = null
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
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
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
                if (isScanning) {
                    stopBleScan()
                }
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
        ConnectionManager.registerListener(connectionEventListener)
        if (isScanning) {
            stopBleScan()
        }
        Timber.d("Attempting to connect to ${device.name}")
        deviceConnectedCallback = callback
        ConnectionManager.connect(device, context)
    }

    fun teardownConnection() {
        ConnectionManager.unregisterListener(connectionEventListener)
        connectedDevice?.let { ConnectionManager.teardownConnection(it) }
    }

    fun getSignCommand(path: String, message: String): ByteArray {

        val paths = splitPath(path)
        val messageBytes = message.hexToByteArray()

        val pathsData = ByteArray(paths.size)
        paths.forEachIndexed { index, element ->
            pathsData[index] = element
        }

        val commandData = mutableListOf<Byte>()
        commandData.add(0xe0.toByte())
        commandData.add(0x08.toByte())
        commandData.add(0x00.toByte())
        commandData.add(0x00.toByte())

        val messageData = ByteArrayOutputStream()
        SerializeHelper.writeUint32BE(messageData, messageBytes.size.toLong())
        messageBytes.forEachIndexed { index, element ->
            messageData.write(element.toInt())
        }

        commandData.add((paths.size + messageBytes.size + 4).toByte())
        commandData.addAll(pathsData.toList())
        commandData.addAll(messageData.toByteArray().toList())

        // Command length should be 150 bytes length otherwise we should split
        // it into chuncks. As we sign hashes we should be fine for now.
        val command = commandData.toByteArray()
        Timber.d("Sign command: ${command.toHexString()}")

        if (command.size > 150) throw LedgerException(LedgerException.ExceptionReason.IO_ERROR, "invalid data format")

        return command
    }

    suspend fun getSignature(path: String, message: String): String = suspendCoroutine {
        ConnectionManager.writeCharacteristic(connectedDevice!!, writeCharacteristic!!, wrapAPDU(getSignCommand(path, message)))
        signContinuation = it
    }

    fun getAddressCommand(path: String, displayVerificationDialog: Boolean = false, chainCode: Boolean = false): ByteArray {

        val paths = splitPath(path)

        val commandData = mutableListOf<Byte>()

        val pathsData = ByteArray(1 + paths.size)
        pathsData[0] = paths.size.toByte()

        paths.forEachIndexed { index, element ->
            pathsData[1 + index] = element
        }

        commandData.add(0xe0.toByte())
        commandData.add(0x02.toByte())
        commandData.add((if (displayVerificationDialog) 0x01.toByte() else 0x00.toByte()))
        commandData.add((if (chainCode) 0x01.toByte() else 0x00.toByte()))
        commandData.addAll(pathsData.toList())

        val command = commandData.toByteArray()
        Timber.d("Get address command: ${command.toHexString()}")

        return command
    }

    suspend fun getAddress(device: BluetoothDevice, path: String): Solidity.Address = suspendCoroutine {
        ConnectionManager.writeCharacteristic(device, writeCharacteristic!!, wrapAPDU(getAddressCommand(path)))
        addressContinuation = it
    }

    suspend fun addressesForPage(derivationPath: String, start: Long, pageSize: Int): List<Solidity.Address> {
        val addressPage = mutableListOf<Solidity.Address>()
        for (i in start until start + pageSize) {
            val address = getAddress(connectedDevice!!, derivationPath.replace("{index}", i.toString()))
            addressPage.add(address)
        }
        return addressPage
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
