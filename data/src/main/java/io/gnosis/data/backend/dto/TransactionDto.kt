package io.gnosis.data.backend.dto

import pm.gnosis.model.Solidity
import java.lang.IllegalArgumentException

data class DataDecodedDto(
    val method: String,
    val parameters: List<ParamDto>?
)

data class ValueDecodedDto(
    val operation: Operation,
    val to: Solidity.Address,
    val value: Long,
    val data: String,
    val dataDecoded: DataDecodedDto?
)

sealed class ParamDto {
    abstract val type: String
    abstract val name: String
    abstract val value: Any

    data class AddressParam(
        override val type: String,
        override val name: String,
        override val value: Solidity.Address
    ) : ParamDto()

    data class ArrayParam(
        override val type: String,
        override val name: String,
        override val value: List<Any>
    ) : ParamDto()

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
}

enum class ParamType(val mappedSolidityTypes: List<String>) {
    ADDRESS(listOf("address")),
    ARRAY(
        Solidity.types.keys.map { "${it}[]" }
    ),
    BYTES(
        Solidity.types.keys.filter { it.contains("byte") }
    ),
    INT(
        Solidity.types.keys.filter { it.contains("int") }
    ),
    BOOL(listOf("bool")),
    STRING(listOf("string"));

    companion object {
        fun of(value: String): ParamType =
            when {
                value == "address" -> ADDRESS
                value.contains("[]") -> ARRAY
                value.startsWith("bytes") -> BYTES
                value.startsWith("uint") || value.startsWith("int") -> INT
                value == "bool" -> BOOL
                value == "string" -> STRING
                else -> throw IllegalArgumentException()
            }
    }
}

enum class Operation(val id: Int) {
    CALL(0),
    DELEGATE(1)
}
