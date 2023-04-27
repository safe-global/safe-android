package io.gnosis.safe.ui.transactions.details

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import com.keystone.module.EthSignRequest
import com.keystone.sdk.KeystoneEthereumSDK
import com.keystone.sdk.KeystoneSDK
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
): BaseStateViewModel<KeystoneSignState>(appDispatchers) {
    private val sdk = KeystoneSDK()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateQrCode = object: Runnable {
        override fun run() {
            updateQrCode()
            mainHandler.postDelayed(this, 500)
        }
    }
    private lateinit var encoder: UREncoder

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
                val ethSignRequest = EthSignRequest(
                    requestId = UUID.randomUUID().toString(),
                    signData = safeTxHash,
                    dataType = signType,
                    chainId = chainId,
                    path = path,
                    xfp = sourceFingerprint,
                    address = "",
                    origin = "safe android"
                )
                encoder = sdk.eth.generateSignRequest(ethSignRequest)
                mainHandler.post(updateQrCode)
            }
        }
    }

    private fun updateQrCode() {
        safeLaunch {
            updateState {
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

                KeystoneSignState(UnsignedUrReady(qrCode))
            }
        }
    }

    fun onPause() {
        mainHandler.removeCallbacks(updateQrCode)
    }
}

data class KeystoneSignState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UnsignedUrReady(
    val qrCode: Bitmap?
): BaseStateViewModel.ViewAction