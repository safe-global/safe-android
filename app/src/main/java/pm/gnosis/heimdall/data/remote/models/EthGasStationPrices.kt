package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json

/**
 * Prices are in <b>0.1 GWei</b> and wait times are in <b>minutes</b>
 */
data class EthGasStationPrices(
    @Json(name = "safeLow")
    val slowPrice: Float,
    @Json(name = "safeLowWait")
    val slowWaitTime: Float,
    @Json(name = "average")
    val standardPrice: Float,
    @Json(name = "avgWait")
    val standardWaitTime: Float,
    @Json(name = "fast")
    val fastPrice: Float,
    @Json(name = "fastWait")
    val fastWaitTime: Float
)
