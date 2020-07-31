package io.gnosis.safe.utils

import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.*

fun Date.formatBackendDate(zoneId : ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String {
    val customFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(zoneId).withLocale(locale)
    return customFormatter.format(Instant.ofEpochMilli(time))
}
