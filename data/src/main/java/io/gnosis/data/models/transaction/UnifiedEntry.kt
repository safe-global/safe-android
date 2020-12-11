package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import java.util.*
import io.gnosis.data.models.transaction.Transaction as TransactionSummary

enum class UnifiedEntryType {
    @Json(name = "TRANSACTION")  TRANSACTION,
    @Json(name = "DATE_LABEL")  DATE_LABEL,
    @Json(name = "LABEL") LABEL,
    @Json(name = "CONFLICT_HEADER")  CONFLICT_HEADER,
    @Json(name = "UNKNOWN")  UNKNOWN
}

enum class ConflictType {
    @Json(name = "None") None,
    @Json(name = "HasNext") HasNext,
    @Json(name = "End") End
}

enum class LabelType {
    @Json(name = "Next") Next,
    @Json(name = "Queued") Queued
}

sealed class UnifiedEntry(@Json(name = "type") val type: UnifiedEntryType) {

    data class Transaction(
        @Json(name = "transaction") val transaction: TransactionSummary,
        @Json(name = "conflictType") val conflictType: ConflictType
    ) : UnifiedEntry(UnifiedEntryType.TRANSACTION)

    data class DateLabel(
        @Json(name = "timestamp")  val timestamp: Date
    ) : UnifiedEntry(UnifiedEntryType.DATE_LABEL)

    data class Label(
        @Json(name = "label") val label: LabelType
    ) : UnifiedEntry(UnifiedEntryType.LABEL)

    data class ConflictHeader(
        @Json(name = "nonce") val nonce: Long
    ) : UnifiedEntry(UnifiedEntryType.CONFLICT_HEADER)

    object Unknown : UnifiedEntry(UnifiedEntryType.UNKNOWN)
}
