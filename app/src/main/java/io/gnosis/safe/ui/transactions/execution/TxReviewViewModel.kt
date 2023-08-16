package io.gnosis.safe.ui.transactions.execution

import androidx.annotation.VisibleForTesting
import io.gnosis.data.backend.rpc.RpcClient
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TxData
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.owner.list.OwnerViewData
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.model.Solidity
import pm.gnosis.models.Fee1559
import pm.gnosis.models.TransactionEip1559
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.utils.rlp
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

    private var executionKey: OwnerViewData? = null

    private var userEditedFeeData: Boolean = false

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

    private var ethTx: TransactionEip1559? = null

    private var ethTxSignature: ECDSASignature? = null

    init {
        safeLaunch {
            activeSafe = safeRepository.getActiveSafe()!!
            loadDefaultKey()
        }
    }

    override fun initialState() = TxReviewState(viewAction = null)

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
                    TxReviewState(viewAction = DefaultKey(executionKey))
                }
                executionKey?.let {
                    updateDefaultKeyBalance()
                }
            }
        }
    }

    fun updateDefaultKey(address: Solidity.Address) {
        safeLaunch {
            address?.let {
                val owner = credentialsRepository.owner(it)!!
                executionKey = OwnerViewData(owner.address, owner.name, owner.type)
                updateState {
                    TxReviewState(viewAction = DefaultKey(executionKey))
                }
                updateDefaultKeyBalance()
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

    fun estimate(txData: TxData, executionInfo: DetailedExecutionInfo) {
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
                        executionInfo,
                        BigInteger.valueOf(DEFAULT_MINER_TIP)
                    )
                    val estimationParams = rpcClient.estimate(activeSafe.chain, ethTx!!)

                    executionKey = executionKey!!.copy(
                        balance = balanceString(estimationParams.balance),
                        zeroBalance = estimationParams.gasPrice == BigInteger.ZERO
                    )

                    updateState {
                        TxReviewState(viewAction = DefaultKey(executionKey))
                    }

                    val gasPrice = estimationParams.gasPrice
                    minNonce = estimationParams.nonce
                    if (!userEditedFeeData) {
                        nonce = minNonce
                        gasLimit = estimationParams.estimate
                        maxPriorityFeePerGas = Wei(BigInteger.valueOf(DEFAULT_MINER_TIP)).toGWei(activeSafe.chain.currency.decimals)
                        maxFeePerGas = Wei(gasPrice).toGWei(activeSafe.chain.currency.decimals).plus(maxPriorityFeePerGas!!)
                    }

                    val totalFee = balanceString(gasLimit!! * Wei.fromGWei(maxFeePerGas!!).value)

                    updateState {
                        TxReviewState(viewAction = UpdateFee(totalFee))
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
        val totalFee = balanceString(gasLimit * Wei.fromGWei(maxFeePerGas).value)
        safeLaunch {
            updateState {
                TxReviewState(viewAction = UpdateFee(totalFee))
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
                when (owner.type) {
                    Owner.Type.IMPORTED, Owner.Type.GENERATED -> {
                        val ethTxHash = Sha3Utils.keccak(byteArrayOf(ethTx!!.type.toByte(), *ethTx!!.rlp()))
                        ethTxSignature = credentialsRepository.signWithOwner(owner, ethTxHash)
                        sendForExecution()
                    }

                    Owner.Type.LEDGER_NANO_X -> {

                    }

                    Owner.Type.KEYSTONE -> {

                    }
                }
            }
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
