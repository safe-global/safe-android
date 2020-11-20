package io.gnosis.data.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.*

class DateAdapter {

    @ToJson
    fun toJson(date: Date): Long = date.time

    @FromJson
    fun fromJson(timestamp: Long): Date = Date(timestamp)
}
