package pm.gnosis.heimdall.ui.deeplinks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.screen_wallet_connect_safe_selection.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitFirst
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.BaseStateViewModel
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.base.handleViewAction
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.safe.list.SafeAdapter
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.heimdall.utils.errorToast
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.*
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

interface DeeplinkTransactionParser {
    fun parse(link: String): Pair<SafeTransaction, String?>
}

@Singleton
class AndroidDeeplinkTransactionParser @Inject constructor() : DeeplinkTransactionParser {
    override fun parse(link: String): Pair<SafeTransaction, String?> =
        Uri.parse(link).run {
            SafeTransaction(Transaction(
                host!!.asEthereumAddress()!!,
                value = Wei(getQueryParameter(KEY_VALUE)?.run { if (startsWith("0x")) hexAsBigInteger() else toBigInteger() }
                    ?: BigInteger.ZERO),
                data = getQueryParameter(KEY_DATA)?.hexStringToByteArray()?.toHex()?.addHexPrefix()
            ), TransactionExecutionRepository.Operation.CALL) to getQueryParameter(KEY_REFERRER)
        }

    companion object {
        private const val KEY_DATA = "data"
        private const val KEY_VALUE = "value"
        private const val KEY_REFERRER = "referrer"
    }
}

abstract class DeeplinkContract(
    context: Context,
    appDispatchers: ApplicationModule.AppCoroutineDispatchers
) : BaseStateViewModel<DeeplinkContract.State>(context, appDispatchers) {
    abstract fun setup(link: String)
    abstract fun selectSafe(safe: Solidity.Address)
    data class State(
        val loading: Boolean,
        val safes: Adapter.Data<Safe>,
        override var viewAction: ViewAction?
    ) : BaseStateViewModel.State

    data class CloseScreenWithError(val error: Throwable) : ViewAction
}

class DeeplinkViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    appDispatchers: ApplicationModule.AppCoroutineDispatchers,
    private val safeRepository: GnosisSafeRepository,
    private val transactionInfoRepository: TransactionInfoRepository,
    private val deeplinkTransactionParser: DeeplinkTransactionParser
) : DeeplinkContract(context, appDispatchers) {

    private lateinit var deeplink: String
    private lateinit var safeTransaction: SafeTransaction
    private lateinit var transactionData: TransactionData
    private var referrer: String? = null

    private val loadingErrorHandler = CoroutineExceptionHandler { context, e ->
        viewModelScope.launch { updateState { copy(loading = false) } }
        coroutineErrorHandler.handleException(context, e)
    }

    private fun loadingLaunch(block: suspend CoroutineScope.() -> Unit) = safeLaunch(loadingErrorHandler, block)

    override fun initialState() = State(false, Adapter.Data(), null)

    override val state = liveData {
        loadSafes()
        for (event in stateChannel.openSubscription()) emit(event)
    }

    override fun setup(link: String) {
        deeplink = link
    }

    override fun selectSafe(safe: Solidity.Address) {
        safeLaunch {
            try {
                transactionInfoRepository.checkRestrictedTransaction(safe, safeTransaction).await()
            } catch (e: Exception) {
                finishWithErrorMessage(e.message ?: "It is not allowed to perform this transaction")
                return@safeLaunch
            }
            updateState {
                copy(
                    viewAction = ViewAction.StartActivity(
                        ReviewTransactionActivity.createIntent(
                            context,
                            safe,
                            transactionData,
                            referrer = referrer
                        )
                    )
                )
            }
        }

    }

    private suspend fun parseTransactionData(): Boolean {
        return try {
            deeplinkTransactionParser.parse(deeplink).let {
                safeTransaction = it.first
                referrer = it.second
            }
            transactionData = transactionInfoRepository.parseTransactionData(safeTransaction).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            finishWithErrorMessage("Invalid transaction in deeplink")
            false
        }
    }

    private suspend fun finishWithErrorMessage(message: String) {
        updateState { copy(viewAction = CloseScreenWithError(SimpleLocalizedException(message))) }
    }

    private fun loadSafes() {
        if (state.value?.loading == true) return // Already loading
        loadingLaunch {
            updateState { copy(loading = true) }
            if (!parseTransactionData()) return@loadingLaunch
            val safes = safeRepository.observeSafes().awaitFirst()
            when {
                safes.isEmpty() -> {
                    finishWithErrorMessage("No Safe available")
                }
                safes.size == 1 -> {
                    updateState { copy(loading = false) }
                    selectSafe(safes.first().address)
                }
                else -> {
                    updateState { copy(loading = false, safes = Adapter.Data(entries = safes)) }
                }
            }
        }
    }
}

class DeeplinkActivity : ViewModelActivity<DeeplinkContract>() {

    @Inject
    lateinit var adapter: SafeAdapter

    @Inject
    lateinit var layoutManager: LinearLayoutManager

    override fun screenId(): ScreenId = ScreenId.DEEPLINK

    override fun layout(): Int = R.layout.screen_deeplink

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_RESULT_PROXY) {
            setResult(resultCode, data)
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action != Intent.ACTION_VIEW || intent.data == null) {
            finish()
            return
        }

        wallet_connect_safe_selection_list.layoutManager = layoutManager
        wallet_connect_safe_selection_list.adapter = adapter
        wallet_connect_safe_selection_close_btn.setOnClickListener { onBackPressed() }
        viewModel.setup(intent.data?.toString()!!)
        viewModel.state.observe(this, Observer { onUpdate(it) })
    }

    private fun onUpdate(state: DeeplinkContract.State) {
        adapter.updateData(state.safes)
        when (val viewAction = state.viewAction) {
            is DeeplinkContract.CloseScreenWithError -> {
                errorToast(viewAction.error)
                finish()
            }
            is BaseStateViewModel.ViewAction.StartActivity -> {
                if (callingActivity != null) {
                    startActivityForResult(viewAction.intent, REQUEST_CODE_RESULT_PROXY)
                } else {
                    startActivity(viewAction.intent)
                    finish()
                }
            }
            else -> wallet_connect_safe_selection_list.handleViewAction(viewAction) { finish() }
        }
    }

    override fun onStart() {
        super.onStart()

        disposables += adapter.safeSelection
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = Timber::e) {
                viewModel.selectSafe(it.address())
            }
    }

    companion object {
        private const val REQUEST_CODE_RESULT_PROXY = 9658
    }

}
