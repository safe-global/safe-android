package io.gnosis.safe.ui.safe.share

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import timber.log.Timber
import javax.inject.Inject

class ShareSafeViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val ensRepository: EnsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ShareSafeState>(appDispatchers) {

    override fun initialState(): ShareSafeState = ShareSafeState()

    fun load() {
        safeLaunch {
            safeRepository.getActiveSafe()?.let { activeSafe ->
                val ensName = runCatching { ensRepository.reverseResolve(activeSafe.address) }
                    .onFailure { Timber.e(it) }
                    .getOrNull()
                updateState {
                    ShareSafeState(ShowSafeDetails(SafeDetails(activeSafe, ensName)))
                }
            }
        }
    }
}

data class ShareSafeState(
    override var viewAction: BaseStateViewModel.ViewAction? = BaseStateViewModel.ViewAction.Loading(true)
) : BaseStateViewModel.State

data class SafeDetails(
    val safe: Safe,
    val ensName: String?
)

data class ShowSafeDetails(
    val safeDetails: SafeDetails
) : BaseStateViewModel.ViewAction
