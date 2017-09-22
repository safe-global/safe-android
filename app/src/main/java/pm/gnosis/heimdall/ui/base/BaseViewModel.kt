package pm.gnosis.heimdall.ui.base

import io.reactivex.Observable
import io.reactivex.ObservableTransformer

/**
 * I - Events emitted by the UI (input)
 * D - Data that causes the UI to change (e.g. results caused by input)
 * O - UI state to adjust the view (output)
 */
abstract class BaseViewModel<I, D, O> : BaseContract.BaseViewModel<I, O> {

    private var cachedViewState: O? = null

    open fun initialData(): Observable<D> = Observable.empty()

    protected abstract fun handleEvent(event: I): Observable<D>

    protected abstract fun initialViewState(): O

    protected abstract fun updateViewState(currentState: O, data: D): O

    override fun transformer() = ObservableTransformer<I, O> { events ->
        events.publish { it.flatMap { handleEvent(it) }.startWith(initialData()) }
                .scan(cachedViewState ?: initialViewState(), this::updateViewState)
    }
}