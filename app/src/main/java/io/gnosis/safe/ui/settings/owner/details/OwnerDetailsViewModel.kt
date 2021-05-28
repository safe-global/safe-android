package io.gnosis.safe.ui.settings.owner.details

import android.graphics.Bitmap
import android.graphics.Color
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import timber.log.Timber
import javax.inject.Inject

class OwnerDetailsViewModel
@Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val tracker: Tracker,
    private val qrCodeGenerator: QrCodeGenerator,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerDetailsState>(appDispatchers) {

    override fun initialState() = OwnerDetailsState(ViewAction.None)

    fun loadOwnerDetails(address: Solidity.Address) {
        safeLaunch {

            val ownerName = credentialsRepository.owner(address)!!.name

            val qrCode = runCatching {
                qrCodeGenerator.generateQrCode(
                    address.asEthereumAddressChecksumString(),
                    512,
                    512,
                    Color.WHITE
                )
            }.onFailure { Timber.e(it) }
                .getOrNull()

            updateState {
                OwnerDetailsState(ShowOwnerDetails(OwnerDetails(ownerName, qrCode)))
            }
        }
    }

    fun removeOwner(address: Solidity.Address) {
        safeLaunch {
            credentialsRepository.removeOwner(address)
            notificationRepository.unregisterOwners()
            tracker.logKeyDeleted()
            tracker.setNumKeysImported(credentialsRepository.ownerCount())
            updateState { OwnerDetailsState(ViewAction.CloseScreen) }
        }
    }
}

data class OwnerDetailsState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class OwnerDetails(
    val name: String?,
    val qrCode: Bitmap?
)

data class ShowOwnerDetails(
    val ownerDetails: OwnerDetails
) : BaseStateViewModel.ViewAction
