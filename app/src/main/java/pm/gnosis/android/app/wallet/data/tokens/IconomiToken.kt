package pm.gnosis.android.app.wallet.data.tokens

import pm.gnosis.android.app.wallet.util.decimalAsBigInteger
import pm.gnosis.android.app.wallet.util.hexAsBigInteger

object IconomiToken : Token {
    override val symbol = "ICN"
    override val name = "ICONOMI"
    override val decimals = "18".decimalAsBigInteger()
    override val contractAddress = "0x888666CA69E0f178DED6D75b5726Cee99A87D698".hexAsBigInteger()
}
