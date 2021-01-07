package io.gnosis.safe.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

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
