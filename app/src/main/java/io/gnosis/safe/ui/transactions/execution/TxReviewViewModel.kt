package io.gnosis.safe.ui.transactions.execution

import androidx.annotation.VisibleForTesting
import io.gnosis.data.backend.rpc.RpcClient
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TxData
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.utils.toSignature
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.owner.list.OwnerViewData
import io.gnosis.safe.ui.transactions.details.SigningMode
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.model.Solidity
import pm.gnosis.models.Fee1559
import pm.gnosis.models.TransactionEip1559
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.utils.rlp
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.toHexString
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

class TxReviewViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val credentialsRepository: CredentialsRepository,
    private val settingsHandler: SettingsHandler,
    private val rpcClient: RpcClient,
    private val balanceFormatter: BalanceFormatter,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TxReviewState>(appDispatchers) {

    lateinit var activeSafe: Safe
        private set

    var executionKey: OwnerViewData? = null
        private set

    var minNonce: BigInteger? = null
        private set

    var nonce: BigInteger? = null
        private set

    var gasLimit: BigInteger? = null
        private set

    var maxPriorityFeePerGas: BigDecimal? = null
        private set

    var maxFeePerGas: BigDecimal? = null
        private set

    private lateinit var txData: TxData

    private lateinit var executionInfo: DetailedExecutionInfo

    private var userEditedFeeData: Boolean = false

    private var ethTx: TransactionEip1559? = null

    private var ethTxSignature: ECDSASignature? = null

    init {
        safeLaunch {
            activeSafe = safeRepository.getActiveSafe()!!
        }
    }

    override fun initialState() = TxReviewState(viewAction = null)

    fun setTxData(txData: TxData, executionInfo: DetailedExecutionInfo) {
        this.txData = txData
        this.executionInfo = executionInfo
        loadDefaultKey()
    }

    fun isInitialized(): Boolean = txData != null && executionInfo != null

    fun isLoading(): Boolean {
        val viewAction = (state.value as TxReviewState).viewAction
        return (viewAction is ViewAction.Loading && viewAction.isLoading)
    }

    @VisibleForTesting
    fun loadDefaultKey() {
        safeLaunch {
            val owners = credentialsRepository.owners()
                .map { OwnerViewData(it.address, it.name, it.type) }
                .sortedBy { it.name }
            activeSafe.signingOwners?.let {
                val acceptedOwners = owners.filter { localOwner ->
                    activeSafe.signingOwners.any {
                        localOwner.address == it
                    }
                }
                executionKey = acceptedOwners.first()
                updateState {
                    TxReviewState(viewAction = DefaultKey(key = executionKey))
                }
                updateDefaultKeyBalance()
                estimate()
            }
        }
    }

    fun updateDefaultKey(address: Solidity.Address) {
        safeLaunch {
            address?.let {
                val owner = credentialsRepository.owner(it)!!
                executionKey = OwnerViewData(owner.address, owner.name, owner.type)
                updateState {
                    TxReviewState(viewAction = DefaultKey(key = executionKey))
                }
                updateDefaultKeyBalance()
                estimate()
            }
        }
    }

    private suspend fun updateDefaultKeyBalance() {
        executionKey?.let {
            val balanceWei = rpcClient.getBalance(it.address)
            balanceWei?.let {
                executionKey = executionKey!!.copy(
                    balance = balanceString(balanceWei.value),
                    zeroBalance = balanceWei.value == BigInteger.ZERO
                )
                updateState {
                    TxReviewState(viewAction = DefaultKey(executionKey))
                }
            }
        }
    }

    fun estimate() {
        safeLaunch {

            if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
                executionKey?.let {

                    updateState {
                        TxReviewState(viewAction = Loading(true))
                    }

                    ethTx = rpcClient.ethTransaction(
                        activeSafe,
                        it.address,
                        txData,
                        executionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
                        BigInteger.valueOf(DEFAULT_MINER_TIP)
                    )

                    val estimationParams = rpcClient.estimate(activeSafe.chain, ethTx!!)

                    executionKey = executionKey!!.copy(
                        balance = balanceString(estimationParams.balance),
                        zeroBalance = estimationParams.gasPrice == BigInteger.ZERO
                    )

                    updateState {
                        TxReviewState(viewAction = DefaultKey(key = executionKey))
                    }

                    val gasPrice = estimationParams.gasPrice
                    minNonce = estimationParams.nonce
                    // If user has not edited the fee data, we set the fee values
                    // Otherwise, we keep the user's values
                    if (!userEditedFeeData) {
                        nonce = minNonce
                        gasLimit = estimationParams.estimate
                        maxPriorityFeePerGas =
                            Wei(BigInteger.valueOf(DEFAULT_MINER_TIP)).toGWei(activeSafe.chain.currency.decimals)
                        maxFeePerGas = Wei(gasPrice).toGWei(activeSafe.chain.currency.decimals)
                            .plus(maxPriorityFeePerGas!!)
                    }

                    updateState {
                        TxReviewState(viewAction = UpdateFee(fee = totalFee()))
                    }
                }
            }
        }
    }

    fun updateEstimationParams(
        nonce: BigInteger,
        gasLimit: BigInteger,
        maxPriorityFeePerGas: BigDecimal,
        maxFeePerGas: BigDecimal
    ) {
        this.userEditedFeeData = true
        this.nonce = nonce
        this.gasLimit = gasLimit
        this.maxPriorityFeePerGas = maxPriorityFeePerGas
        this.maxFeePerGas = maxFeePerGas
        safeLaunch {
            updateState {
                TxReviewState(viewAction = UpdateFee(fee = totalFee()))
            }
        }
    }

    fun signAndExecute() {
        safeLaunch {
            executionKey?.let {
                ethTx = ethTx!!.copy(nonce = nonce!!)
                ethTx!!.fee = Fee1559(
                    gas = gasLimit!!,
                    maxPriorityFee = Wei.fromGWei(maxPriorityFeePerGas!!).value,
                    maxFeePerGas = Wei.fromGWei(maxFeePerGas!!).value
                )
                val owner = credentialsRepository.owner(it.address)!!
                val ethTxHash = Sha3Utils.keccak(byteArrayOf(ethTx!!.type.toByte(), *ethTx!!.rlp()))
                when (owner.type) {
                    Owner.Type.IMPORTED, Owner.Type.GENERATED -> {
                        ethTxSignature = credentialsRepository.signWithOwner(owner, ethTxHash)
                        if (settingsHandler.usePasscode && settingsHandler.requirePasscodeForConfirmations) {
                            updateState {
                                TxReviewState(
                                    viewAction = ViewAction.NavigateTo(
                                        TxReviewFragmentDirections.actionTxReviewFragmentToEnterPasscodeFragment()
                                    )
                                )
                            }

                        } else {
                            sendForExecution()
                        }
                    }

                    Owner.Type.LEDGER_NANO_X -> {

                    }

                    Owner.Type.KEYSTONE -> {
                        updateState {
                            TxReviewState(
                                viewAction = ViewAction.NavigateTo(
                                    TxReviewFragmentDirections.actionTxReviewFragmentToKeystoneRequestSignatureFragment(
                                        SigningMode.EXECUTION,
                                        activeSafe.chain,
                                        it.address.asEthereumAddressString(),
                                        ethTxHash.toHexString(),
                                        false
                                    )
                                )
                            )
                        }
                        updateState {
                            TxReviewState(
                                viewAction = ViewAction.None
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setSignature(signatueString: String) {
        ethTxSignature = signatueString.toSignature()
        if (ethTxSignature!!.v <= 1) {
            ethTxSignature!!.v = (ethTxSignature!!.v + 27).toByte()

        }
    }

    fun resumeExecutionFlow(signatureString: String? = null) {
        safeLaunch {
            signatureString?.let {
                setSignature(it)
            }
            sendForExecution()
        }
    }

    fun sendForExecution() {
        safeLaunch {
            ethTxSignature?.let {
                rpcClient.send(ethTx!!, it)
                updateState {
                    TxReviewState(
                        viewAction =
                        ViewAction.NavigateTo(
                            TxReviewFragmentDirections.actionTxReviewFragmentToTxSuccessFragment()
                        )
                    )
                }
            }
        }
    }

    private fun balanceString(balance: BigInteger): String {
        return "${
            balanceFormatter.shortAmount(
                balance.convertAmount(
                    activeSafe.chain.currency.decimals
                )
            )
        } ${activeSafe.chain.currency.symbol}"
    }

    fun totalFee(): String? {
        return if (gasLimit != null && maxFeePerGas != null) {
            balanceString(gasLimit!! * Wei.fromGWei(maxFeePerGas!!).value)
        } else {
            null
        }
    }

    fun isChainPrefixPrependEnabled() = settingsHandler.chainPrefixPrepend

    fun isChainPrefixCopyEnabled() = settingsHandler.chainPrefixCopy

    companion object {
        const val DEFAULT_MINER_TIP = 1_500_000_000L
    }
}

data class TxReviewState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class DefaultKey(
    val key: OwnerViewData?
) : BaseStateViewModel.ViewAction

data class UpdateFee(
    val fee: String?
) : BaseStateViewModel.ViewAction
