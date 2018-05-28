package pm.gnosis.heimdall.data.remote.impls

import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.GnosisSafePersonalEdition
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.EstimateParams
import pm.gnosis.heimdall.data.remote.models.ExecuteParams
import pm.gnosis.heimdall.data.remote.models.RelayEstimate
import pm.gnosis.heimdall.data.remote.models.RelayExecution
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.utils.*
import java.math.BigInteger


class LocalRelayServiceApi(
    private val accountsRepository: AccountsRepository,
    private val ethereumRepository: EthereumRepository
) : RelayServiceApi {
    override fun execute(params: ExecuteParams): Single<RelayExecution> =
        Single.fromCallable {
            val vList = mutableListOf<Solidity.UInt8>()
            val rList = mutableListOf<Solidity.Bytes32>()
            val sList = mutableListOf<Solidity.Bytes32>()
            params.signatures.forEach {
                vList.add(Solidity.UInt8(BigInteger.valueOf(it.v.toLong())))
                rList.add(Solidity.Bytes32(it.r.decimalAsBigInteger().toBytes(32)))
                sList.add(Solidity.Bytes32(it.s.decimalAsBigInteger().toBytes(32)))
            }

            val to = params.to.asEthereumAddress()!!
            val value = Solidity.UInt256(params.value.decimalAsBigInteger())
            val data = Solidity.Bytes(params.data.hexStringToByteArrayOrNull() ?: ByteArray(0))
            val operation = Solidity.UInt8(params.operation.toBigInteger())
            val txGas = params.safeTxGas.decimalAsBigInteger()
            val dataGas = params.dataGas.decimalAsBigInteger()
            val gasPrice = params.gasPrice.decimalAsBigInteger()
            val executionData = GnosisSafePersonalEdition.ExecAndPayTransaction.encode(
                to, value, data, operation,
                Solidity.UInt256(txGas), Solidity.UInt256(dataGas),
                Solidity.UInt256(gasPrice), Solidity.Address(BigInteger.ZERO),
                SolidityBase.Vector(vList), SolidityBase.Vector(rList), SolidityBase.Vector(sList)
            )
            Transaction(params.safe.asEthereumAddress()!!, data = executionData)
        }
            .flatMap(::loadExecutionParams)
            .flatMap(accountsRepository::signTransaction)
            .flatMapObservable(ethereumRepository::sendRawTransaction)
            .firstOrError()
            .map(::RelayExecution)

    private fun loadExecutionParams(transaction: Transaction) =
        accountsRepository.loadActiveAccount()
            .flatMap {
                ethereumRepository.request(
                    ExecutionInfoRequest(
                        EthGetTransactionCount(it.address, id = 0),
                        EthEstimateGas(it.address, transaction, id = 1)
                    )
                )
                    .firstOrError()
                    .map {
                        val gasPrice = BigInteger("10000000000")
                        val gasLimit = it.estimate.result()!! * BigInteger.valueOf(2)
                        transaction.copy(nonce = it.nonce.result()!!, gas = gasLimit, gasPrice = gasPrice)
                    }
            }

    override fun estimate(params: EstimateParams): Single<RelayEstimate> {
        return Observable.fromCallable {
            val to = params.to.asEthereumAddress()!!
            val value = Solidity.UInt256(params.value.decimalAsBigInteger())
            val data = Solidity.Bytes(params.data.hexStringToByteArrayOrNull() ?: ByteArray(0))
            val operation = Solidity.UInt8(params.operation.toBigInteger())
            val estimateData = GnosisSafePersonalEdition.RequiredTxGas.encode(to, value, data, operation)
            Transaction(params.safe.asEthereumAddress()!!, data = estimateData)
        }.flatMap {
            ethereumRepository.request(
                EthCall(transaction = it, id = 2, from = params.safe.asEthereumAddress()!!)
            )
        }
            .firstOrError()
            .map {
                val gasPrice = BigInteger.valueOf(20000000000)
                val txGas = it.result()!!.substring(138).hexAsBigInteger()
                val dataGas = calculateDataGas(params, txGas, gasPrice)
                RelayEstimate(txGas.asDecimalString(), dataGas.asDecimalString(), gasPrice.asDecimalString())
            }
    }

    private fun calculateDataGas(params: EstimateParams, txGas: BigInteger, gasPrice: BigInteger): BigInteger {
        val signateCosts = 3L * (64 + 64) + params.threshold * (192 + 2176 + 2176)
        val to = params.to.asEthereumAddress()!!
        val value = Solidity.UInt256(params.value.decimalAsBigInteger())
        val data = Solidity.Bytes(params.data.hexStringToByteArrayOrNull() ?: ByteArray(0))
        val operation = Solidity.UInt8(params.operation.toBigInteger())
        val payload = GnosisSafePersonalEdition.ExecAndPayTransaction.encode(
            to, value, data, operation,
            Solidity.UInt256(txGas), Solidity.UInt256(BigInteger.ZERO),
            Solidity.UInt256(gasPrice), Solidity.Address(BigInteger.ZERO),
            SolidityBase.Vector(emptyList()), SolidityBase.Vector(emptyList()), SolidityBase.Vector(emptyList())
        )
        val dataGasEstimate = payload.chunked(2).fold(0L, { acc, part -> acc + dataGasValue(part) }) + signateCosts
        return (dataGasEstimate + (if (dataGasEstimate > 65536) 128 else 64)).toBigInteger()
    }

    private fun dataGasValue(hexValue: String) =
        when (hexValue) {
            "0x" -> 0L
            "00" -> 4L
            else -> 68L
        }

    private class ExecutionInfoRequest(
        val nonce: EthGetTransactionCount,
        val estimate: EthEstimateGas
    ) : BulkRequest(nonce, estimate)
}
