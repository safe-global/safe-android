package io.gnosis.data.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson
import io.gnosis.data.models.Operation
import io.gnosis.data.models.TransferType

class OperationEnumAdapter {
    @ToJson
    fun toJson(operation: Operation): String = operation.id.toString()

    @FromJson
    fun fromJson(operation: String): Operation =
        when (operation) {
            "0" -> Operation.CALL
            "1" -> Operation.DELEGATE
            else -> throw JsonDataException("Unsupported operation value: \"$operation\"")
        }
}

class TransferTypeEnumAdapter {
    @ToJson
    fun toJson(transferType: TransferType): String = transferType.id.toString()

    @FromJson
    fun fromJson(transferType: String): TransferType =
        when (transferType) {
            TransferType.ETHER_TRANSFER.name -> TransferType.ETHER_TRANSFER
            TransferType.ERC20_TRANSFER.name -> TransferType.ERC20_TRANSFER
            TransferType.ERC721_TRANSFER.name -> TransferType.ERC721_TRANSFER
            TransferType.UNKNOWN.name -> TransferType.UNKNOWN
            else -> throw JsonDataException("Unsupported transferType value: \"$transferType\"")
        }
}
