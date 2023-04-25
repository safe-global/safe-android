package io.gnosis.safe.ui.settings.owner.list

import io.gnosis.data.models.Chain
import io.gnosis.data.models.Owner
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.owner.ledger.LedgerDeviceListFragment
import io.gnosis.safe.ui.transactions.details.ConfirmConfirmation
import io.gnosis.safe.ui.transactions.details.ConfirmRejection
import io.gnosis.safe.ui.transactions.details.SigningMode
import io.gnosis.safe.ui.transactions.details.SigningOwnerSelectionFragmentDirections
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class OwnerListViewModel
@Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val settingsHandler: SettingsHandler,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerListState>(appDispatchers) {

    override fun initialState() = OwnerListState(ViewAction.Loading(true))

    fun loadOwners(missingSigners: List<String>? = null) {
        safeLaunch {
            updateState {
                OwnerListState(viewAction = ViewAction.Loading(true))
            }
            val owners = credentialsRepository.owners().map { OwnerViewData(it.address, it.name, it.type) }.sortedBy { it.name }
            missingSigners?.let {
                val acceptedOwners = owners.filter { localOwner ->
                    missingSigners.any {
                        localOwner.address.asEthereumAddressString() == it
                    }
                }
                updateState {
                    OwnerListState(viewAction = LocalOwners(acceptedOwners))
                }
            } ?: updateState {
                OwnerListState(viewAction = LocalOwners(owners))
            }

        }
    }

    fun selectKeyForSigning(
        owner: Solidity.Address,
        type: Owner.Type,
        signingMode: SigningMode,
        chain: Chain,
        safeTxHash: String? = null
    ) {
        val isConfirmation = signingMode == SigningMode.CONFIRMATION || signingMode == SigningMode.INITIATE_TRANSFER
        safeLaunch {
            when(type) {
                Owner.Type.LEDGER_NANO_X -> {
                    updateState {
                        OwnerListState(
                            ViewAction.NavigateTo(
                                SigningOwnerSelectionFragmentDirections.actionSigningOwnerSelectionFragmentToLedgerDeviceListFragmet(
                                    if (isConfirmation) LedgerDeviceListFragment.Mode.CONFIRMATION.name else LedgerDeviceListFragment.Mode.REJECTION.name,
                                    owner.asEthereumAddressString(),
                                    safeTxHash
                                )
                            )
                        )
                    }
                }
                Owner.Type.KEYSTONE -> {
                    updateState {
                        OwnerListState(
                            ViewAction.NavigateTo(
                                SigningOwnerSelectionFragmentDirections.actionSigningOwnerSelectionFragmentToKeystoneRequestSignatureFragment(
                                    owner.asEthereumAddressString(),
                                    chain,
                                    safeTxHash
                                )
                            )
                        )
                    }
                }
                else -> {
                    if (settingsHandler.usePasscode && settingsHandler.requirePasscodeForConfirmations) {
                        updateState {
                            OwnerListState(
                                ViewAction.NavigateTo(
                                    SigningOwnerSelectionFragmentDirections.actionSigningOwnerSelectionFragmentToEnterPasscodeFragment(selectedOwner = owner.asEthereumAddressString())
                                )
                            )
                        }
                        updateState { OwnerListState(ViewAction.None) }
                    } else {
                        if (isConfirmation) {
                            updateState { OwnerListState(ConfirmConfirmation(owner)) }
                            updateState { OwnerListState(ViewAction.None) }
                        } else {
                            updateState { OwnerListState(ConfirmRejection(owner)) }
                            updateState { OwnerListState(ViewAction.None) }
                        }
                    }
                }
            }
        }
    }
}

data class OwnerListState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class LocalOwners(
    val owners: List<OwnerViewData>
) : BaseStateViewModel.ViewAction
