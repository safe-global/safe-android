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
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.Runnable
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
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
    private var ur: UR? = null
    private var requestId = ""

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
        signType: KeystoneEthereumSDK.DataType,
        chainId: Int
    ) {
        safeLaunch {
            val owner = credentialsRepository.owner(ownerAddress)
            owner?.let {
                val path = owner.keyDerivationPath!!
                val sourceFingerprint = owner.sourceFingerprint!!
                requestId = UUID.randomUUID().toString()
                val ethSignRequest = EthSignRequest(
                    requestId = requestId,
                    signData = safeTxHash,
                    dataType = signType,
                    chainId = chainId,
                    path = path,
                    xfp = sourceFingerprint,
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

    fun validator(scannedValue: String): Boolean {
        return if (scannedValue.startsWith(UR_PREFIX_OF_SIGNATURE)) {
            sdk.decodeQR(scannedValue)?.let {
                this.ur = it
                true
            } ?: false
        } else {
            false
        }
    }

    fun handleQrResult() {
        this.ur?.let { ur ->
            val signature = sdk.eth.parseSignature(ur)
            println(signature.requestId)
            println(requestId)
            if (signature.requestId == requestId) {
                println(signature.signature)
            }
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