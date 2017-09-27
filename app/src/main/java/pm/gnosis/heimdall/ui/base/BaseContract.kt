package pm.gnosis.heimdall.ui.base

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import java.lang.RuntimeException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton


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

    @Singleton
    class ViewModelFactory @Inject constructor(
            private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            var creator = creators[modelClass]
            if (creator == null) {
                for ((key, value) in creators) {
                    if (modelClass.isAssignableFrom(key)) {
                        creator = value
                        break
                    }
                }
            }
            creator ?: throw IllegalArgumentException("Unknown model class $modelClass")
            try {
                return modelClass.cast(creator.get())
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

}

fun <I, D, O> BaseContract.TransformerViewModel.Builder<I, D, O>.buildTransformer(initialData: Observable<D> = Observable.empty()) =
        ObservableTransformer<I, O> { events ->
            events.publish { it.flatMap { handleEvent(it) }.startWith(initialData) }
                    .scan(initialViewState(), this::updateViewState)
        }