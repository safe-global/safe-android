package io.gnosis.safe.ui.safe.share

import android.graphics.Bitmap
import android.graphics.Color
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import timber.log.Timber
import javax.inject.Inject

class ShareSafeViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val ensRepository: EnsRepository,
    private val qrCodeGenerator: QrCodeGenerator,
    private val settingsHandler: SettingsHandler,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ShareSafeState>(appDispatchers) {

    override fun initialState(): ShareSafeState = ShareSafeState()

    fun load() {
        safeLaunch {
            safeRepository.getActiveSafe()?.let { activeSafe ->
                val ensName = runCatching {
                    ensRepository.reverseResolve(
                        activeSafe.address,
                        activeSafe.chain
                    )
                }.onFailure { Timber.e(it) }
                    .getOrNull()

                val qrCode = if (settingsHandler.chainPrefixQr) {
                    generateQrCode(activeSafe.address, activeSafe.chain.shortName)
                } else {
                    generateQrCode(activeSafe.address)
                }

                updateState {
                    ShareSafeState(ShowSafeDetails(SafeDetails(activeSafe, ensName, qrCode)))
                }
            } ?: throw IllegalStateException("Safe share is only accessible with an active safe")
        }
    }

    fun isChainPrefixQrEnabled(): Boolean = settingsHandler.chainPrefixQr

    fun isChainPrefixPrependEnabled(): Boolean = settingsHandler.chainPrefixPrepend

    fun isChainPrefixCopyEnabled(): Boolean = settingsHandler.chainPrefixCopy

    fun toggleChainPrefixQr() {
        settingsHandler.chainPrefixQr = !settingsHandler.chainPrefixQr
        safeLaunch {
            (state.value?.viewAction as? ShowSafeDetails)?.safeDetails?.let {
                val qrCode = if (settingsHandler.chainPrefixQr) {
                    generateQrCode(it.safe.address, it.safe.chain.shortName)
                } else {
                    generateQrCode(it.safe.address)
                }
                updateState {
                    ShareSafeState(ShowSafeDetails(it.copy(qrCode = qrCode)))
                }
            } ?: kotlin.run {
                load()
            }
        }
    }

    private fun generateQrCode(
        safeAddress: Solidity.Address
    ): Bitmap? {
        return generateQrCode(safeAddress, null)
    }

    private fun generateQrCode(
        safeAddress: Solidity.Address,
        chainPrefix: String?
    ): Bitmap? {
        val qrContent = if (chainPrefix.isNullOrBlank()) {
            safeAddress.asEthereumAddressChecksumString()
        } else {
            "$chainPrefix:${safeAddress.asEthereumAddressChecksumString()}"
        }
        return kotlin.runCatching {
            qrCodeGenerator.generateQrCode(
                qrContent,
                512,
                512,
                Color.WHITE
            )
        }.onFailure { Timber.e(it) }
            .getOrNull()
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
