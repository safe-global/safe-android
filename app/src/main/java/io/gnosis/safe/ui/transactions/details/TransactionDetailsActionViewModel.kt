package io.gnosis.safe.ui.transactions.details

import io.gnosis.data.models.AddressInfoExtended
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import javax.inject.Inject

class TransactionDetailsActionViewModel @Inject constructor(
    private val safeRepository: SafeRepository,
    private val credentialsRepository: CredentialsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ActionDetailsState>(appDispatchers) {

    override fun initialState() = ActionDetailsState(null, ViewAction.Loading(true))

    fun extendAddressInfoIndexWithLocalData(addressInfoIndex: Map<String, AddressInfoExtended>?) {
        safeLaunch {
            val extendedAddressInfoIndex = mutableMapOf<String, AddressInfoExtended>()
            addressInfoIndex?.let { extendedAddressInfoIndex.putAll(it) }

            val safes = safeRepository.getSafes()
            safes.forEach {
                extendedAddressInfoIndex[it.address.asEthereumAddressChecksumString()] = AddressInfoExtended(it.address, it.localName, null)
            }

            val owners = credentialsRepository.owners()
            owners.forEach {
                extendedAddressInfoIndex[it.address.asEthereumAddressChecksumString()] = AddressInfoExtended(it.address, it.name ?: "", null)
            }

            updateState {
                ActionDetailsState(extendedAddressInfoIndex, ViewAction.Loading(false))
            }
        }
    }
}

data class ActionDetailsState(
    val addressInfoIndex: Map<String, AddressInfoExtended>?,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
