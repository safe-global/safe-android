package io.gnosis.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Page<T>(
    @Json(name = "count") val count: Int?,
    @Json(name = "next") val next: String?,
    @Json(name = "previous") val previous: String?,
    @Json(name = "results") val results: List<T>
)

inline fun <T, R> Page<T>.foldInner(folder: (item: T, MutableList<R>) -> MutableList<R>): Page<R> =
    Page(count, next, previous, results.fold(mutableListOf(), { acc, item -> folder(item, acc) }))
