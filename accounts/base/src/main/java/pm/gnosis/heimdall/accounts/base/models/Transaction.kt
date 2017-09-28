package pm.gnosis.heimdall.accounts.base.models

import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import java.math.BigDecimal
import java.math.BigInteger

data class Transaction(val nonce: BigInteger, val gasPrice: BigInteger, private val startGas: BigInteger,
                       val to: BigInteger, val value: BigInteger, val data: ByteArray,
                       val chainId: Int = AccountsRepository.CHAIN_ID_ANY) {
    val adjustedStartGas: BigInteger by lazy {
        BigDecimal.valueOf(1.1).multiply(BigDecimal(startGas)).setScale(0, BigDecimal.ROUND_UP).unscaledValue()
    }
}
