package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Single
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.ethereum.EthCall
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.repositories.EnsRepository
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.toHexString
import java.net.IDN
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// TODO Maybe move to Svalinn
@Singleton
class DefaultEnsRepository @Inject constructor(
    private val ensNormalizer: EnsNormalizer,
    private val ethereumRepository: EthereumRepository
) : EnsRepository {
    override fun resolve(url: String): Single<Solidity.Address> =
        Single.fromCallable {
            ensNormalizer.normalize(url).split(".").foldRight<String, ByteArray?>(null) { part, node ->
                if (node == null && part.isEmpty()) ByteArray(32)
                else Sha3Utils.keccak((node ?: ByteArray(32)) + Sha3Utils.keccak(part.toByteArray()))
            } ?: ByteArray(32)
        }.flatMap { node ->
            val registerData = GET_RESOLVER + node.toHexString()
            ethereumRepository.request(EthCall(transaction = Transaction(address = ENS_ADDRESS, data = registerData))).firstOrError()
                .map { it to node }
        }.flatMap { (resp, node) ->
            val resolverAddress = resp.result()!!.asEthereumAddress()!!
            val resolverData = GET_ADDRESS + node.toHexString()
            ethereumRepository.request(EthCall(transaction = Transaction(address = resolverAddress, data = resolverData))).firstOrError()
        }.map { resp ->
            resp.result()!!.asEthereumAddress()!!
        }

    companion object {
        private val ENS_ADDRESS = BuildConfig.ENS_REGISTRY.asEthereumAddress()!!
        private const val GET_ADDRESS = "0x3b3b57de"
        private const val GET_RESOLVER = "0x0178b8bf"
    }
}

interface EnsNormalizer {
    fun normalize(name: String): String
}

@Singleton
class IDNEnsNormalizer @Inject constructor() : EnsNormalizer {
    override fun normalize(name: String) = IDN.toASCII(name, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.getDefault())
}
