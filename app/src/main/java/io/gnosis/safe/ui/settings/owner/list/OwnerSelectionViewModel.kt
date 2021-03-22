package io.gnosis.safe.ui.settings.owner.list

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.utils.MnemonicKeyAndAddressDerivator
import kotlinx.coroutines.flow.collectLatest
import pm.gnosis.crypto.KeyPair
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.hexAsBigInteger
import javax.inject.Inject

class OwnerSelectionViewModel
@Inject constructor(
    private val derivator: MnemonicKeyAndAddressDerivator,
    private val credentialsRepository: CredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerSelectionState>(appDispatchers) {

    private var ownerIndex: Long = 0

    override fun initialState() = OwnerSelectionState(ViewAction.Loading(true))

    fun loadSingleOwner(privateKey: String) {
        safeLaunch {
            val keyPair = KeyPair.fromPrivate(privateKey.hexAsBigInteger())
            updateState {
                OwnerSelectionState(SingleOwner(Solidity.Address(keyPair.address.asBigInteger()), false))
            }
        }
    }

    fun loadFirstDerivedOwner(mnemonic: String) {
        derivator.initialize(mnemonic)
        loadMoreOwners()
    }

    fun loadMoreOwners() {
        safeLaunch {
            OwnerPagingProvider(derivator).getOwnersStream()
                .cachedIn(viewModelScope)
                .collectLatest {
                    updateState { OwnerSelectionState(DerivedOwners(it)) }
                }
        }
    }

    fun setOwnerIndex(index: Long) {
        ownerIndex = index
    }

    fun importOwner(privateKey: String? = null) {
        safeLaunch {

            val key = privateKey?.hexAsBigInteger() ?: derivator.keyForIndex(ownerIndex)

            val addresses = if (privateKey != null) {
                listOf(Solidity.Address(KeyPair.fromPrivate(privateKey.hexAsBigInteger()).address.asBigInteger()))
            } else {
                derivator.addressesForPage(ownerIndex, 1)
            }

            credentialsRepository.saveOwner(addresses[0], key)

            settingsHandler.showOwnerBanner = false
            settingsHandler.showOwnerScreen = false
            tracker.logKeyImported(privateKey == null)
            tracker.setNumKeysImported(1)

            notificationRepository.registerOwner(key)

            updateState {
                OwnerSelectionState(ViewAction.CloseScreen)
            }
        }
    }
}

data class OwnerSelectionState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class SingleOwner(
    val owner: Solidity.Address,
    val hasMore: Boolean
) : BaseStateViewModel.ViewAction

data class DerivedOwners(
    val newOwners: PagingData<Solidity.Address>
) : BaseStateViewModel.ViewAction

