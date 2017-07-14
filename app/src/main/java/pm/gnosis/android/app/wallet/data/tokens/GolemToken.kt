package pm.gnosis.android.app.wallet.data.tokens

import pm.gnosis.android.app.wallet.util.decimalAsBigInteger
import pm.gnosis.android.app.wallet.util.hexAsBigInteger

object GolemToken : Token {
    override val symbol = "GNT"
    override val name = "GolemToken Network Token"
    override val decimals = "18".decimalAsBigInteger()
    override val contractAddress = "0xa74476443119A942dE498590Fe1f2454d7D4aC0d".hexAsBigInteger()
}
