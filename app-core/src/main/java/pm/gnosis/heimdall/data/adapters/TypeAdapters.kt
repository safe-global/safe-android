package pm.gnosis.heimdall.data.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import pm.gnosis.models.Wei
import pm.gnosis.utils.hexAsBigInteger
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
