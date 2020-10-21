package io.gnosis.data

import com.squareup.moshi.JsonAdapter
import io.gnosis.data.models.TransactionDetails

fun <T> buildMultiSigTransactionDetails(fileName: String = "tx_details_transfer.json", adapter: JsonAdapter<T>): T = adapter.readJsonFrom(fileName)
