package io.gnosis.data.repositories

data class Page<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)

inline fun <T, R> Page<T>.mapInner(mapper: (T) -> R): Page<R> = Page(count, next, previous, results.map(mapper))
