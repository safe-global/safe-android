package io.gnosis.safe.ui.safe.send_funds

import io.gnosis.data.models.Chain
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.UnstoppableDomainsRepository
import io.gnosis.safe.ui.assets.SafeBalancesState
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collect
import pm.gnosis.utils.asEthereumAddress
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

class SendAssetViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val unstoppableDomainsRepository: UnstoppableDomainsRepository,
    private val ensRepository: EnsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SendAssetState>(appDispatchers) {

    var activeSafe: Safe? = null
        private set

    override fun initialState(): SendAssetState =
        SendAssetState(viewAction = null)

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState {
                    activeSafe = safe
                    SendAssetState(viewAction = ViewAction.UpdateActiveSafe(activeSafe))
                }
            }
        }
    }

    fun validateInputs(recipientInput: String?, amountInput: BigDecimal?): Boolean {
        val address = recipientInput?.asEthereumAddress()
        return address != null && amountInput != null
    }

    fun enableUD(chain: Chain): Boolean {
        return unstoppableDomainsRepository.canResolve(chain)
    }

    fun enableENS(chain: Chain): Boolean {
        return ensRepository.canResolve(chain)
    }
}

data class SendAssetState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
