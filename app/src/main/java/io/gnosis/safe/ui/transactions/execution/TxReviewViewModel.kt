package io.gnosis.safe.ui.transactions.execution

import androidx.annotation.VisibleForTesting
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.owner.list.OwnerViewData
import javax.inject.Inject

class TxReviewViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val credentialsRepository: CredentialsRepository,
    private val settingsHandler: SettingsHandler,
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

    @VisibleForTesting
    fun loadDefaultKey() {
        safeLaunch {
            val owners = credentialsRepository.owners().map { OwnerViewData(it.address, it.name, it.type) }.sortedBy { it.name }
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
    val key: OwnerViewData?
) : BaseStateViewModel.ViewAction

