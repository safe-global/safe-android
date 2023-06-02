package io.gnosis.safe.ui.transactions.details

import io.gnosis.data.models.AddressInfo
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import javax.inject.Inject

class TransactionDetailsActionViewModel @Inject constructor(
    private val safeRepository: SafeRepository,
    private val credentialsRepository: CredentialsRepository,
    private val settingsHandler: SettingsHandler,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ActionDetailsState>(appDispatchers) {

    override fun initialState() = ActionDetailsState(null, ViewAction.Loading(true))

    fun extendAddressInfoIndexWithLocalData(addressInfoIndex: Map<String, AddressInfo>?) {
        safeLaunch {
            val extendedAddressInfoIndex = mutableMapOf<String, AddressInfo>()
            addressInfoIndex?.let { extendedAddressInfoIndex.putAll(it) }

            val safes = safeRepository.getSafes()
            safes.forEach {
                extendedAddressInfoIndex[it.address.asEthereumAddressChecksumString()] = AddressInfo(it.address, it.localName, null)
            }

            val owners = credentialsRepository.owners()
            owners.forEach {
                extendedAddressInfoIndex[it.address.asEthereumAddressChecksumString()] = AddressInfo(it.address, it.name ?: "", null)
            }

            updateState {
                ActionDetailsState(extendedAddressInfoIndex, ViewAction.Loading(false))
            }
        }
    }

    fun isChainPrefixPrependEnabled() = settingsHandler.chainPrefixPrepend

    fun isChainPrefixCopyEnabled() = settingsHandler.chainPrefixCopy
}

data class ActionDetailsState(
    val addressInfoIndex: Map<String, AddressInfo>?,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
