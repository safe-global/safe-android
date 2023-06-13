package io.gnosis.safe.ui.dialogs

import androidx.lifecycle.ViewModel
import io.gnosis.data.models.Chain
import io.gnosis.data.repositories.UnstoppableDomainsRepository
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.model.Solidity
import javax.inject.Inject

class UnstoppableInputViewModel
@Inject constructor(
    private val unstoppableRepository: UnstoppableDomainsRepository,
    private val settingsHandler: SettingsHandler
) : ViewModel() {

    suspend fun processInput(input: CharSequence, chain: Chain): Solidity.Address {
        return kotlin.runCatching {
            unstoppableRepository.resolve(input.toString(), chain.chainId)
        }
            .onSuccess {
                it
            }
            .onFailure {
                throw it
            }
            .getOrNull()!!
    }

    fun isChainPrefixPrependEnabled() = settingsHandler.chainPrefixPrepend
}
