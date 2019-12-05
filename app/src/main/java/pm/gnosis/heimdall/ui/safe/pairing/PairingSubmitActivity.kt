package pm.gnosis.heimdall.ui.safe.pairing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.screen_pairing_review.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.dialogs.base.ConfirmationDialog
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.*
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.nullOnThrow
import timber.log.Timber
import java.math.BigInteger
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.android.synthetic.main.include_transfer_summary_final.transfer_data_fees_error as feesError
import kotlinx.android.synthetic.main.include_transfer_summary_final.transfer_data_fees_info as feesInfo
import kotlinx.android.synthetic.main.include_transfer_summary_final.transfer_data_fees_value as feesValue
import kotlinx.android.synthetic.main.include_transfer_summary_final.transfer_data_safe_balance_after_value as balanceAfterValue
import kotlinx.android.synthetic.main.include_transfer_summary_final.transfer_data_safe_balance_before_value as balanceBeforeValue

class PairingSubmitActivity : ViewModelActivity<PairingSubmitContract>(), ConfirmationDialog.OnDismissListener {

    @Inject
    lateinit var addressHelper: AddressHelper

    private var submissionInProgress = false
    private lateinit var pairingAction: PairingAction

    override fun layout(): Int = R.layout.screen_pairing_review

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId(): ScreenId? = ScreenId.PAIRING_SUBMIT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val txGas = nullOnThrow { intent.getStringExtra(EXTRA_TX_GAS)?.toBigInteger() } ?: run { finish(); return }
        val dataGas = nullOnThrow { intent.getStringExtra(EXTRA_DATA_GAS)?.toBigInteger() } ?: run { finish(); return }
        val operationalGas = nullOnThrow { intent.getStringExtra(EXTRA_DATA_GAS)?.toBigInteger() } ?: run { finish(); return }
        val gasPrice = nullOnThrow { intent.getStringExtra(EXTRA_GAS_PRICE)?.toBigInteger() } ?: run { finish(); return }
        val gasToken =
            nullOnThrow { intent.getStringExtra(EXTRA_GAS_TOKEN)?.asEthereumAddress()!! } ?: run { finish(); return }
        val safeTransaction = intent.getParcelableExtra<SafeTransaction>(EXTRA_SAFE_TRANSACTION) ?: run { finish(); return }
        val signature1 = nullOnThrow { Signature.from(intent.getStringExtra(EXTRA_SIGNATURE_1)) } ?: run { finish(); return }
        val signature2 = nullOnThrow { Signature.from(intent.getStringExtra(EXTRA_SIGNATURE_2)) } ?: run { finish(); return }
        val authenticatorInfo = intent.getAuthenticatorInfo() ?: run { finish(); return }
        val txHash = nullOnThrow { intent.getStringExtra(EXTRA_TX_HASH).hexStringToByteArray() } ?: run { finish(); return }

        val pairingName = when (authenticatorInfo.authenticator.type) {
            AuthenticatorInfo.Type.KEYCARD -> getString(R.string.status_keycard)
            AuthenticatorInfo.Type.EXTENSION -> getString(R.string.gnosis_safe_authenticator)
        }

        pairingAction = nullOnThrow { intent.getSerializableExtra(EXTRA_PAIRING_ACTION) as PairingAction? }  ?: run { finish(); return }

        when (pairingAction) {

            PairingAction.REPLACE -> {
                review_transaction_info_value.text = getString(R.string.replace_2fa)
                review_transaction_info_description.text = getString(R.string.replace_2fa_desc, pairingName)
            }
            PairingAction.REMOVE -> {
                review_transaction_info_value.text = getString(R.string.disable_2fa)
                review_transaction_info_description.text = getString(R.string.disable_2fa_desc, pairingName)
            }
        }


