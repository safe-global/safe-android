package io.gnosis.safe.ui.transactions.execution

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import javax.inject.Inject

class TxReviewViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val settingsHandler: SettingsHandler,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TxReviewState>(appDispatchers) {

    lateinit var activeSafe: Safe
        private set

    init {
        safeLaunch {
            activeSafe = safeRepository.getActiveSafe()!!
        }
    }

    override fun initialState() = TxReviewState(viewAction = null)

    fun isChainPrefixPrependEnabled() = settingsHandler.chainPrefixPrepend

    fun isChainPrefixCopyEnabled() = settingsHandler.chainPrefixCopy
}

data class TxReviewState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
