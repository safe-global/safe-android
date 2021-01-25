package io.gnosis.safe.ui.settings.owner.list

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.utils.MnemonicKeyAndAddressDerivator
import io.gnosis.safe.utils.OwnerCredentials
import io.gnosis.safe.utils.OwnerCredentialsRepository
import kotlinx.coroutines.flow.collectLatest
import pm.gnosis.crypto.KeyPair
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.toHexString
import timber.log.Timber
import javax.inject.Inject

class OwnerSelectionViewModel
@Inject constructor(
    private val derivator: MnemonicKeyAndAddressDerivator,
    private val ownerCredentialsVault: OwnerCredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerSelectionState>(appDispatchers) {

    private var ownerIndex: Long = 0

    override fun initialState() = OwnerSelectionState(ViewAction.Loading(true))

    fun loadOwner(privateKey: String) {
        safeLaunch {
            val keyPair = KeyPair.fromPrivate(privateKey.hexAsBigInteger())

            Timber.i("----> address: ${keyPair.address.toHexString()}")
            updateState {
                //OwnerSelectionState(LoadedOwners(PagingData.from(listOf(Solidity.Address(keyPair.address.asBigInteger())))))
                OwnerSelectionState(FirstOwner(Solidity.Address(keyPair.address.asBigInteger()), false))
            }
        }
    }

    fun loadOwners(mnemonic: String) {
        derivator.initialize(mnemonic)

//        derivator.addressesForRange(LongRange(0, 0)).forEach {
//            Timber.i("----> $it")
//        }

        //TODO: Show only first owner
        safeLaunch {

            updateState {
//                OwnerSelectionState(LoadedOwners(PagingData.from(derivator.addressesForRange(LongRange(0, 0)))))
                OwnerSelectionState(FirstOwner(derivator.addressesForRange(LongRange(0, 0))[0], true))
            }
        }

//        //TODO Do this only after button clicked
//        safeLaunch {
//            derivator.initialize(mnemonic)
//            OwnerPagingProvider(derivator).getOwnersStream()
//                .cachedIn(viewModelScope)
//                .collectLatest {
//                    updateState { OwnerSelectionState(LoadedOwners(it)) }
//                }
//        }
    }

    fun loadMoreOwners() {
        safeLaunch {
            OwnerPagingProvider(derivator).getOwnersStream()
                .cachedIn(viewModelScope)
                .collectLatest {
                    updateState { OwnerSelectionState(LoadedOwners(it)) }
                }
        }
    }

    fun setOwnerIndex(index: Long) {
        ownerIndex = index
    }

    fun importOwner(privateKey: String? = null) {
        //TODO Distinguish between secret key load and seed load
        safeLaunch {

            val key = privateKey?.hexAsBigInteger() ?: derivator.keyForIndex(ownerIndex)

            val addresses = if (privateKey != null) {
                listOf(Solidity.Address(KeyPair.fromPrivate(privateKey.hexAsBigInteger()).address.asBigInteger()))
            } else {
                derivator.addressesForPage(ownerIndex, 1)
            }
            OwnerCredentials(address = addresses[0], key = key).let {
                ownerCredentialsVault.storeCredentials(it)
                notificationRepository.registerOwner(it.key)
            }
            settingsHandler.showOwnerBanner = false
            tracker.logKeyImported()
            tracker.setNumKeysImported(1)
            updateState {
                OwnerSelectionState(ViewAction.CloseScreen)
            }
        }
    }
}

data class OwnerSelectionState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class FirstOwner(
    val owner: Solidity.Address,
    val hasMore: Boolean
) : BaseStateViewModel.ViewAction

data class LoadedOwners(
    val newOwners: PagingData<Solidity.Address>
) : BaseStateViewModel.ViewAction

