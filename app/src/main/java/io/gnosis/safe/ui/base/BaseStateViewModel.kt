package io.gnosis.safe.ui.base

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import io.gnosis.data.models.Safe
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import timber.log.Timber

data class AppDispatchers(
    val main: CoroutineDispatcher = Dispatchers.Main,
    val background: CoroutineDispatcher = Dispatchers.IO
)

abstract class BaseStateViewModel<T>(private val dispatchers: AppDispatchers) :
    ViewModel() where T : BaseStateViewModel.State {

    val state: LiveData<T> = liveData {
        onStateSubscribed()
        for (event in stateChannel.openSubscription())
            emit(event)
    }

    open fun onStateSubscribed() {
    }

    protected abstract fun initialState(): T

    interface State {
        var viewAction: ViewAction?
    }

    interface ViewAction {
        data class Loading(val isLoading: Boolean) : ViewAction
        data class ShowError(val error: Throwable) : ViewAction
        data class UpdateActiveSafe(val newSafe: Safe?) : ViewAction
        data class StartActivity(val intent: Intent) : ViewAction
        data class NavigateTo(val navDirections: NavDirections) : ViewAction
        object ShowEmptyState : ViewAction
        object CloseScreen : ViewAction
        object None : ViewAction
    }

    @Suppress("LeakingThis")
    protected val stateChannel = ConflatedBroadcastChannel(initialState())

    protected val coroutineErrorHandler = CoroutineExceptionHandler { _, e ->
        Timber.e(e)
        viewModelScope.launch { updateState(true) { viewAction = ViewAction.ShowError(e); this } }
    }

    protected fun currentState(): T = stateChannel.value

    protected suspend fun updateState(forceViewAction: Boolean = false, update: T.() -> T) {
        try {
            val currentState = currentState()
            val nextState = currentState.run(update)
            // Reset view action if the same
            if (!forceViewAction && nextState.viewAction === currentState.viewAction) nextState.viewAction = null
            stateChannel.send(nextState)
        } catch (e: Exception) {
            // Could not submit update
            Timber.e(e)
        }
    }

    protected fun safeLaunch(errorHandler: CoroutineExceptionHandler = coroutineErrorHandler, block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(dispatchers.background + errorHandler, block = block)
}
