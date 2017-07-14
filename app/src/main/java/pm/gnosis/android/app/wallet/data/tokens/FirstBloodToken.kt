package pm.gnosis.android.app.wallet.data.tokens

import pm.gnosis.android.app.wallet.util.decimalAsBigInteger
import pm.gnosis.android.app.wallet.util.hexAsBigInteger

object FirstBloodToken : Token {
    override val symbol = "1ST"
    override val name = "FirstBlood Token"
    override val decimals = "18".decimalAsBigInteger()
    override val contractAddress = "0xAf30D2a7E90d7DC361c8C4585e9BB7D2F6f15bc7".hexAsBigInteger()
}
