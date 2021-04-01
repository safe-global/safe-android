package io.gnosis.safe.ui.settings.owner.selection

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.utils.MnemonicKeyAndAddressDerivator
import kotlinx.coroutines.flow.collectLatest
import pm.gnosis.crypto.KeyPair
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.toHexString
import javax.inject.Inject

class OwnerSelectionViewModel
@Inject constructor(
    private val derivator: MnemonicKeyAndAddressDerivator,
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
            DerivedOwnerPagingProvider(derivator).getOwnersStream()
                .cachedIn(viewModelScope)
                .collectLatest {
                    updateState { OwnerSelectionState(DerivedOwners(it)) }
                }
        }
    }

    fun setOwnerIndex(index: Long) {
        ownerIndex = index
    }

    fun getOwnerData(privateKey: String? = null): Pair<String, String> {
        val key = privateKey?.hexAsBigInteger() ?: derivator.keyForIndex(ownerIndex)

        val address = if (privateKey != null) {
            listOf(Solidity.Address(KeyPair.fromPrivate(privateKey.hexAsBigInteger()).address.asBigInteger()))[0]
        } else {
            derivator.addressesForPage(ownerIndex, 1)[0]
        }

        return address.asEthereumAddressString() to key.toHexString()
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


