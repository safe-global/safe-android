package io.gnosis.safe.ui.transactions.details

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.keystone.module.EthSignRequest
import com.keystone.sdk.KeystoneEthereumSDK
import com.keystone.sdk.KeystoneSDK
import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.UREncoder
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.utils.toSignatureString
import io.gnosis.safe.qrscanner.HasFinished
import io.gnosis.safe.qrscanner.IsValid
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.Runnable
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.hexToByteArray
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class KeystoneSignViewModel
@Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val qrCodeGenerator: QrCodeGenerator,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<KeystoneSignState>(appDispatchers) {
    private val sdk = KeystoneSDK()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val handlerThread = HandlerThread("HandlerThread")
    private lateinit var backgroundHandler: Handler
    private lateinit var encoder: UREncoder
    private lateinit var ethSignRequest: EthSignRequest
    private lateinit var signingMode: SigningMode
    private var ur: UR? = null

    private val updateQrCode = object : Runnable {
        override fun run() {
            updateQrCode()
            backgroundHandler.postDelayed(this, 500)
        }
    }

    companion object {
        const val UR_PREFIX_OF_SIGNATURE = "UR:ETH-SIGNATURE"
    }

    override fun initialState() = KeystoneSignState(ViewAction.Loading(true))

    fun setSignRequestUREncoder(
        ownerAddress: Solidity.Address,
        safeTxHash: String,
        signingMode: SigningMode,
        chainId: Int
    ) {
        this.signingMode = signingMode
        safeLaunch {
            val owner = credentialsRepository.owner(ownerAddress)
            owner?.let {
                ethSignRequest = EthSignRequest(
                    requestId = UUID.randomUUID().toString(),
                    signData = safeTxHash,
                    dataType = signingMode.toDataType(),
                    chainId = chainId,
                    path = owner.keyDerivationPath!!,
                    xfp = owner.sourceFingerprint!!,
                    address = "",
                    origin = "safe android"
                )
                encoder = sdk.eth.generateSignRequest(ethSignRequest)

                handlerThread.start()
                backgroundHandler = Handler(handlerThread.looper)
                backgroundHandler.post(updateQrCode)
            }
        }
    }

    private fun updateQrCode() {
        val qrValue = encoder.nextPart()
        val qrCode = runCatching {
            qrCodeGenerator.generateQrCode(
                qrValue,
                512,
                512,
                Color.WHITE
            )
        }.onFailure { Timber.e(it) }
            .getOrNull()

        mainHandler.post {
            safeLaunch {
                updateState {
                    KeystoneSignState(UnsignedUrReady(qrCode))
                }
            }
        }
    }

    fun stopUpdatingQrCode() {
        backgroundHandler.removeCallbacks(updateQrCode)
        handlerThread.quitSafely()
    }

    fun validator(scannedValue: String): Pair<IsValid, HasFinished> {
        return if (scannedValue.startsWith(UR_PREFIX_OF_SIGNATURE)) {
            sdk.decodeQR(scannedValue)?.let {
                this.ur = it
                Pair(true, true)
            } ?: Pair(true, false)
        } else {
            Pair(false, true)
        }
    }

    fun handleQrResult() {
        this.ur?.let { ur ->
            val signature = sdk.eth.parseSignature(ur)
            if (signature.requestId == ethSignRequest.requestId) {
                val ecdsaSignature = parseSignature(signature.signature)
                safeLaunch {
                    updateState {
                        KeystoneSignState(KeystoneSignature(ecdsaSignature))
                    }
                }
            }
        }
    }

    private fun parseSignature(signature: String): String {
        val data = signature.hexToByteArray()
        if (data.size < 65) throw Exception("invalid data size")

        val r = data.slice(0..31).toByteArray()
        val s = data.slice(32..63).toByteArray()
        var v: Byte

        if (data.size > 65) {
            val vBytes = data.slice(64 until data.size)
            val vRecovered = vBytes - (ethSignRequest.chainId * 2 + 35).toByte()
            v = vRecovered.toByteArray().asBigInteger().rem(256.toBigInteger()).toByte()
        } else {
            val vByte = data[64]
            val vInt = vByte.toUInt()
            val isLegacyTx = ethSignRequest.dataType == KeystoneEthereumSDK.DataType.Transaction
            v = if (isLegacyTx) {
                (vInt - 27u).toByte()
            } else {
                if (vInt >= 35u) {
                    val vRecovered = vInt - ((ethSignRequest.chainId * 2 + 35).toUInt())
                    vRecovered.rem(256u).toByte()
                } else {
                    vByte
                }
            }
        }

        if (signingMode == SigningMode.CONFIRMATION
            || signingMode == SigningMode.REJECTION
            || signingMode == SigningMode.INITIATE_TRANSFER
        ) {
            v = (v + 4.toByte()).toByte()
        }

        val ecdsaSignature = ECDSASignature.fromComponents(r, s, v)
        return ecdsaSignature.toSignatureString()
    }

    private fun SigningMode.toDataType(): KeystoneEthereumSDK.DataType {
        return when (this) {
            SigningMode.CONFIRMATION -> KeystoneEthereumSDK.DataType.PersonalMessage
            SigningMode.REJECTION -> KeystoneEthereumSDK.DataType.PersonalMessage
            SigningMode.INITIATE_TRANSFER -> KeystoneEthereumSDK.DataType.PersonalMessage
        }
    }
}

data class KeystoneSignState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UnsignedUrReady(
    val qrCode: Bitmap?
) : BaseStateViewModel.ViewAction

data class KeystoneSignature(
    val signature: String
) : BaseStateViewModel.ViewAction
