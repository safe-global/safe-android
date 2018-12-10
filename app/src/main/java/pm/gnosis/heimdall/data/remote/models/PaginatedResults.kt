package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class PaginatedResults<T>(
    @Json(name = "results") val results: List<T>,
    @Json(name = "count") val count: Int = results.size,
    @Json(name = "next") val next: String? = null,
    @Json(name = "previous") val previous: String? = null
)
