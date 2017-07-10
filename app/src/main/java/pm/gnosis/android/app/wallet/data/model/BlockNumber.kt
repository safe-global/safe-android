package pm.gnosis.android.app.wallet.data.model

import com.squareup.moshi.Json
import java.math.BigInteger

data class BlockNumber(@Json(name = "jsonrpc") val jsonRpcVersion: String,
                       @Json(name = "id") val id: Int,
                       @Json(name = "result") val result: BigInteger)
