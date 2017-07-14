package pm.gnosis.android.app.wallet.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import pm.gnosis.android.app.wallet.util.hexAsBigInteger
import java.math.BigInteger

class WeiAdapter {
    @ToJson fun toJson(wei: Wei): String = wei.value.toString(10)

    @FromJson fun fromJson(wei: String): Wei {
        return Wei(BigInteger(wei))
    }
}

class HexNumberAdapter {
    @ToJson fun toJson(hexNumber: BigInteger): String {
        return StringBuilder("0x").append(hexNumber.toString(16)).toString()
    }

    @FromJson fun fromJson(hexNumber: String): BigInteger {
        return hexNumber.hexAsBigInteger()
    }
}
