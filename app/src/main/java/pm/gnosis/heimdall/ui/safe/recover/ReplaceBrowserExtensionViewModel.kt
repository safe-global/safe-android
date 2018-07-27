package pm.gnosis.heimdall.ui.safe.recover

import io.reactivex.Observable
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.mapToResult
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReplaceBrowserExtensionViewModel @Inject constructor(
    private val ethereumRepository: EthereumRepository,
    private val gnosisSafeRepository: GnosisSafeRepository,
    private val pushServiceRepository: PushServiceRepository,
    private val transactionExecutionRepository: TransactionExecutionRepository
) : ReplaceBrowserExtensionContract() {
    private lateinit var safeTransaction: SafeTransaction
    private lateinit var signatures: Map<Solidity.Address, Signature>
    private lateinit var txGas: BigInteger
    private lateinit var dataGas: BigInteger
    private lateinit var gasPrice: BigInteger
    private lateinit var newChromeExtension: Solidity.Address

    override fun setup(
        safeTransaction: SafeTransaction,
        signature1: Pair<Solidity.Address, Signature>,
        signature2: Pair<Solidity.Address, Signature>,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        newChromeExtension: Solidity.Address
    ) {
        this.safeTransaction = safeTransaction
        this.signatures = mapOf(signature1, signature2)
        this.txGas = txGas
        this.dataGas = dataGas
        this.gasPrice = gasPrice
        this.newChromeExtension = newChromeExtension
    }

    override fun getMaxTransactionFee() = Wei((txGas + dataGas) * gasPrice)

    override fun observeSafeBalance(): Observable<Wei> =
        Observable.interval(5, TimeUnit.SECONDS)
            .concatMap { ethereumRepository.getBalance(safeTransaction.wrapped.address) }
            .retry()

    override fun getSafeTransaction() = safeTransaction

    override fun loadSafe() = gnosisSafeRepository.loadSafe(safeTransaction.wrapped.address)

    override fun submitTransaction() =
        transactionExecutionRepository.submit(
            safeAddress = safeTransaction.wrapped.address,
            transaction = safeTransaction,
            signatures = signatures,
            senderIsOwner = false,
            txGas = txGas,
            dataGas = dataGas,
            gasPrice = gasPrice,
            addToHistory = true
        )
            // TODO: For correctness this should be done when the transaction is mined
            .flatMapCompletable { pushServiceRepository.propagateSafeCreation(safeTransaction.wrapped.address, setOf(newChromeExtension)) }
            .mapToResult()
}
