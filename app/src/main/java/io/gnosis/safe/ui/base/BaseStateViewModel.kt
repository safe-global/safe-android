package io.gnosis.safe.ui.base

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class BaseStateViewModel<T> : ViewModel() where T : BaseStateViewModel.State {
    abstract val state: LiveData<T>

//    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    protected abstract fun initialState(): T

    interface State {
        var viewAction: ViewAction?
    }

    interface ViewAction {
        data class Loading(val isLoading: Boolean) : ViewAction
        data class ShowError(val error: Throwable) : ViewAction
        data class StartActivity(val intent: Intent) : ViewAction
        data class NavigateTo(val navDirections: NavDirections) : ViewAction
        object CloseScreen : ViewAction
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
        viewModelScope.launch(Dispatchers.IO + errorHandler, block = block)
}
