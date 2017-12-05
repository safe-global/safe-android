package pm.gnosis.heimdall.ui.transactions

import android.os.Bundle
import android.view.View
import com.gojuno.koptional.Optional
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_transaction_details.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.transactions.details.BaseTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.GenericTransactionDetailsFragment
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.models.Transaction
import pm.gnosis.utils.asDecimalString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject


abstract class BaseTransactionActivity : BaseActivity() {

    @Inject
    lateinit var viewModel: BaseTransactionContract

    private lateinit var currentFragment: BaseTransactionDetailsFragment
    private var currentInfo: Pair<BigInteger?, Transaction>? = null

    private val transactionInfoTransformer: ObservableTransformer<Pair<BigInteger?, Result<Transaction>>, *> =
            ObservableTransformer { up: Observable<Pair<BigInteger?, Result<Transaction>>> ->
                up.doOnNext { setUnknownTransactionInfo() }
                        .flatMap {
                            checkInfoAndPerform(it, { safeAddress, transaction ->
                                viewModel.loadTransactionInfo(safeAddress, transaction)
                            })
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext({ it.handle(::updateInfo, ::handleInfoError) })
            }

    private val cacheTransactionInfoTransformer: ObservableTransformer<Pair<BigInteger?, Result<Transaction>>, *> =
            ObservableTransformer { up: Observable<Pair<BigInteger?, Result<Transaction>>> ->
                up.observeOn(AndroidSchedulers.mainThread()).doOnNext({ (safeAddress, transaction) ->
                    transaction.handle({ currentInfo = safeAddress to it }, { currentInfo = null })
                })
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_transaction_details)

        setupToolbar(layout_transaction_details_toolbar, R.drawable.ic_close_24dp)
    }

    abstract fun loadTransactionDetails()

    override fun onStart() {
        super.onStart()
        setUnknownTransactionInfo()
        disposables += layout_transaction_details_button.clicks()
                .flatMap {
                    currentInfo?.let { (safeAddress, transaction) ->
                        safeAddress?.let {
                            viewModel.submitTransaction(safeAddress, transaction)
                                    .subscribeOn(AndroidSchedulers.mainThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .doOnSubscribe { submittingTransaction(true) }
                                    .doAfterTerminate { submittingTransaction(false) }
                                    .toObservable()
                        }
                    } ?: Observable.empty<Result<Unit>>()
                }.subscribeForResult({ finish() }, { showErrorSnackbar(it) })

        val fragment = supportFragmentManager.findFragmentById(R.id.layout_transaction_details_transaction_container)
        if (fragment is BaseTransactionDetailsFragment) {
            setupFragment(fragment)
        } else {
            loadTransactionDetails()
        }
    }

    private fun submittingTransaction(loading: Boolean) {
        layout_transaction_details_progress_bar.visibility = if (loading) View.VISIBLE else View.GONE
        layout_transaction_details_button.isEnabled = !loading
        currentFragment.inputEnabled(!loading)
    }

    protected fun displayTransactionDetails(fragment: BaseTransactionDetailsFragment) {
        supportFragmentManager.transaction {
            replace(R.id.layout_transaction_details_transaction_container, fragment)
        }
        setupFragment(fragment)
    }

    private fun setupFragment(fragment: BaseTransactionDetailsFragment) {
        currentFragment = fragment
        disposables += Observable.combineLatest(fragment.observeSafe(), fragment.observeTransaction(), BiFunction { safeAddress: Optional<BigInteger>, transaction: Result<Transaction> ->
            safeAddress.toNullable() to transaction
        })
                .observeOn(AndroidSchedulers.mainThread())
                .publish { shared ->
                    Observable.merge(
                            shared.compose(transactionInfoTransformer),
                            shared.compose(cacheTransactionInfoTransformer)
                    )
                }.subscribeBy(onError = Timber::e)
        layout_transaction_details_progress_bar.visibility = View.GONE
    }

    private fun updateInfo(info: BaseTransactionContract.Info) {
        info.transactionInfo.let {
            val leftConfirmations = Math.max(0, it.requiredConfirmation - it.confirmations - if (it.isOwner) 1 else 0)
            layout_transaction_details_confirmations_hint_text.text = getString(R.string.confirm_transaction_hint, leftConfirmations.toString())
            layout_transaction_details_confirmations.text = getString(R.string.x_of_x_confirmations, it.confirmations.toString(), it.requiredConfirmation.toString())
            layout_transaction_details_button.isEnabled = it.isOwner && !it.isExecuted
        }
        if (info.estimation != null) {
            layout_transaction_details_transaction_fee.text = getString(R.string.x_wei, info.estimation.value.asDecimalString())
        } else {
            setUnknownEstimate()
        }
    }

    private fun setUnknownEstimate() {
        layout_transaction_details_transaction_fee.text = "-"
    }

    private fun setUnknownTransactionInfo() {
        setUnknownEstimate()
        layout_transaction_details_button.isEnabled = false
        layout_transaction_details_confirmations_hint_text.text = getString(R.string.confirm_transaction_hint, "-")
        layout_transaction_details_confirmations.text = getString(R.string.x_of_x_confirmations, "-", "-")
    }

    private fun handleInfoError(throwable: Throwable) {
        Timber.e(throwable)
        showErrorSnackbar(throwable)
    }

    private fun showErrorSnackbar(throwable: Throwable) {
        // We don't want to show a snackbar if no safe is selected (UI should have been updated accordingly)
        if (throwable is NoSafeSelectedException) return
        if (throwable !is GenericTransactionDetailsFragment.TransactionInputException || throwable.showSnackbar) {
            errorSnackbar(layout_transaction_details_transaction_container, throwable)
        }
    }

    private fun <T> checkInfoAndPerform(info: Pair<BigInteger?, Result<Transaction>>, action: (BigInteger, Transaction) -> Observable<Result<T>>): Observable<Result<T>> =
            info.let { (safeAddress, transaction) ->
                safeAddress?.let {
                    when (transaction) {
                        is DataResult -> action(safeAddress, transaction.data)
                        is ErrorResult -> Observable.just(ErrorResult(transaction.error))
                    }
                } ?: Observable.just(ErrorResult(NoSafeSelectedException()))
            }

    protected open fun createDetailsFragment(safeAddress: String?, type: TransactionType, transaction: Transaction?, editable: Boolean): BaseTransactionDetailsFragment {
        return when (type) {
            else -> GenericTransactionDetailsFragment.createInstance(transaction, safeAddress, editable)
        }
    }

    abstract fun inject()

    private class NoSafeSelectedException : Exception()
}