package io.gnosis.safe.ui.safe

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import io.gnosis.data.models.Safe
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class SafeOverviewViewModel @Inject constructor(
    repositories: Repositories
) : BaseStateViewModel<SafeOverviewState>() {

    private val safeRepository = repositories.safeRepository()

    override val state: LiveData<SafeOverviewState> = liveData {
        for (event in stateChannel.openSubscription())
            emit(event)
    }

    override fun initialState(): SafeOverviewState =  SafeOverviewState.ActiveSafe(null, null)

    fun loadSafe() {
        safeLaunch {
            val activeSafe = safeRepository.getActiveSafe()
            updateState { SafeOverviewState.ActiveSafe(activeSafe, null) }
        }
    }
}

sealed class SafeOverviewState : BaseStateViewModel.State {

    data class ActiveSafe(
        val safe: Safe?,
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeOverviewState()
}
