package pm.gnosis.android.app.accounts.models

import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import java.math.BigInteger

data class Transaction(val nonce: BigInteger, val gasPrice: BigInteger, val startGas: BigInteger,
                       val to: BigInteger, val value: BigInteger, val data: ByteArray,
                       val chainCode: Int = AccountsRepository.CHAIN_ID_ANY)