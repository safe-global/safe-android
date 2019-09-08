package pm.gnosis.heimdall.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class LifecycleBoundObserver(
    private val onCreate: (() -> Unit)? = null,
    private val onStart: (() -> Unit)? = null,
    private val onResume: (() -> Unit)? = null,
    private val onPause: (() -> Unit)? = null,
    private val onStop: (() -> Unit)? = null,
    private val onDestroy: (() -> Unit)? = null,
    private val onAny: (() -> Unit)? = null
) : LifecycleEventObserver {
    fun observe(lifecycleOwner: LifecycleOwner) {
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> onCreate?.invoke()
            Lifecycle.Event.ON_START -> onStart?.invoke()
            Lifecycle.Event.ON_RESUME -> onResume?.invoke()
            Lifecycle.Event.ON_PAUSE -> onPause?.invoke()
            Lifecycle.Event.ON_STOP -> onStop?.invoke()
            Lifecycle.Event.ON_DESTROY -> {
                onDestroy?.invoke()
                source.lifecycle.removeObserver(this)
            }
            Lifecycle.Event.ON_ANY -> onAny?.invoke()
        }
    }
}

