package pm.gnosis.android.app.wallet.data.tokens

import java.math.BigInteger

interface Token {
    val symbol: String
    val name: String
    val decimals: BigInteger
    val contractAddress: BigInteger
}
