package io.gnosis.safe.ui.safe.share

import android.graphics.Bitmap
import android.graphics.Color
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import timber.log.Timber
import javax.inject.Inject

class ShareSafeViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val ensRepository: EnsRepository,
    private val qrCodeGenerator: QrCodeGenerator,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ShareSafeState>(appDispatchers) {

    override fun initialState(): ShareSafeState = ShareSafeState()

    fun load() {
        safeLaunch {
            safeRepository.getActiveSafe()?.let { activeSafe ->
                val ensName = runCatching { ensRepository.reverseResolve(activeSafe.address) }
                    .onFailure { Timber.e(it) }
                    .getOrNull()
                val qrCode = runCatching {
                    qrCodeGenerator.generateQrCodeSync(
                        activeSafe.address.asEthereumAddressChecksumString(),
                        512,
                        512,
                        Color.WHITE
                    )
                }.onFailure { Timber.e(it) }
                    .getOrNull()

                updateState {
                    ShareSafeState(ShowSafeDetails(SafeDetails(activeSafe, ensName, qrCode)))
                }
            } ?: throw IllegalStateException("Safe share is only accessible with an active safe")
        }
    }
}

data class ShareSafeState(
    override var viewAction: BaseStateViewModel.ViewAction? = BaseStateViewModel.ViewAction.Loading(true)
) : BaseStateViewModel.State

data class SafeDetails(
    val safe: Safe,
    val ensName: String?,
    val qrCode: Bitmap?
)

data class ShowSafeDetails(
    val safeDetails: SafeDetails
) : BaseStateViewModel.ViewAction
