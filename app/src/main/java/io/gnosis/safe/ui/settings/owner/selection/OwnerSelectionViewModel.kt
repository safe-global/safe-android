package io.gnosis.safe.ui.settings.owner.selection

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.utils.MnemonicKeyAndAddressDerivator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.toHexString
import javax.inject.Inject

class OwnerSelectionViewModel
@Inject constructor(
    private val derivator: MnemonicKeyAndAddressDerivator,
    private val credentialsRepository: CredentialsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerSelectionState>(appDispatchers) {

    private var ownerIndex: Long = 0

    override fun initialState() = OwnerSelectionState(ViewAction.Loading(true))

    fun loadFirstDerivedOwner(mnemonic: String) {
        derivator.initialize(mnemonic)
        loadMoreOwners()
    }

    fun loadMoreOwners() {
        safeLaunch {
            DerivedOwnerPagingProvider(derivator).getOwnersStream()
                .cachedIn(viewModelScope)
                .map {
                    it.map {address ->
                        val name = credentialsRepository.owner(address)?.name
                        OwnerHolder(address, name, name != null)
                    }
                }
                .collectLatest {
                    updateState { OwnerSelectionState(DerivedOwners(it)) }
                }
        }
    }

    fun setOwnerIndex(index: Long) {
        ownerIndex = index
    }

    fun getOwnerData(): Pair<String, String> {
        val key = derivator.keyForIndex(ownerIndex)
        val address = derivator.addressesForPage(ownerIndex, 1)[0]
        return address.asEthereumAddressString() to key.toHexString()
    }
}

data class OwnerSelectionState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class DerivedOwners(
    val newOwners: PagingData<OwnerHolder>
) : BaseStateViewModel.ViewAction


