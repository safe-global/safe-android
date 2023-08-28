package io.gnosis.data.repositories

import io.gnosis.data.backend.rpc.RpcClient
import io.gnosis.data.db.daos.TransactionLocalDao
import io.gnosis.data.models.Safe
import io.gnosis.data.models.TransactionLocal
import io.gnosis.data.models.transaction.TransactionStatus
import pm.gnosis.models.Transaction
import java.math.BigInteger

class TransactionLocalRepository(
    private val localTxDao: TransactionLocalDao,
    private val rpcClient: RpcClient
) {

    suspend fun saveLocally(tx: Transaction, txHash: String, safeTxHash: String, safeTxNonce: BigInteger) {
        // clean up old txs
        // only latest tx submitted for execution is relevant
        localTxDao.deleteAll()
        val localTx = TransactionLocal(
            safeAddress = tx.to,
            chainId = tx.chainId,
            safeTxNonce = safeTxNonce,
            safeTxHash = safeTxHash,
            ethTxHash = txHash,
            status = TransactionStatus.PENDING
        )
        localTxDao.save(localTx)
    }

    suspend fun save(localTx: TransactionLocal) {
        localTxDao.save(localTx)
    }

    suspend fun delete(localTx: TransactionLocal) {
        localTxDao.delete(localTx)
    }

    suspend fun getLocalTx(safe: Safe, safeTxHash: String): TransactionLocal? =
        localTxDao.loadyByEthTxHash(safe.chainId, safe.address, safeTxHash)

    suspend fun getLocalTxLatest(safe: Safe): TransactionLocal? =
        localTxDao.loadLatest(safe.chainId, safe.address)

    suspend fun updateLocalTx(safe: Safe, safeTxHash: String): TransactionLocal? {
        return getLocalTx(safe, safeTxHash)?.let {
            updateLocalTx(safe, it)
        }
    }

    suspend fun updateLocalTxLatest(safe: Safe): TransactionLocal? {
        return getLocalTxLatest(safe)?.let {
            updateLocalTx(safe, it)
        }
    }

    suspend fun updateLocalTx(safe: Safe, localTx: TransactionLocal): TransactionLocal? {
        var localTx = localTx
        kotlin.runCatching {
            rpcClient.getTransactionReceipt(safe.chain, localTx.ethTxHash)
        }.onSuccess { txReceipt ->
            localTx = localTx.copy(
                status = if (txReceipt.status == BigInteger.ONE) TransactionStatus.SUCCESS else TransactionStatus.FAILED
            )
            localTxDao.save(localTx)
        }.onFailure {
            // fail silently
        }
        return localTx
    }
}
