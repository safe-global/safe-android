package pm.gnosis.android.app.wallet.data.tokens

import pm.gnosis.android.app.wallet.util.decimalAsBigInteger
import pm.gnosis.android.app.wallet.util.hexAsBigInteger

object BasicAttentionToken : Token {
    override val symbol = "BAT"
    override val name = "FirstBlood Token"
    override val decimals = "18".decimalAsBigInteger()
    override val contractAddress = "0x0D8775F648430679A709E98d2b0Cb6250d2887EF".hexAsBigInteger()
}
