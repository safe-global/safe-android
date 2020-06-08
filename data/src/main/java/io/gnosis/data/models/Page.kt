package io.gnosis.data.models

data class Page<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)

inline fun <T, R> Page<T>.mapInner(mapper: (T) -> R): Page<R> =
    Page(count, next, previous, results.map(mapper))

inline fun <T, R> Page<T>.fold(mapper: (T) -> List<R>): Page<R> =
    Page(count, next, previous, results.fold(mutableListOf(), { acc, t -> acc.apply { addAll(mapper(t)) } }))
