package io.gnosis.safe.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.gnosis.safe.di.modules.ApplicationModule
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import timber.log.Timber

//TODO cover with unit tests and refactor. For complete version of the class check branch safe_v1
abstract class BaseStateViewModel<T : ViewState>(
    private val appDispatcher: ApplicationModule.AppCoroutineDispatchers
) : ViewModel() {
    abstract val state: LiveData<T>

    protected abstract fun initialState(): T

    @Suppress("LeakingThis")
    private val stateChannel = ConflatedBroadcastChannel(initialState())

    private val coroutineErrorHandler = CoroutineExceptionHandler { _, e ->
        Timber.e(e)
    }

    protected fun currentState(): T = stateChannel.value

    protected suspend fun updateState(forceViewAction: Boolean = false, update: T.() -> T) {
        try {
            val currentState = currentState()
            val nextState = currentState.run(update)
            // Reset view action if the same
            takeUnless { !forceViewAction && nextState === currentState }?.run {
                stateChannel.send(nextState)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

}
