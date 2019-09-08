package pm.gnosis.heimdall.ui.base

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import timber.log.Timber

@ExperimentalCoroutinesApi
abstract class BaseStateViewModel<T : BaseStateViewModel.State>(context: Context) : ViewModel() {
    abstract val state: LiveData<T>

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    protected abstract fun initialState(): T

    interface State {
        var viewAction: ViewAction?
    }

    sealed class ViewAction {
        data class ShowError(val error: Throwable) : ViewAction()
        data class StartActivity(val intent: Intent) : ViewAction()
    }

    @Suppress("LeakingThis")
    protected val stateChannel = ConflatedBroadcastChannel(initialState())

    protected val coroutineErrorHandler = CoroutineExceptionHandler { _, e ->
        Timber.e(e)
        viewModelScope.launch { updateState { viewAction = ViewAction.ShowError(errorHandler.translate(e)); this } }
    }

    protected fun currentState(): T = stateChannel.value

    protected suspend fun updateState(update: T.() -> T) {
        try {
            val currentState = currentState()
            val nextState = currentState.run(update)
            // Reset view action if the same
            if (nextState.viewAction === currentState.viewAction) nextState.viewAction = null
            stateChannel.send(nextState)
        } catch (e: Exception) {
            // Could not submit update
            Timber.e(e)
        }
    }

    protected fun safeLaunch(errorHandler: CoroutineExceptionHandler = coroutineErrorHandler, block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(Dispatchers.IO + errorHandler, block = block)
}
