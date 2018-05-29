package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.fromDb
import pm.gnosis.heimdall.data.remote.models.GnosisSafeTransactionDescription
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfo
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.models.Transaction
import pm.gnosis.utils.isSolidityMethod
import pm.gnosis.utils.removeSolidityMethodPrefix
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DefaultTransactionInfoRepository @Inject constructor(
    appDb: ApplicationDb
) : TransactionInfoRepository {

    private val descriptionsDao = appDb.descriptionsDao()

    override fun parseTransactionData(transaction: SafeTransaction): Single<TransactionData> =
        Single.fromCallable {
            val tx = transaction.wrapped
            val data = tx.data
            when {

                data.isNullOrBlank() -> // If we have no data we default to ether transfer
                    TransactionData.AssetTransfer(ERC20Token.ETHER_TOKEN.address, tx.value?.value ?: BigInteger.ZERO, tx.address)
                data?.isSolidityMethod(ERC20Contract.Transfer.METHOD_ID) == true ->
                        parseTokenTransfer(tx)
                else ->
                    TransactionData.Generic(tx.address, tx.value?.value ?: BigInteger.ZERO, tx.data)
            }
        }
            .subscribeOn(Schedulers.io())

    private fun parseTokenTransfer(transaction: Transaction): TransactionData.AssetTransfer {
        val arguments = transaction.data!!.removeSolidityMethodPrefix(ERC20Contract.Transfer.METHOD_ID)
        return ERC20Contract.Transfer.decodeArguments(arguments).let { TransactionData.AssetTransfer(transaction.address, it._value.value, it._to) }
    }

    override fun loadTransactionInfo(id: String): Single<TransactionInfo> =
        descriptionsDao.loadDescription(id)
            .subscribeOn(Schedulers.io())
            .flatMap { info ->
                parseTransactionData(info.fromDb().toTransaction())
                    .map { TransactionInfo(id, info.safeAddress, it, info.submittedAt) }
            }

    private fun GnosisSafeTransactionDescription.toTransaction(): SafeTransaction =
        SafeTransaction(
            Transaction(to, value = value, data = data, nonce = nonce),
            TransactionExecutionRepository.Operation.values()[operation.toInt()]
        )

}
