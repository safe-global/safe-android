package io.gnosis.safe.ui.settings.owner.ledger

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import timber.log.Timber
import javax.inject.Inject

class LedgerOwnerSelectionViewModel
@Inject constructor(
    private val addressProvider: LedgerAddressProvider,
    private val credentialsRepository: CredentialsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerSelectionState>(appDispatchers) {

    init {
        Timber.i("----> LedgerOwnerSelectionViewModel.init() -> $this")
    }

    private var ownerIndex: Long = 0

    override fun initialState() = OwnerSelectionState(ViewAction.Loading(true))

    fun loadFirstDerivedOwner(derivationPath: String) {
        loadMoreOwners(derivationPath)
    }

    private fun loadMoreOwners(derivationPath: String) {
        safeLaunch {
            LedgerOwnerPagingProvider(addressProvider, derivationPath).getOwnersStream()
                .cachedIn(viewModelScope)
                .map {
                    it.map { address ->
                        val name = credentialsRepository.owner(address)?.name
                        Timber.i("Address: ${address.asEthereumAddressChecksumString()}")
                        OwnerHolder(address, name, name != null)
                    }
                }
                .collectLatest {
                    Timber.i("LedgerOwnerSelectionViewModel ----> updateState... DerivedOwners")

                    updateState { OwnerSelectionState(DerivedOwners(it)) }
                }
        }
    }

    fun setOwnerIndex(index: Long) {
        ownerIndex = index
        safeLaunch {
            Timber.i("LedgerOwnerSelectionViewModel ----> updateState... EnableNextButton")
            updateState { OwnerSelectionState(EnableNextButton) }
        }
    }
}

data class OwnerSelectionState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class DerivedOwners(
    val newOwners: PagingData<OwnerHolder>
) : BaseStateViewModel.ViewAction

object EnableNextButton : BaseStateViewModel.ViewAction
