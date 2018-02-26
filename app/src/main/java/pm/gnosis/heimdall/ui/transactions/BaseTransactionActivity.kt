package pm.gnosis.heimdall.ui.transactions

import android.os.Bundle
import android.support.design.widget.Snackbar
import com.gojuno.koptional.Optional
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.merge_transaction_details_container.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.transaction
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.transactions.details.base.BaseTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.models.Transaction
import timber.log.Timber
import java.math.BigInteger


abstract class BaseTransactionActivity : BaseActivity() {

    private var currentFragment: BaseTransactionDetailsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
    }

    abstract fun loadTransactionDetails()

    override fun onStart() {
        super.onStart()
        val fragment = supportFragmentManager.findFragmentById(R.id.merge_transaction_details_container)
        if (fragment is BaseTransactionDetailsFragment) {
            setCurrentFragment(fragment)
        } else {
            loadTransactionDetails()
        }
    }

    protected fun transactionInputEnabled(enabled: Boolean) {
        currentFragment?.inputEnabled(enabled)
    }

    protected fun displayTransactionDetails(fragment: BaseTransactionDetailsFragment) {
        setCurrentFragment(fragment)
        supportFragmentManager.transaction {
            replace(R.id.merge_transaction_details_container, fragment)
        }
    }

    private fun setCurrentFragment(fragment: BaseTransactionDetailsFragment) {
        currentFragment = fragment
    }

    abstract fun transactionDataTransformer(): ObservableTransformer<Pair<BigInteger?, Result<Transaction>>, Any>

    abstract fun fragmentRegistered()

    fun registerFragmentObservables(fragment: BaseTransactionDetailsFragment) {
        if (currentFragment != fragment) {
            return
        }
        disposables += Observable.combineLatest(
            fragment.observeSafe(),
            fragment.observeTransaction(),
            BiFunction { safeAddress: Optional<BigInteger>, transaction: Result<Transaction> ->
                safeAddress.toNullable() to transaction
            })
            .observeOn(AndroidSchedulers.mainThread())
            .compose(transactionDataTransformer())
            .subscribeBy(onError = Timber::e)
        fragmentRegistered()
    }

    protected fun handleInputError(throwable: Throwable) {
        Timber.e(throwable)
        showErrorSnackbar(throwable, Snackbar.LENGTH_INDEFINITE)
    }

    protected fun showErrorSnackbar(throwable: Throwable, duration: Int = Snackbar.LENGTH_LONG) {
        // We don't want to show a snackbar if no safe is selected (UI should have been updated accordingly)
        if (throwable is NoSafeSelectedException) return
        if (throwable !is TransactionInputException || throwable.showSnackbar) {
            errorSnackbar(merge_transaction_details_container, throwable, duration)
        }
    }

    protected fun <T> Observable<Pair<BigInteger?, Result<Transaction>>>.checkedFlatMap(action: (BigInteger, Transaction) -> Observable<Result<T>>): Observable<Result<T>> =
        flatMap {
            it.let { (safeAddress, transaction) ->
                safeAddress?.let {
                    when (transaction) {
                        is DataResult -> action(safeAddress, transaction.data)
                        is ErrorResult -> Observable.just(ErrorResult(transaction.error))
                    }
                } ?: Observable.just(ErrorResult(NoSafeSelectedException()))
            }
        }

    abstract fun inject()

    private class NoSafeSelectedException : Exception()
}