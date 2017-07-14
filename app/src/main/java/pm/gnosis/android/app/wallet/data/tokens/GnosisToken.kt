package pm.gnosis.android.app.wallet.data.tokens

import pm.gnosis.android.app.wallet.util.decimalAsBigInteger
import pm.gnosis.android.app.wallet.util.hexAsBigInteger

object GnosisToken : Token {
    override val symbol = "GNO"
    override val name = "Gnosis Token"
    override val decimals = "18".decimalAsBigInteger()
    override val contractAddress = "0x6810e776880c02933d47db1b9fc05908e5386b96".hexAsBigInteger()
}
