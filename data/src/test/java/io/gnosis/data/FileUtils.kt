package io.gnosis.data

import com.squareup.moshi.JsonAdapter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

fun Any.readResource(fileName: String): String {
    return BufferedReader(
        InputStreamReader(
            this::class.java.classLoader?.getResourceAsStream(fileName)!!
        )
    ).lines().parallel().collect(Collectors.joining("\n"))
}

fun <T> JsonAdapter<T>.readJsonFrom(fileName: String): T = fromJson(readResource(fileName))!!
