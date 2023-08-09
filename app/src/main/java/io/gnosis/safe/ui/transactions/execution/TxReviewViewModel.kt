package io.gnosis.safe.ui.transactions.execution

import androidx.annotation.VisibleForTesting
import io.gnosis.data.backend.rpc.RpcClient
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.owner.list.OwnerViewData
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import javax.inject.Inject

class TxReviewViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val credentialsRepository: CredentialsRepository,
    private val settingsHandler: SettingsHandler,
    private val rpcClient: RpcClient,
    private val balanceFormatter: BalanceFormatter,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TxReviewState>(appDispatchers) {

    lateinit var activeSafe: Safe
        private set

    private var executionKey: OwnerViewData? = null

    init {
        safeLaunch {
            activeSafe = safeRepository.getActiveSafe()!!
            loadDefaultKey()
        }
    }

    override fun initialState() = TxReviewState(viewAction = null)

    fun isLoading(): Boolean {
        val viewAction = (state.value as TxReviewState).viewAction
        return (viewAction is ViewAction.Loading && viewAction.isLoading)
    }

    @VisibleForTesting
    fun loadDefaultKey() {
        safeLaunch {
            val owners = credentialsRepository.owners()
                .map { OwnerViewData(it.address, it.name, it.type) }
                .sortedBy { it.name }
            activeSafe.signingOwners?.let {
                val acceptedOwners = owners.filter { localOwner ->
                    activeSafe.signingOwners.any {
                        localOwner.address == it
                    }
                }
                executionKey = acceptedOwners.first()
                updateState {
                    TxReviewState(viewAction = DefaultKey(executionKey))
                }
                executionKey?.let {
                    val balanceWei = rpcClient.getBalance(it.address)
                    balanceWei?.let {
                        updateState {
                            TxReviewState(
                                viewAction = DefaultKey(
                                    executionKey,
                                    "${
                                        balanceFormatter.shortAmount(
                                            it.value.convertAmount(
                                                activeSafe.chain.currency.decimals
                                            )
                                        )
                                    } ${activeSafe.chain.currency.symbol}",
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun isChainPrefixPrependEnabled() = settingsHandler.chainPrefixPrepend

    fun isChainPrefixCopyEnabled() = settingsHandler.chainPrefixCopy
}

data class TxReviewState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class DefaultKey(
    val key: OwnerViewData?,
    val balance: String? = null
) : BaseStateViewModel.ViewAction

