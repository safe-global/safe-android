package pm.gnosis.heimdall.helpers

import io.reactivex.Completable
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTransactionTriggerManager @Inject constructor(
    private val pushServiceRepository: PushServiceRepository,
    private val transactionExecutionRepository: TransactionExecutionRepository,
    private val transactionInfoRepository: TransactionInfoRepository
) : TransactionTriggerManager, TransactionExecutionRepository.TransactionEventsCallback {

    override fun init() {
        transactionExecutionRepository.addTransactionEventsCallback(this)
    }

    override fun onTransactionSubmitted(safeAddress: Solidity.Address, transaction: SafeTransaction, chainHash: String, referenceId: Long?) {
        transactionInfoRepository.parseTransactionData(transaction)
            .flatMapCompletable {
                when (it) {
                    is TransactionData.ConnectAuthenticator -> notifySafeCreated(safeAddress, it.extension)
                    else -> Completable.complete()
                }
            }
            .subscribe()
    }

    override fun onTransactionRejected(referenceId: Long) {}

    private fun notifySafeCreated(safeAddress: Solidity.Address, target: Solidity.Address): Completable =
            pushServiceRepository.propagateSafeCreation(safeAddress, setOf(target))

}

interface TransactionTriggerManager {
    fun init()
}
