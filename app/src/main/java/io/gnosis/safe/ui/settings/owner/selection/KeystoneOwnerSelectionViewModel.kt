package io.gnosis.safe.ui.settings.owner.selection

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.keystone.module.HDKey
import com.keystone.module.MultiHDKeys
import com.keystone.module.Note
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
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
    private var multiHDKeys: MultiHDKeys? = null
    private var hdKey: HDKey? = null

    override fun initialState() = OwnerSelectionState(ViewAction.Loading(true))

    fun loadFirstDerivedOwner(hdKey: HDKey) {
        this.hdKey = hdKey
        derivator.initialize(hdKey)
        loadMoreOwners()
    }

    fun loadFirstDerivedOwner(multiHDKeys: MultiHDKeys) {
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

    private fun loadAllOwners(multiHDKeys: MultiHDKeys) {
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
            val hdKey = it.hdKeys[ownerIndex.toInt()]
            return Triple(hdKey.toAddress().asEthereumAddressString(), derivationPath(ownerIndex), hdKey.sourceFingerprint)
        } ?: run {
            val address = derivator.addressesForPage(ownerIndex, 1)[0]
            return Triple(address.asEthereumAddressString(), derivationPath(ownerIndex), hdKey!!.sourceFingerprint)
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

fun HDKey.toAddress(): Solidity.Address {
    val keyPair = KeyPair.fromPublicOnly(this.key.hexStringToByteArray())
    return Solidity.Address(keyPair.address.asBigInteger())
}