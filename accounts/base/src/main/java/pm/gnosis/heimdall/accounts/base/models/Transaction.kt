package pm.gnosis.heimdall.accounts.base.models

import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

data class Transaction(val nonce: BigInteger, val gasPrice: BigInteger, private val startGas: BigInteger,
                       val to: BigInteger, val value: BigInteger, val data: ByteArray,
                       val chainId: Int = AccountsRepository.CHAIN_ID_ANY) {
    val adjustedStartGas: BigInteger by lazy {
        BigDecimal.valueOf(1.1).multiply(BigDecimal(startGas)).setScale(0, BigDecimal.ROUND_UP).unscaledValue()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Transaction

        if (nonce != other.nonce) return false
        if (gasPrice != other.gasPrice) return false
        if (startGas != other.startGas) return false
        if (to != other.to) return false
        if (value != other.value) return false
        if (!Arrays.equals(data, other.data)) return false
        if (chainId != other.chainId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nonce.hashCode()
        result = 31 * result + gasPrice.hashCode()
        result = 31 * result + startGas.hashCode()
        result = 31 * result + to.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + Arrays.hashCode(data)
        result = 31 * result + chainId
        return result
    }
}
