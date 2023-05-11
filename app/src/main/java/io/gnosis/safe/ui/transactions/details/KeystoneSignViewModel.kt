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

    override fun initialState() = KeystoneSignState(ViewAction.None)

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
                safeLaunch {
                    updateState {
                        parseSignature(signature.signature)?.let {
                            KeystoneSignState(KeystoneSignature(it))
                        } ?: KeystoneSignState(ViewAction.ShowError(KeystoneSignFailed()))
                    }
                }
            }
        }
    }

    // Same logic with safe-ios:
    // https://github.com/safe-global/safe-ios/blob/17b4537284be612621e5ee6e9d2f30f116a3753b/Multisig/UI/Settings/OwnerKeyManagement/KeystoneOwnerKey/KeystoneSignFlow.swift#L92
    fun parseSignature(
        signature: String,
        dataType: KeystoneEthereumSDK.DataType = ethSignRequest.dataType,
        chainId: Int = ethSignRequest.chainId,
        signingMode: SigningMode = this.signingMode
    ): String? {
        val data = signature.hexToByteArray()
        if (data.size < 65) return null

        val r = data.slice(0..31).toByteArray()
        val s = data.slice(32..63).toByteArray()
        var v: Byte

        if (data.size > 65) {
            val vBytes = data.slice(64 until data.size)
            val vInt = vBytes.toByteArray().asBigInteger()
            val vRecovered = vInt - (chainId * 2 + 35).toBigInteger()
            v = vRecovered.rem(256.toBigInteger()).toByte()
        } else {
            val vByte = data[64]
            val vInt = vByte.toUInt()
            val isLegacyTx = dataType == KeystoneEthereumSDK.DataType.Transaction
            v = if (isLegacyTx) {
                (vInt - 27u).toByte()
            } else {
                if (vInt >= 35u) {
                    val vRecovered = vInt - ((chainId * 2 + 35).toUInt())
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

    /*
    For future reference:

    CONFIRMATION, REJECTION, INITIATE_TRANSFER, DELEGATE, SIGNATURE_REQUEST -> PersonalMessage
    CREATE_SAFE, REVIEW_EXECUTION, SEND_TRANSACTION -> if (isLegacy) Transaction else TypedTransaction
    */
    private fun SigningMode.toDataType(): KeystoneEthereumSDK.DataType {
        return when (this) {
            SigningMode.CONFIRMATION -> KeystoneEthereumSDK.DataType.PersonalMessage
            SigningMode.REJECTION -> KeystoneEthereumSDK.DataType.PersonalMessage
            SigningMode.INITIATE_TRANSFER -> KeystoneEthereumSDK.DataType.PersonalMessage
        }
    }
}

class KeystoneSignFailed : Throwable()

data class KeystoneSignState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UnsignedUrReady(
    val qrCode: Bitmap?
) : BaseStateViewModel.ViewAction

data class KeystoneSignature(
    val signature: String
) : BaseStateViewModel.ViewAction
