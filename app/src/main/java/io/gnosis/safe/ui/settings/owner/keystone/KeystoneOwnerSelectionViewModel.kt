package io.gnosis.safe.ui.settings.owner.keystone

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.keystone.module.Account
import com.keystone.module.MultiAccounts
import com.keystone.module.Note
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.owner.selection.DerivedOwnerPagingProvider
import io.gnosis.safe.ui.settings.owner.selection.DerivedOwners
import io.gnosis.safe.ui.settings.owner.selection.OwnerHolder
import io.gnosis.safe.ui.settings.owner.selection.OwnerSelectionState
import io.gnosis.safe.utils.PublicKeyAndAddressDerivator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import pm.gnosis.crypto.KeyPair
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexStringToByteArray
import javax.inject.Inject

class KeystoneOwnerSelectionViewModel
@Inject constructor(
    private val derivator: PublicKeyAndAddressDerivator,
    private val credentialsRepository: CredentialsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerSelectionState>(appDispatchers) {

    private var ownerIndex: Long = 0
    private var multiHDKeys: MultiAccounts? = null
    private var hdKey: Account? = null

    override fun initialState() = OwnerSelectionState(ViewAction.Loading(true))

    fun loadFirstDerivedOwner(hdKey: Account) {
        this.hdKey = hdKey
        derivator.initialize(hdKey)
        loadMoreOwners()
    }

    fun loadFirstDerivedOwner(multiHDKeys: MultiAccounts) {
        this.multiHDKeys = multiHDKeys
        loadAllOwners(multiHDKeys)
    }

    private fun loadMoreOwners() {
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

    private fun loadAllOwners(multiHDKeys: MultiAccounts) {
        safeLaunch {
            KeystoneOwnerPagingProvider(multiHDKeys).getOwnersStream()
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

    fun getOwnerData(): Triple<String, String, String> {
        multiHDKeys?.let {
            val hdKey = it.keys[ownerIndex.toInt()]
            return Triple(hdKey.toAddress().asEthereumAddressString(), derivationPath(ownerIndex), hdKey.xfp)
        } ?: run {
            val address = derivator.addressesForPage(ownerIndex, 1)[0]
            return Triple(address.asEthereumAddressString(), derivationPath(ownerIndex), hdKey!!.xfp)
        }
    }

    private fun derivationPath(index: Long): String {
        hdKey?.let {
            val path = if (it.note == Note.LEDGER_LEGACY.value) "$index" else "0/$index"
            return "m/44'/60'/0'/$path"
        } ?: run {
            return "m/44'/60'/$index'/0/0"
        }
    }
}

fun Account.toAddress(): Solidity.Address {
    val keyPair = KeyPair.fromPublicOnly(this.publicKey.hexStringToByteArray())
    return Solidity.Address(keyPair.address.asBigInteger())
}
