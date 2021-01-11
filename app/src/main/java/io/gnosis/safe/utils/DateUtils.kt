package io.gnosis.safe.utils

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinDuration

fun Date.formatBackendDateTime(zoneId: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale("en", Locale.getDefault().country)): String {
    val customFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(zoneId).withLocale(locale)
    return customFormatter.format(Instant.ofEpochMilli(time))
}

fun Date.formatBackendDate(zoneId: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale("en", Locale.getDefault().country)): String {
    val customFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(zoneId).withLocale(locale)
    return customFormatter.format(Instant.ofEpochMilli(time))
}

fun Date.formatBackendTimeOfDay(zoneId: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale("en", Locale.getDefault().country)): String {
    val customFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zoneId).withLocale(locale)
    return customFormatter.format(Instant.ofEpochMilli(time))
}

@ExperimentalTime
fun Date.elapsedIntervalTo(date: Date): ElapsedInterval {

    val start = Instant.ofEpochMilli(time)
    val end = date.toInstant()
    val duration = Duration.between(start, end).toKotlinDuration()

    return when {
        duration.inSeconds < 60 -> ElapsedInterval(ChronoUnit.SECONDS, duration.inSeconds.toInt())
        duration.inMinutes < 60 -> ElapsedInterval(ChronoUnit.MINUTES, duration.inMinutes.toInt())
        duration.inHours < 24 -> ElapsedInterval(ChronoUnit.HOURS, duration.inHours.toInt())
        else -> ElapsedInterval(ChronoUnit.DAYS, duration.inDays.toInt())
    }
}

data class ElapsedInterval(
    val unit: ChronoUnit,
    val value: Int
)
