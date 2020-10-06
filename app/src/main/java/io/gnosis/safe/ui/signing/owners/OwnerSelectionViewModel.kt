package io.gnosis.safe.ui.signing.owners

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.utils.MnemonicKeyAndAddressDerivator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import pm.gnosis.model.Solidity
import java.math.BigInteger
import javax.inject.Inject

class OwnerSelectionViewModel
@Inject constructor(
    private val derivator: MnemonicKeyAndAddressDerivator,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerSelectionState>(appDispatchers) {

    private var ownerIndex: Long = 1

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
            //TODO: store key
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

