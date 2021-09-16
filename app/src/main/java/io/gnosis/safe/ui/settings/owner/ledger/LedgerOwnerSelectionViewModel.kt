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
import pm.gnosis.model.Solidity
import javax.inject.Inject

class LedgerOwnerSelectionViewModel
@Inject constructor(
    private val ledgerController: LedgerController,
    private val credentialsRepository: CredentialsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerSelectionState>(appDispatchers) {

    private var ownerIndex: Long = 0
    private var derivationPath: String = ""

    override fun initialState() = OwnerSelectionState(ViewAction.Loading(true))

    fun loadOwners(derivationPath: String) {
        safeLaunch {
            LedgerOwnerPagingProvider(ledgerController, derivationPath).getOwnersStream()
                .cachedIn(viewModelScope)
                .map {
                    it.map { address ->
                        val name = credentialsRepository.owner(address)?.name
                        OwnerHolder(address, name, name != null)
                    }
                }
                .collectLatest {
                    updateState { OwnerSelectionState(DerivedOwners(it, derivationPath)) }
                }
        }
    }

    fun setOwnerIndex(index: Long, address: Solidity.Address) {
        ownerIndex = index
        safeLaunch {
            updateState {
                OwnerSelectionState(
                    OwnerSelected(
                        selectedOwner = OwnerHolder(
                            address = address,
                            name = null,
                            disabled = false
                        ), derivationPath = derivationPath
                    )
                )
            }
        }
    }
}

data class OwnerSelectionState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class DerivedOwners(
    val newOwners: PagingData<OwnerHolder>,
    val derivationPath: String
) : BaseStateViewModel.ViewAction

data class OwnerSelected(
    val selectedOwner: OwnerHolder,
    val derivationPath: String
) : BaseStateViewModel.ViewAction
