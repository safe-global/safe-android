package io.gnosis.safe.ui.settings.owner.ledger

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.utils.MnemonicAddressDerivator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import timber.log.Timber
import javax.inject.Inject

class LedgerOwnerSelectionViewModel
@Inject constructor(
    private val derivator: MnemonicAddressDerivator,
    private val credentialsRepository: CredentialsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerSelectionState>(appDispatchers) {

    private var ownerIndex: Long = 0

    override fun initialState() = OwnerSelectionState(ViewAction.Loading(true))

//    fun loadSingleOwner(privateKey: String) {
//        safeLaunch {
//            val keyPair = KeyPair.fromPrivate(privateKey.hexAsBigInteger())
//            updateState {
//                OwnerSelectionState(SingleOwner(Solidity.Address(keyPair.address.asBigInteger()), false))
//            }
//        }
//    }

    fun loadFirstDerivedOwner(mnemonic: String) {
        derivator.initialize(mnemonic)
        loadMoreOwners()
    }

    fun loadMoreOwners() {
        safeLaunch {
            LedgerOwnerPagingProvider(derivator).getOwnersStream()
                .cachedIn(viewModelScope)
                .map {
                    it.map {address ->
                        val name = credentialsRepository.owner(address)?.name
                        Timber.i("Address: ${address.asEthereumAddressChecksumString()}")
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

//    fun getOwnerData(privateKey: String? = null): Pair<String, String> {
//        val key = privateKey?.hexAsBigInteger() ?: derivator.keyForIndex(ownerIndex)
//
//        val address = if (privateKey != null) {
//            listOf(Solidity.Address(KeyPair.fromPrivate(privateKey.hexAsBigInteger()).address.asBigInteger()))[0]
//        } else {
//            derivator.addressesForPage(ownerIndex, 1)[0]
//        }
//
//        return address.asEthereumAddressString() to key.toHexString()
//    }
}

data class OwnerSelectionState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class SingleOwner(
    val owner: Solidity.Address,
    val hasMore: Boolean
) : BaseStateViewModel.ViewAction

data class DerivedOwners(
    val newOwners: PagingData<OwnerHolder>
) : BaseStateViewModel.ViewAction


