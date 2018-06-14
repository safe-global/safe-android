package pm.gnosis.heimdall.data.remote.impls

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.GnosisSafePersonalEdition
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.*
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.utils.*
import java.math.BigInteger

class LocalRelayServiceApi(
    private val accountsRepository: AccountsRepository,
    private val ethereumRepository: EthereumRepository,
    private val remoteService: RelayServiceApi
) : RelayServiceApi {
    override fun execute(params: ExecuteParams): Single<RelayExecution> =
        Single.fromCallable {
            val signatureBytes = StringBuilder().apply {
                params.signatures.forEach {
                    append(it.r.toString(16).padStart(64, '0').substring(0, 64))
                    append(it.s.toString(16).padStart(64, '0').substring(0, 64))
                    append(it.v.toString(16).padStart(2, '0'))
                }
            }.toString().hexStringToByteArray()

            val to = params.to.asEthereumAddress()!!
            val value = Solidity.UInt256(params.value.decimalAsBigInteger())
            val data = Solidity.Bytes(params.data.hexStringToByteArrayOrNull() ?: ByteArray(0))
            val operation = Solidity.UInt8(params.operation.toBigInteger())
            val txGas = params.safeTxGas.decimalAsBigInteger()
            val dataGas = params.dataGas.decimalAsBigInteger()
            val gasPrice = params.gasPrice.decimalAsBigInteger()
            val executionData = GnosisSafePersonalEdition.ExecTransactionAndPaySubmitter.encode(
                to, value, data, operation,
                Solidity.UInt256(txGas), Solidity.UInt256(dataGas),
                Solidity.UInt256(gasPrice), Solidity.Address(BigInteger.ZERO),
                Solidity.Bytes(signatureBytes)
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
                val gasPrice = BigInteger("20000000000")
                val txGas = it.result()!!.substring(138).hexAsBigInteger() + BigInteger.valueOf(10000)
                val dataGas = calculateDataGas(params, txGas, gasPrice) + BigInteger.valueOf(32000)
                RelayEstimate(txGas.asDecimalString(), dataGas.asDecimalString(), gasPrice.asDecimalString())
            }
    }

    override fun safeCreation(params: RelaySafeCreationParams): Single<RelaySafeCreation> =
        remoteService.safeCreation(params)

    override fun notifySafeFunded(address: String): Completable =
        remoteService.notifySafeFunded(address)

    override fun safeFundStatus(address: String): Single<RelaySafeFundStatus> =
        remoteService.safeFundStatus(address)

    private fun calculateDataGas(params: EstimateParams, txGas: BigInteger, gasPrice: BigInteger): BigInteger {
        val signatureCosts = params.threshold * (68 + 2176 + 2176)
        val to = params.to.asEthereumAddress()!!
        val value = Solidity.UInt256(params.value.decimalAsBigInteger())
        val data = Solidity.Bytes(params.data.hexStringToByteArrayOrNull() ?: ByteArray(0))
        val operation = Solidity.UInt8(params.operation.toBigInteger())
        val payload = GnosisSafePersonalEdition.ExecTransactionAndPaySubmitter.encode(
            to, value, data, operation,
            Solidity.UInt256(txGas), Solidity.UInt256(BigInteger.ZERO),
            Solidity.UInt256(gasPrice), Solidity.Address(BigInteger.ZERO),
            Solidity.Bytes(byteArrayOf())
        )
        val dataGasEstimate = payload.chunked(2).fold(0L) { acc, part -> acc + dataGasValue(part) } + signatureCosts
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
