package pm.gnosis.heimdall.ui.base

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import io.reactivex.ObservableTransformer
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.ui.security.SecurityContract
import javax.inject.Inject


object BaseContract {

    interface UiEvent

    interface BaseViewModel<I, O> {
        fun transformer(): ObservableTransformer<I, O>
    }

    abstract class ViewModelHolder<I, O, out VM: BaseViewModel<I, O>>(val viewModel: VM): ViewModel() {
        fun transformer() = viewModel.transformer()
    }

    @ForView
    class ViewModelFactory @Inject constructor(
            private val securityViewModel: SecurityContract.ViewModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SecurityContract.ViewModelHolder::class.java)) {
                return modelClass.cast(SecurityContract.ViewModelHolder(securityViewModel))
            }
            throw IllegalStateException("Unknown model class $modelClass")
        }
    }

}