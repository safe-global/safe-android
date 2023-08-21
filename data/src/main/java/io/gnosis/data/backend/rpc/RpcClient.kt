package io.gnosis.data.backend.rpc

import io.gnosis.contracts.GnosisSafeL2_v1_3_0
import io.gnosis.contracts.GnosisSafe_v1_0_0
import io.gnosis.contracts.GnosisSafe_v1_1_1
import io.gnosis.contracts.GnosisSafe_v1_2_0
import io.gnosis.contracts.GnosisSafe_v1_3_0
import io.gnosis.data.backend.rpc.models.EstimationParams
import io.gnosis.data.models.Chain
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TxData
import io.gnosis.data.utils.SemVer
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.ethereum.BulkRequest
import pm.gnosis.ethereum.EthBalance
import pm.gnosis.ethereum.EthCallEip1559
import pm.gnosis.ethereum.EthEstimateGasEip1559
import pm.gnosis.ethereum.EthGasPrice
import pm.gnosis.ethereum.EthGetTransactionCount
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.model.Solidity
import pm.gnosis.models.Fee1559
import pm.gnosis.models.TransactionEip1559
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.utils.rlp
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.removeHexPrefix
import pm.gnosis.utils.toHexString
import java.math.BigInteger

class RpcClient(
    private val ethereumRepository: EthereumRepository
) {

    fun updateRpcUrl(chain: Chain) {
        ethereumRepository.rpcUrl = chain.rpcUri
    }

    suspend fun gasPrice(): BigInteger? {
        val response = ethereumRepository.request(
            EthGasPrice()
        )
        return response.result()
    }

    suspend fun getBalance(address: Solidity.Address): Wei? {
        return ethereumRepository.request(
            EthBalance(address)
        ).checkedResult("Could not retrieve balance")
    }

    suspend fun getBalances(addresses: List<Solidity.Address>): List<Wei?> {
        val requests = addresses.mapIndexed { index, address ->
            EthBalance(address = address, id = index)
        }
        val responses = ethereumRepository.request(
            BulkRequest(requests)
        ).let {
            requests.map {
                it.checkedResult("Could not retrieve balance")
            }
        }
        return responses
    }

    fun ethTransaction(
        safe: Safe,
        executionKey: Solidity.Address,
        txData: TxData,
        executionInfo: DetailedExecutionInfo.MultisigExecutionDetails,
        minerTip: BigInteger
    ): TransactionEip1559 {
        val chain = safe.chain
        val safeVersion = SemVer.parse(safe.version!!)

        val signatures = executionInfo.confirmations
            .sortedBy { it.signer.value.value }
            .joinToString("") { it.signature.removeHexPrefix() }
            .hexToByteArray()

        val input = when {
            // is L2 contract
            chain.l2 && safeVersion >= SemVer(1, 3, 0) -> {
                GnosisSafeL2_v1_3_0.ExecTransaction.encode(
                    txData.to.value,
                    Solidity.UInt256(txData.value ?: BigInteger.ZERO),
                    Solidity.Bytes(txData.hexData?.hexToByteArray() ?: byteArrayOf()),
                    Solidity.UInt8(BigInteger.valueOf(txData.operation.id.toLong())),
                    Solidity.UInt256(executionInfo.safeTxGas),
                    Solidity.UInt256(executionInfo.baseGas),
                    Solidity.UInt256(executionInfo.gasPrice),
                    executionInfo.gasToken,
                    executionInfo.refundReceiver?.value ?: Solidity.Address(BigInteger.ZERO),
                    Solidity.Bytes(signatures)
                )
            }
            // ? ..< 1.1.1
            safeVersion < SemVer(1, 1, 1) -> {
                GnosisSafe_v1_0_0.ExecTransaction.encode(
                    txData.to.value,
                    Solidity.UInt256(txData.value ?: BigInteger.ZERO),
                    Solidity.Bytes(txData.hexData?.hexToByteArray() ?: byteArrayOf()),
                    Solidity.UInt8(BigInteger.valueOf(txData.operation.id.toLong())),
                    Solidity.UInt256(executionInfo.safeTxGas),
                    Solidity.UInt256(executionInfo.baseGas),
                    Solidity.UInt256(executionInfo.gasPrice),
                    executionInfo.gasToken,
                    executionInfo.refundReceiver?.value ?: Solidity.Address(BigInteger.ZERO),
                    Solidity.Bytes(signatures)
                )
            }
            // 1.1.1 ..< 1.2.0
            safeVersion < SemVer(1, 2, 0) -> {
                GnosisSafe_v1_1_1.ExecTransaction.encode(
                    txData.to.value,
                    Solidity.UInt256(txData.value ?: BigInteger.ZERO),
                    Solidity.Bytes(txData.hexData?.hexToByteArray() ?: byteArrayOf()),
                    Solidity.UInt8(BigInteger.valueOf(txData.operation.id.toLong())),
                    Solidity.UInt256(executionInfo.safeTxGas),
                    Solidity.UInt256(executionInfo.baseGas),
                    Solidity.UInt256(executionInfo.gasPrice),
                    executionInfo.gasToken,
                    executionInfo.refundReceiver?.value ?: Solidity.Address(BigInteger.ZERO),
                    Solidity.Bytes(signatures)
                )
            }
            // 1.2.0 ..< 1.3.0
            safeVersion < SemVer(1, 3, 0) -> {
                GnosisSafe_v1_2_0.ExecTransaction.encode(
                    txData.to.value,
                    Solidity.UInt256(txData.value ?: BigInteger.ZERO),
                    Solidity.Bytes(txData.hexData?.hexToByteArray() ?: byteArrayOf()),
                    Solidity.UInt8(BigInteger.valueOf(txData.operation.id.toLong())),
                    Solidity.UInt256(executionInfo.safeTxGas),
                    Solidity.UInt256(executionInfo.baseGas),
                    Solidity.UInt256(executionInfo.gasPrice),
                    executionInfo.gasToken,
                    executionInfo.refundReceiver?.value ?: Solidity.Address(BigInteger.ZERO),
                    Solidity.Bytes(signatures)
                )
            }
            // >= 1.3.0
            else -> {
                GnosisSafe_v1_3_0.ExecTransaction.encode(
                    txData.to.value,
                    Solidity.UInt256(txData.value ?: BigInteger.ZERO),
                    Solidity.Bytes(txData.hexData?.hexToByteArray() ?: byteArrayOf()),
                    Solidity.UInt8(BigInteger.valueOf(txData.operation.id.toLong())),
                    Solidity.UInt256(executionInfo.safeTxGas),
                    Solidity.UInt256(executionInfo.baseGas),
                    Solidity.UInt256(executionInfo.gasPrice),
                    executionInfo.gasToken,
                    executionInfo.refundReceiver?.value ?: Solidity.Address(BigInteger.ZERO),
                    Solidity.Bytes(signatures)
                )
            }
        }

//        return if (chain.features.contains(Chain.Feature.EIP1559)) {
        return  TransactionEip1559(
                chainId = chain.chainId,
                from = executionKey,
                to = txData.to.value,
                data = input,
                fee = Fee1559(maxPriorityFee = minerTip)
            )
//        } else {
//
//        }
    }

    suspend fun estimate(chain: Chain, tx: TransactionEip1559): EstimationParams {
        // remove the fee (it will be estimated)
        tx.fee = Fee1559()

        val gasPriceRequest = EthGasPrice(id = 1)
        val balanceRequest = EthBalance(address = tx.from!!, id = 2)
        val nonceRequest = EthGetTransactionCount(from = tx.from!!, id = 3)
        val callRequest = EthCallEip1559(from = tx.from, transaction = tx, id = 4)
        val estimateRequest = EthEstimateGasEip1559(from = tx.from, transaction = tx, id = 5)

        ethereumRepository.request(
            BulkRequest(
                listOf(
                    gasPriceRequest,
                    balanceRequest,
                    nonceRequest,
                    callRequest,
                    estimateRequest
                )
            )
        )

        val gasPrice = gasPriceRequest.checkedResult("Failed to get gas price")
        val banance = balanceRequest.checkedResult("Failed to get balance")
        val nonce = nonceRequest.checkedResult("Failed to get nonce")
        val callSuccess = callRequest.checkedResult("Failed to call transaction")
        val estimate = estimateRequest.checkedResult("Failed to estimate transaction")

        return EstimationParams(
            gasPrice = gasPrice,
            balance = banance.value,
            nonce = nonce,
            callSuccess = BigInteger(callSuccess.removeHexPrefix()) == BigInteger.ONE,
            estimate = estimate
        )
    }

    suspend fun send(tx: TransactionEip1559, signature: ECDSASignature) {
        val rawTxData = byteArrayOf(tx.type.toByte(), *tx.rlp(signature))
        ethereumRepository.sendRawTransaction(rawTxData.toHexString().addHexPrefix())
    }
}
