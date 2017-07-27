package pm.gnosis.android.app.wallet.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_transaction_details.*
import pm.gnosis.android.app.wallet.GnosisApplication
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.geth.GethRepository
import pm.gnosis.android.app.wallet.data.model.TransactionCallParams
import pm.gnosis.android.app.wallet.data.model.TransactionDetails
import pm.gnosis.android.app.wallet.data.remote.InfuraRepository
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.util.asDecimalString
import pm.gnosis.android.app.wallet.util.asHexString
import pm.gnosis.android.app.wallet.util.toast
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class TransactionDetailsActivity : AppCompatActivity() {
    companion object {
        const val TRANSACTION_EXTRA = "extra.transaction"
    }

    @Inject lateinit var infuraRepository: InfuraRepository
    @Inject lateinit var gethRepository: GethRepository
    private lateinit var transaction: TransactionDetails
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_transaction_details)

        intent.extras?.let {
            val t: TransactionDetails? = it.getParcelable(TRANSACTION_EXTRA)
            if (t == null) finish() else transaction = t
        }

        fillTransactionDetails()
        sign_transaction.setOnClickListener { signTransaction(transaction) }
    }

    override fun onStart() {
        super.onStart()
        val observables = ArrayList<Observable<FieldResult>>()

        if (transaction.gas == null) {
            observables.add(infuraRepository.estimateGas(
                    TransactionCallParams(to = transaction.address.asHexString(), data = transaction.data))
                    .doOnSubscribe { _ -> suggested_gas.text = "Loading..." }
                    .map { GasResult(it) })
        }

        if (transaction.nonce == null) {
            observables.add(infuraRepository.getTransactionCount()
                    .map { NonceResult(it) })
        }

        if (transaction.gasPrice == null) {
            observables.add(infuraRepository.getGasPrice()
                    .doOnSubscribe { _ -> gas_price.text = "Loading..." }
                    .map { GasPriceResult(it) })
        }

        disposables += Observable.merge(observables)
                .scan(transaction, { previous, result -> stateReducer(previous, result) })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::onTransactionDetails, onError = Timber::e)
    }

    private fun stateReducer(previous: TransactionDetails, result: FieldResult): TransactionDetails {
        return when (result) {
            is GasResult -> previous.copy(gas = result.value)
            is NonceResult -> previous.copy(nonce = result.value)
            is GasPriceResult -> previous.copy(gasPrice = result.value)
            else -> previous
        }
    }

    private fun onTransactionDetails(transactionDetails: TransactionDetails) {
        transaction = transactionDetails
        sign_transaction.isEnabled = transaction.nonce != null && transaction.gas != null && transaction.gasPrice != null
        gas_price.text = transaction.gasPrice?.asDecimalString() ?: "Loading..."
    }

    interface FieldResult
    data class GasResult(val value: BigInteger?) : FieldResult
    data class NonceResult(val value: BigInteger?) : FieldResult
    data class GasPriceResult(val value: BigInteger?) : FieldResult

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    fun fillTransactionDetails() {
        recipient.text = transaction.address.asHexString()
        suggested_gas.text = transaction.gas?.toString(10) ?: "No value"
        value.text = transaction.value?.toEther()?.stripTrailingZeros()?.toPlainString() ?: "No value"
        data.text = transaction.data ?: "No value"
    }

    data class NonceAndGasPrice(val nonce: BigInteger, val gasPrice: BigInteger)

    private fun signTransaction(transaction: TransactionDetails) {
        val nonce = transaction.nonce
        val gasLimit = transaction.gas
        val gasPrice = transaction.gasPrice

        if (nonce != null && gasLimit != null && gasPrice != null) {
            val signedTx = gethRepository.signTransaction(nonce, transaction.address, transaction.value?.value, gasLimit, gasPrice, transaction.data)
            Timber.d(signedTx)
        }
    }

    fun onSignedTransaction(hash: String) {
        toast(hash)
        Timber.d(hash)
    }

    fun onSignedError(throwable: Throwable) {
        Timber.e(throwable)
    }

    fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(GnosisApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }
}
