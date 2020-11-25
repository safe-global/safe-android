package io.gnosis.safe.ui.settings.owner.list

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import io.gnosis.safe.Tracker
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.utils.MnemonicKeyAndAddressDerivator
import io.gnosis.safe.utils.OwnerCredentials
import io.gnosis.safe.utils.OwnerCredentialsRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import pm.gnosis.model.Solidity
import java.math.BigInteger
import javax.inject.Inject

class OwnerSelectionViewModel
@Inject constructor(
    private val derivator: MnemonicKeyAndAddressDerivator,
    private val ownerCredentialsVault: OwnerCredentialsRepository,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerSelectionState>(appDispatchers) {

    private var ownerIndex: Long = 0

    override fun initialState() = OwnerSelectionState(ViewAction.Loading(true))

    fun loadOwners(mnemonic: String) {
        safeLaunch {
            derivator.initialize(mnemonic)
            OwnerPagingProvider(derivator).getOwnersStream()
                .map {
                    it.insertSeparators { before, after ->
                        return@insertSeparators if (before == null) {
                            Solidity.Address(BigInteger.ZERO)
                        } else {
                            null
                        }
                    }
                }
                .cachedIn(viewModelScope)
                .collectLatest {
                    updateState {
                        OwnerSelectionState(
                            LoadedOwners(
                                it
                            )
                        )
                    }
                }
        }
    }

    fun setOwnerIndex(index: Long) {
        ownerIndex = index
    }

    fun importOwner() {
        safeLaunch {
            val key = derivator.keyForIndex(ownerIndex)
            val addresses = derivator.addressesForPage(ownerIndex, 1)
            ownerCredentialsVault.storeCredentials(OwnerCredentials(address = addresses[0], key = key))
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


data class LoadedOwners(
    val newOwners: PagingData<Solidity.Address>
) : BaseStateViewModel.ViewAction

