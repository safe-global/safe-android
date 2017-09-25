package pm.gnosis.heimdall.ui.base

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.ui.security.SecurityContract
import javax.inject.Inject


object BaseContract {

    interface UiEvent

    abstract class TransformerViewModel<I, O>: ViewModel() {
        abstract fun transformer(): ObservableTransformer<I, O>

        interface Builder<in I, D, O> {

            fun handleEvent(event: I): Observable<D>

            fun initialViewState(): O

            fun updateViewState(currentState: O, data: D): O
        }
    }

    @ForView
    class ViewModelFactory @Inject constructor(
            private val securityViewModel: SecurityContract.ViewModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SecurityContract.ViewModel::class.java)) {
                return modelClass.cast(securityViewModel)
            }
            throw IllegalStateException("Unknown model class $modelClass")
        }
    }

}

fun <I, D, O> BaseContract.TransformerViewModel.Builder<I, D, O>.buildTransformer(initialData: Observable<D> = Observable.empty()) =
        ObservableTransformer<I, O> { events ->
            events.publish { it.flatMap { handleEvent(it) }.startWith(initialData) }
                    .scan(initialViewState(), this::updateViewState)
        }