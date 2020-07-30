package io.gnosis.data.utils

import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.*

fun Date.formatBackendDate(): String {
    val customFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    val instant = Instant.ofEpochMilli(this.time)
    return customFormatter.format(instant)
}
