package io.gnosis.data.backend.dto

import pm.gnosis.model.Solidity
import java.math.BigInteger

data class DataDecodedDto(
    val method: String,
    val parameters: List<ParamDto>?
)

data class ValueDecodedDto(
    val operation: Operation,
    val to: Solidity.Address,
    val value: BigInteger,
    val data: String,
    val dataDecoded: DataDecodedDto?
)

sealed class ParamDto {
    abstract val type: String
    abstract val name: String
    abstract val value: Any?

    data class AddressParam(
        override val type: String,
        override val name: String,
        override val value: Solidity.Address
    ) : ParamDto()

    data class ArrayParam(
        override val type: String,
        override val name: String,
        override val value: List<Any>
    ) : ParamDto() {

        fun getItemType(): ParamType = when (type.split("[")[0]) {
            "address" -> ParamType.ADDRESS
            "bytes" -> ParamType.BYTES
            else -> ParamType.VALUE
        }
    }

    data class BytesParam(
        override val type: String,
        override val name: String,
        override val value: String,
        val valueDecoded: List<ValueDecodedDto>?
    ) : ParamDto()

    data class ValueParam(
        override val type: String,
        override val name: String,
        override val value: Any
    ) : ParamDto()

    object UnknownParam : ParamDto() {
        override val type: String
            get() = "unknown"
        override val name: String
            get() = "unknown"
        override val value: Any?
            get() = null
    }
}

enum class ParamType {
    ADDRESS,
    BYTES,
    VALUE
}

enum class Operation(val id: Int) {
    CALL(0),
    DELEGATE(1)
}
