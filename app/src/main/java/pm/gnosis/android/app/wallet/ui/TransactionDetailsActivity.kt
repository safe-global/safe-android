package pm.gnosis.android.app.wallet.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_transaction_details.*
import pm.gnosis.android.app.wallet.GnosisApplication
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.geth.GethRepository
import pm.gnosis.android.app.wallet.data.model.TransactionDetails
import pm.gnosis.android.app.wallet.data.remote.InfuraRepository
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
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
    private var gasPrice: BigInteger? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_transaction_details)

        intent.extras?.let {
            val t: TransactionDetails? = it.getParcelable(TRANSACTION_EXTRA)
            if (t == null) finish() else transaction = t
        }

        fillTransactionDetails()
        sign_transaction.setOnClickListener { processTransaction() }
    }

    override fun onStart() {
        super.onStart()
        disposables += infuraRepository.getGasPrice()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _ -> onGasPriceLoading(true) }
                .doOnTerminate { onGasPriceLoading(false) }
                .subscribeBy(onNext = this::onGasPrice, onError = this::onGasPriceError)
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    fun fillTransactionDetails() {
        recipient.text = "0x${transaction.address.toString(16)}"
        suggested_gas.text = transaction.gas?.toString(10) ?: "No value"
        value.text = transaction.value?.toEther()?.stripTrailingZeros()?.toPlainString() ?: "No value"
        data.text = transaction.data ?: "No value"
    }

    fun processTransaction() {
        val wei = transaction.value?.value
        if (wei != null && gasPrice != null) { // Ether being transferred
            disposables += infuraRepository.getTransactionCount()
                    .map { gethRepository.signTransaction(it, transaction.address, wei, transaction.gas!!, gasPrice!!, transaction.data!!) }
                    .subscribeBy(onNext = this::onSignedTransaction, onError = this::onSignedError)
        }
    }

    fun onSignedTransaction(hash: String) {
        toast(hash)
    }

    fun onSignedError(throwable: Throwable) {
        Timber.e(throwable)
    }

    fun onGasPrice(gasPrice: BigInteger) {
        this.gasPrice = gasPrice
        gas_price.text = gasPrice.toString(10)
    }

    fun onGasPriceError(throwable: Throwable) {
        Timber.e(throwable)
        gas_price.text = "Error fetching gas price"
    }

    fun onGasPriceLoading(isLoading: Boolean) {
        if (isLoading) {
            gas_price.text = "Fetching gas price..."
        }
    }

    fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(GnosisApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }
}
