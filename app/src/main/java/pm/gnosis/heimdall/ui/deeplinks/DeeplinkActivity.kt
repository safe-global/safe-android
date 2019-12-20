package pm.gnosis.heimdall.ui.deeplinks

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.screen_wallet_connect_safe_selection.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.rx2.await
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.BuildConfig
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
import pm.gnosis.heimdall.utils.parseToBigInteger
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.*
import timber.log.Timber
import java.lang.IllegalStateException
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

interface DeeplinkTransactionParser {
    fun parse(link: String): Pair<SafeTransaction, String?>
}

@Singleton
class EIP681DeeplinkTransactionParser @Inject constructor() : DeeplinkTransactionParser {
    override fun parse(link: String): Pair<SafeTransaction, String?> {
        if (!link.startsWith(ETHEREUM_SCHEME)) throw IllegalArgumentException("Not an ethereum url")
        val data = link.removePrefix(ERC67_PREFIX).removePrefix("//") // In case a normal url scheme was used
        val state = ParserState()
        for (i in data.indices) {
            when (data[i]) {
                Type.ADDRESS.separator -> parseSegment(state, data, Type.ADDRESS, i)
                Type.CHAIN_ID.separator -> parseSegment(state, data, Type.CHAIN_ID, i)
                Type.FUNCTION.separator -> parseSegment(state, data, Type.FUNCTION, i)
                Type.PARAMS.separator -> parseSegment(state, data, Type.PARAMS, i)
            }
        }
        parseSegment(state, data, Type.PARAMS, data.length)
        cleanParams(state)
        return state.run {
            SafeTransaction(
                Transaction(
                    address ?: throw IllegalArgumentException("No to address provided"),
                    value = state.value?.let { Wei(it) },
                    data = state.encodeData()
                ),
                operation = TransactionExecutionRepository.Operation.CALL
            ) to state.referrer
        }
    }

    private data class ParserState(
        var currentType: Type = Type.ADDRESS,
        var currentIndex: Int = 0,
        var address: Solidity.Address? = null,
        var value: BigInteger? = null,
        var function: String? = null,
        var referrer: String? = null,
        var functionParams: MutableList<Pair<String, String>>? = null
    ) {
        fun encodeData(): String? =
            function?.let {
                val signatureBuilder = StringBuilder()
                val encodedData = functionParams?.map { (key, value) ->
                    if (signatureBuilder.isNotEmpty()) signatureBuilder.append(",")
                    signatureBuilder.append(key)
                    parseValue(key, value)
                }?.let {
                    SolidityBase.encodeTuple(it)
                }
                "0x" + Sha3Utils.keccak("$function($signatureBuilder)".toByteArray()).toHexString().substring(0, 8) + encodedData
            }

        private fun parseValue(type: String, value: String) =
            when {
                type == "address" ->
                    value.asEthereumAddress()!!
                type.startsWith("int") ->
                    Solidity.Int256(if (value.startsWith("0x")) SolidityBase.decodeInt(value.removeHexPrefix()) else value.parseToBigInteger())
                type.startsWith("uint") ->
                    Solidity.Int256(value.parseToBigInteger())
                type == "string" ->
                    if (value.startsWith("0x"))
                        Solidity.String(String(value.hexToByteArray()))
                    else
                        Solidity.String(value)
                type == "bytes" ->
                    Solidity.Bytes(value.hexToByteArray())
                type.startsWith("bytes") ->
                    Solidity.Bytes32(value.hexToByteArray())
                type == "bool" ->
                    Solidity.Bool(
                        when (value) {
                            "true" -> true
                            "false" -> false
                            else -> SolidityBase.decodeBool(value.removeHexPrefix())
                        }
                    )
                else -> throw IllegalArgumentException("Unknown value type")
            }
    }

    private fun parseSegment(
        state: ParserState,
        data: String,
        nextType: Type,
        currentIndex: Int
    ) {
        if (currentIndex > state.currentIndex) {
            when (state.currentType) {
                Type.CHAIN_ID ->
                    if (data.substring(state.currentIndex, currentIndex).toLong() != BuildConfig.BLOCKCHAIN_CHAIN_ID)
                        throw IllegalStateException("Wrong network")
                Type.ADDRESS ->
                    state.address = data.substring(state.currentIndex, currentIndex).asEthereumAddress()!!
                Type.FUNCTION ->
                    state.function = data.substring(state.currentIndex, currentIndex)
                Type.PARAMS ->
                    state.functionParams = data.substring(state.currentIndex, currentIndex).split("&").mapTo(mutableListOf()) {
                        it.split("=", limit = 2).run { first() to get(1) }
                    }
            }
        }
        state.currentType = nextType
        state.currentIndex = currentIndex + 1 // Skip separator
    }

    private fun cleanParams(state: ParserState) {
        state.functionParams?.filter { (key, value) ->
            when (key) {
                KEY_GAS, KEY_GAS_LIMIT, KEY_GAS_PRICE -> false
                KEY_DEEPLINK_REFERRER -> {
                    state.referrer = value
                    false
                }
                KEY_VALUE -> {
                    state.value = value.parseToBigInteger()
                    false
                }
                else -> true
            }
        }

    }

    enum class Type(val separator: Char) {
        ADDRESS(':'),
        CHAIN_ID('@'),
        FUNCTION('/'),
        PARAMS('?'),
    }

    companion object {
        private const val ETHEREUM_SCHEME = "ethereum"
        private const val ERC67_PREFIX = "$ETHEREUM_SCHEME:"
        private const val KEY_VALUE = "value"
        private const val KEY_GAS = "gas"
        private const val KEY_GAS_LIMIT = "gasLimit"
        private const val KEY_GAS_PRICE = "gasPrice"
        private const val KEY_DEEPLINK_REFERRER = "referrer"
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