        viewModel.setup(safeTransaction, signature1, signature2, txGas, dataGas, operationalGas, gasPrice, gasToken, authenticatorInfo, txHash)

    }

    override fun onStart() {
        super.onStart()

        disposables += viewModel.loadFeeInfo()
            .subscribeBy { feesValue.text = it.displayString(roundingMode = RoundingMode.UP) }

        addressHelper.populateAddressInfo(
            review_transaction_info_safe_address,
            review_transaction_info_safe_name,
            review_transaction_info_safe_image,
            viewModel.getSafeTransaction().wrapped.address
        ).forEach { disposables.add(it) }

        disposables += viewModel.observeSubmitStatus()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onSafeBalance, onError = ::onSafeBalanceError)


        review_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        feesInfo.setOnClickListener {
            InfoTipDialogBuilder.build(this, R.layout.dialog_network_fee, R.string.ok).show()
        }

        disposables += review_bottom_panel.forwardClicks
            .flatMapSingle { _ ->
                viewModel.submitTransaction()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { onSubmitTransactionProgress(true) }
                    .doAfterTerminate { onSubmitTransactionProgress(false) }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {

                when (pairingAction) {

                    PairingAction.REPLACE -> {
                        ConfirmationDialog.create(R.drawable.img_2fa_replace, R.string.tx_submitted_replace_be)
                            .show(supportFragmentManager, null)
                    }
                    PairingAction.REMOVE -> {
                        ConfirmationDialog.create(R.drawable.img_2fa_disable, R.string.tx_submitted_remove_be)
                            .show(supportFragmentManager, null)
                    }
                }

            }, onError = ::onSubmitTransactionError)

    }

    override fun onConfirmationDialogDismiss() {
        startActivity(
            SafeMainActivity.createIntent(
                this,
                viewModel.getSafeTransaction().wrapped.address,
                selectedTab = 1
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    private fun onSafeBalance(status: PairingSubmitContract.SubmitStatus) {
        review_bottom_panel.disabled = !(status.canSubmit && !submissionInProgress)
        balanceBeforeValue.text = status.balance.displayString()
        balanceAfterValue.text = status.balanceAfter.displayString()
        feesError.text = getString(R.string.insufficient_funds_please_add, status.balance.token.symbol)
        feesError.visible(!status.canSubmit)
    }

    private fun onSafeBalanceError(throwable: Throwable) {
        if (throwable !is PairingSubmitContract.NoTokenBalanceException) Timber.e(throwable)
        balanceBeforeValue.text = getString(R.string.error_retrieving_safe_balance)
        review_bottom_panel.disabled = true
        feesError.visible(false)
    }

    private fun onSubmitTransactionProgress(isLoading: Boolean) {
        submissionInProgress = isLoading
        review_progress_bar.visible(isLoading)
        review_bottom_panel.disabled = isLoading
    }

    private fun onSubmitTransactionError(throwable: Throwable) {
        errorSnackbar(review_coordinator, throwable)
    }

    companion object {
        private const val EXTRA_PAIRING_ACTION = "extra.parcelable.pairingAction"

        private const val EXTRA_SAFE_TRANSACTION = "extra.parcelable.safe_transaction"
        private const val EXTRA_SIGNATURE_1 = "extra.string.signature1"
        private const val EXTRA_SIGNATURE_2 = "extra.string.signature2"
        private const val EXTRA_TX_GAS = "extra.string.tx_gas"
        private const val EXTRA_DATA_GAS = "extra.string.data_gas"
        private const val EXTRA_OPERATIONAL_GAS = "extra.string.operational_gas"
        private const val EXTRA_GAS_PRICE = "extra.string.gas_price"
        private const val EXTRA_GAS_TOKEN = "extra.string.gas_token"
        private const val EXTRA_TX_HASH = "extra.string.tx_hash"

        fun createIntent(
            context: Context,
            action: PairingAction,
            safeTransaction: SafeTransaction,
            signature1: Signature,
            signature2: Signature,
            txGas: BigInteger,
            dataGas: BigInteger,
            operationalGas: BigInteger,
            gasPrice: BigInteger,
            gasToken: Solidity.Address,
            authenticatorSetupInfo: AuthenticatorSetupInfo,
            txHash: String
        ) =
            Intent(context, PairingSubmitActivity::class.java).apply {
                putExtra(EXTRA_PAIRING_ACTION, action)
                putExtra(EXTRA_SAFE_TRANSACTION, safeTransaction)
                putExtra(EXTRA_SIGNATURE_1, signature1.toString())
                putExtra(EXTRA_SIGNATURE_2, signature2.toString())
                putExtra(EXTRA_TX_GAS, txGas.toString())
                putExtra(EXTRA_DATA_GAS, dataGas.toString())
                putExtra(EXTRA_OPERATIONAL_GAS, operationalGas.toString())
                putExtra(EXTRA_GAS_PRICE, gasPrice.toString())
                putExtra(EXTRA_GAS_TOKEN, gasToken.asEthereumAddressString())
                putExtra(EXTRA_TX_HASH, txHash)
                authenticatorSetupInfo.put(this)
            }
    }
}

abstract class PairingSubmitContract : ViewModel() {

    abstract fun setup(
        safeTransaction: SafeTransaction,
        signature1: Signature,
        signature2: Signature,
        txGas: BigInteger,
        dataGas: BigInteger,
        operationalGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        authenticatorInfo: AuthenticatorSetupInfo,
        txHash: ByteArray
    )

    abstract fun observeSubmitStatus(): Observable<Result<SubmitStatus>>
    abstract fun submitTransaction(): Single<Result<Unit>>
    abstract fun loadFeeInfo(): Single<ERC20TokenWithBalance>
    abstract fun getSafeTransaction(): SafeTransaction
    abstract fun loadSafe(): Single<Safe>

    data class SubmitStatus(val balance: ERC20TokenWithBalance, val balanceAfter: ERC20TokenWithBalance, val canSubmit: Boolean)

    class NoTokenBalanceException : Exception()
}

class PairingSubmitViewModel @Inject constructor(
    private val cryptoHelper: CryptoHelper,
    private val gnosisSafeRepository: GnosisSafeRepository,
    private val pushServiceRepository: PushServiceRepository,
    private val tokenRepository: TokenRepository,
    private val transactionExecutionRepository: TransactionExecutionRepository
) : PairingSubmitContract() {
    private lateinit var safeTransaction: SafeTransaction
    private lateinit var signature1: Signature
    private lateinit var signature2: Signature
    private lateinit var txGas: BigInteger
    private lateinit var dataGas: BigInteger
    private lateinit var operationalGas: BigInteger
    private lateinit var gasPrice: BigInteger
    private lateinit var gasToken: Solidity.Address
    private lateinit var authenticatorInfo: AuthenticatorSetupInfo
    private lateinit var txHash: ByteArray


    override fun setup(
        safeTransaction: SafeTransaction,
        signature1: Signature,
        signature2: Signature,
        txGas: BigInteger,
        dataGas: BigInteger,
        operationalGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        authenticatorInfo: AuthenticatorSetupInfo,
        txHash: ByteArray
    ) {
        this.safeTransaction = safeTransaction
        this.signature1 = signature1
        this.signature2 = signature2
        this.txGas = txGas
        this.dataGas = dataGas
        this.operationalGas = operationalGas
        this.gasPrice = gasPrice
        this.gasToken = gasToken
        this.authenticatorInfo = authenticatorInfo
        this.txHash = txHash
    }

    override fun loadFeeInfo(): Single<ERC20TokenWithBalance> =
        tokenRepository.loadToken(gasToken).map {
            ERC20TokenWithBalance(it, requiredFunds())
        }

    override fun observeSubmitStatus(): Observable<Result<SubmitStatus>> =
        tokenRepository.loadToken(gasToken)
            .flatMapObservable { token ->
                Observable.interval(0, SAFE_BALANCE_REQUEST_INTERVAL, SAFE_BALANCE_REQUEST_TIME_UNIT)
                    .concatMap {
                        tokenRepository.loadTokenBalances(safeTransaction.wrapped.address, listOf(token))
                            .map { tokenBalances ->
                                if (tokenBalances.size != 1) throw NoTokenBalanceException()
                                tokenBalances.first().let { (token, balance) ->
                                    val balanceAfterTx = (balance ?: BigInteger.ZERO) - requiredFunds()
                                    val canSubmit = balanceAfterTx >= BigInteger.ZERO
                                    SubmitStatus(
                                        ERC20TokenWithBalance(token, balanceAfterTx),
                                        ERC20TokenWithBalance(token, balanceAfterTx - requiredFunds()),
                                        canSubmit
                                    )
                                }
                            }
                            .mapToResult()
                    }
            }

    private fun requiredFunds() = (txGas + dataGas + operationalGas) * gasPrice

    override fun getSafeTransaction() = safeTransaction

    override fun loadSafe() = gnosisSafeRepository.loadSafe(safeTransaction.wrapped.address)

    override fun submitTransaction() =
        gnosisSafeRepository.loadInfo(safeTransaction.wrapped.address)
            .firstOrError()
            .flatMap { info ->
                transactionExecutionRepository.calculateHash(
                    safeAddress = safeTransaction.wrapped.address,
                    transaction = safeTransaction,
                    txGas = txGas,
                    dataGas = dataGas,
                    gasPrice = gasPrice,
                    gasToken = gasToken,
                    version = info.version
                )
                    // Verify if transaction hash matches
                    .map { computedTxHash ->
                        if (!computedTxHash.contentEquals(txHash)) throw IllegalStateException("Invalid transaction hash")
                        else Unit
                    }
                    // Recover addresses from the signatures
                    .flatMap {
                        Single.zip(listOf(signature1, signature2).map { signature ->
                            Single.fromCallable { cryptoHelper.recover(txHash, signature) to signature }
                        }) { pairs -> (pairs.map { it as Pair<Solidity.Address, Signature> }).toList() }
                    }
                    // Submit transaction
                    .flatMap { signaturesWithAddresses ->
                        transactionExecutionRepository.submit(
                            safeAddress = safeTransaction.wrapped.address,
                            transaction = safeTransaction,
                            signatures = signaturesWithAddresses.toMap(),
                            senderIsOwner = false,
                            txGas = txGas,
                            dataGas = dataGas,
                            gasPrice = gasPrice,
                            gasToken = gasToken,
                            version = info.version,
                            addToHistory = true
                        )
                    }
            }
            .flatMapCompletable {
                gnosisSafeRepository.saveAuthenticatorInfo(authenticatorInfo.authenticator)
                if (authenticatorInfo.authenticator.type == AuthenticatorInfo.Type.EXTENSION) {
                    pushServiceRepository.propagateSafeCreation(safeTransaction.wrapped.address, setOf(authenticatorInfo.authenticator.address))
                        .onErrorComplete()
                } else {
                    Completable.complete()
                }
            }
            .mapToResult()

    companion object {
        private const val SAFE_BALANCE_REQUEST_INTERVAL = 5L
        private val SAFE_BALANCE_REQUEST_TIME_UNIT = TimeUnit.SECONDS
    }
}
