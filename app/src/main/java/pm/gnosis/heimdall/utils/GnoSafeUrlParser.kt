package pm.gnosis.heimdall.utils

import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.nullOnThrow
import java.math.BigInteger

object GnoSafeUrlParser {
    private const val GNOSIS_SAFE_SCHEMA = "gnosafe://"
    private const val SIGN_REQUEST = "sign_req"
    private const val SIGN_RESPONSE = "sign_res"
    private const val KEY_TO = "to"
    private const val KEY_SAFE = "safe"
    private const val KEY_VALUE = "value"
    private const val KEY_DATA = "data"
    private const val KEY_NONCE = "nonce"
    private const val KEY_OPERATION = "nonce"

    fun signRequest(
        hash: String,
        safe: Solidity.Address,
        to: Solidity.Address,
        value: Wei?,
        data: String?,
        nonce: BigInteger,
        operation: Int
    ): String =
        "$GNOSIS_SAFE_SCHEMA$SIGN_REQUEST/$hash?" +
                "$KEY_SAFE=${safe.asEthereumAddressString()}&" +
                "$KEY_TO=${to.asEthereumAddressString()}&" +
                (value?.value?.let { "$KEY_VALUE=${it.toString(16)}&" } ?: "") +
                (data?.let { "$KEY_DATA=$it&" } ?: "") +
                "$KEY_NONCE=${nonce.toString(16)}" +
                "$KEY_OPERATION=$operation"

    fun signResponse(signature: Signature): String =
        "$GNOSIS_SAFE_SCHEMA$SIGN_RESPONSE/$signature"

    fun parse(url: String): Parsed? {
        if (!url.startsWith(GNOSIS_SAFE_SCHEMA, true)) {
            return null
        }
        val parts = url.removePrefix(GNOSIS_SAFE_SCHEMA).split("?", limit = 2)
        val path = parts.first().split("/")
        return when (path.firstOrNull()) {
            SIGN_REQUEST -> nullOnThrow {
                val params = paramsAsMap(parts.getOrNull(1))
                val transaction = Transaction(
                    address = params[KEY_TO]!!.asEthereumAddress()!!,
                    value = params[KEY_VALUE]?.hexAsBigInteger()?.let { Wei(it) },
                    data = params[KEY_DATA],
                    nonce = params[KEY_NONCE]!!.hexAsBigInteger()
                )
                val operation = params[KEY_OPERATION]?.toIntOrNull() ?: 0
                Parsed.SignRequest(
                    path[1],
                    params[KEY_SAFE]!!.asEthereumAddress()!!,
                    transaction,
                    operation
                )

            }
            SIGN_RESPONSE -> nullOnThrow {
                Parsed.SignResponse(Signature.from(path.last()))
            }
            else -> null
        }
    }

    private fun paramsAsMap(query: String?): Map<String, String> =
        query?.let {
            it.split("&")
                .map { it.split("=", limit = 2) }
                .map { it.first() to it.last() }
                .toMap()
        } ?: emptyMap()

    sealed class Parsed {
        data class SignRequest(val transactionHash: String, val safe: Solidity.Address, val transaction: Transaction, val operation: Int) : Parsed()
        data class SignResponse(val signature: Signature) : Parsed()
    }
}
