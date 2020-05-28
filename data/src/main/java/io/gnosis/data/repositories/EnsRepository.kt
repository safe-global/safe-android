package io.gnosis.data.repositories

import io.gnosis.contracts.BuildConfig
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.ethereum.Block
import pm.gnosis.ethereum.EthCall
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.toHexString
import java.net.IDN
import java.util.*

class EnsRepository(
    private val ensNormalizer: EnsNormalizer,
    private val ethereumRepository: EthereumRepository
) {

    suspend fun resolve(url: String): Solidity.Address? {
        val node = ensNormalizer.normalize(url).nameHash()

        val resolverAddress = ethereumRepository.request(
            EthCall(
                block = Block.LATEST,
                transaction = Transaction(
                    address = ENS_ADDRESS,
                    data = GET_RESOLVER + node.toHexString()
                )
            )
        ).checkedResult("ENS resolver address request failure").asEthereumAddress()!!

        return ethereumRepository.request(
            EthCall(
                block = Block.LATEST,
                transaction = Transaction(
                    address = resolverAddress,
                    data = GET_ADDRESS + node.toHexString()
                )
            )
        ).checkedResult("ENS address request failure").asEthereumAddress()
    }

    private fun String.nameHash(): ByteArray {
        return this.split(".").foldRight<String, ByteArray?>(null) { part, node ->
            if (node == null && part.isEmpty()) ByteArray(32)
            else Sha3Utils.keccak((node ?: ByteArray(32)) + Sha3Utils.keccak(part.toByteArray()))
        } ?: ByteArray(32)
    }

    companion object {

        val ENS_ADDRESS = BuildConfig.ENS_REGISTRY.asEthereumAddress()!!

        /*
        contract ENS {
            function resolver(bytes32 node) constant returns (Resolver);
        }
         */
        private const val GET_ADDRESS = "0x3b3b57de" // addr(bytes 32 node)

        /*
        contract Resolver {
            function addr(bytes32 node) constant returns (address);
        }
         */
        private const val GET_RESOLVER = "0x0178b8bf" // resolver(bytes32 node)
        private const val GET_NAME = "0x691f3431" // name(bytes32)
    }
}

interface EnsNormalizer {
    fun normalize(name: String): String
}

class IDNEnsNormalizer : EnsNormalizer {

    override fun normalize(name: String) = IDN.toASCII(name, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.getDefault())
}
