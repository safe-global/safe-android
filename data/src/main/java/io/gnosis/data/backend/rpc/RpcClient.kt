package io.gnosis.data.backend.rpc

import pm.gnosis.ethereum.BulkRequest
import pm.gnosis.ethereum.EthBalance
import pm.gnosis.ethereum.EthGasPrice
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import java.math.BigInteger

class RpcClient(
    private val ethereumRepository: EthereumRepository
) {

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
}
