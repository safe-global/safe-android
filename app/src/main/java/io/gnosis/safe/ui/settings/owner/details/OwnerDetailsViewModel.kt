package io.gnosis.safe.ui.settings.owner.details

import android.graphics.Bitmap
import android.graphics.Color
import io.gnosis.data.models.Owner
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.utils.toHexString
import timber.log.Timber
import javax.inject.Inject

class OwnerDetailsViewModel
@Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker,
    private val qrCodeGenerator: QrCodeGenerator,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerDetailsState>(appDispatchers) {

    private lateinit var owner: Owner

    override fun initialState() = OwnerDetailsState(ViewAction.None)

    fun loadOwnerDetails(address: Solidity.Address) {
        safeLaunch {

            owner = credentialsRepository.owner(address)!!

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
                OwnerDetailsState(ShowOwnerDetails(OwnerDetails(owner.name, qrCode, owner.privateKey != null, owner.type)))
            }
        }
    }

    fun startExportFlow() {
        safeLaunch {
            if (settingsHandler.usePasscode && settingsHandler.requirePasscodeToExportKeys) {
                updateState {
                    OwnerDetailsState(ViewAction.NavigateTo(OwnerDetailsFragmentDirections.actionOwnerDetailsFragmentToEnterPasscodeFragment()))
                }
                updateState {
                    OwnerDetailsState(ViewAction.None)
                }

            } else {
                showExportData()
            }
        }
    }

    fun showExportData() {
        safeLaunch {
            val seed = owner.seedPhrase?.let {
                credentialsRepository.decryptSeed(it)
            }
            val key = credentialsRepository.decryptKey(owner.privateKey!!).toHexString()
            updateState {
                OwnerDetailsState(ViewAction.NavigateTo(OwnerDetailsFragmentDirections.actionOwnerDetailsFragmentToOwnerExportFragment(key, seed)))
            }
            updateState {
                OwnerDetailsState(ViewAction.None)
            }
        }
    }

    fun removeOwner(address: Solidity.Address) {
        safeLaunch {
            val owner = credentialsRepository.owner(address)!!
            credentialsRepository.removeOwner(owner)
            notificationRepository.unregisterOwners()
            tracker.logKeyDeleted()
            when(owner.type) {
                Owner.Type.IMPORTED -> tracker.setNumKeysImported(credentialsRepository.ownerCount(owner.type))
                Owner.Type.GENERATED -> tracker.setNumKeysGenerated(credentialsRepository.ownerCount(owner.type))
                Owner.Type.LEDGER_NANO_X -> tracker.setNumKeysLedger(credentialsRepository.ownerCount(owner.type))
            }
            updateState { OwnerDetailsState(ViewAction.CloseScreen) }
        }
    }
}

data class OwnerDetailsState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class OwnerDetails(
    val name: String?,
    val qrCode: Bitmap?,
    val exportable: Boolean,
    val ownerType: Owner.Type
)

data class ShowOwnerDetails(
    val ownerDetails: OwnerDetails
) : BaseStateViewModel.ViewAction
