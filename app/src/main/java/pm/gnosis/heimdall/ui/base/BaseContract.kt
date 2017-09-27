package pm.gnosis.heimdall.ui.base

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.ObservableTransformer

object BaseContract {
    interface UiEvent

    abstract class TransformerViewModel<I, O> : ViewModel() {
        abstract fun transformer(): ObservableTransformer<I, O>

        interface Builder<in I, D, O> {

            fun handleEvent(event: I): Observable<D>

            fun initialViewState(): O

            fun updateViewState(currentState: O, data: D): O
        }
    }
}

fun <I, D, O> BaseContract.TransformerViewModel.Builder<I, D, O>.buildTransformer(initialData: Observable<D> = Observable.empty()) =
        ObservableTransformer<I, O> { events ->
            events.publish { it.flatMap { handleEvent(it) }.startWith(initialData) }
                    .scan(initialViewState(), this::updateViewState)
        }
