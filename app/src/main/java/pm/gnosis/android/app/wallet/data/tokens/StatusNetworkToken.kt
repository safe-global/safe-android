package pm.gnosis.android.app.wallet.data.tokens

import pm.gnosis.android.app.wallet.util.decimalAsBigInteger
import pm.gnosis.android.app.wallet.util.hexAsBigInteger

object StatusNetworkToken : Token {
    override val symbol = "SNT"
    override val name = "Status Network Token"
    override val decimals = "18".decimalAsBigInteger()
    override val contractAddress = "0x744d70FDBE2Ba4CF95131626614a1763DF805B9E".hexAsBigInteger()
}
