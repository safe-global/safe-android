package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.model.Solidity
import java.math.BigInteger

@JsonClass(generateAdapter = true)
data class DataDecoded(
    @Json(name = "method")
    val method: String,
    @Json(name = "parameters")
    val parameters: List<Param>?
)

@JsonClass(generateAdapter = true)
data class ValueDecoded(
    @Json(name = "operation")
    val operation: Operation,
    @Json(name = "to")
    val to: Solidity.Address,
    @Json(name = "value")
    val value: BigInteger,
    @Json(name = "data")
    val data: String,
    @Json(name = "dataDecoded")
    val dataDecoded: DataDecoded?
)

sealed class Param {
    abstract val type: String
    abstract val name: String
    abstract val value: Any?

    data class Address(
        override val type: String,
        override val name: String,
        override val value: Solidity.Address
    ) : Param()

    data class Array(
        override val type: String,
        override val name: String,
        override val value: List<Any>
    ) : Param() {

        fun getItemType(): ParamType = getParamItemType(type)
    }

    data class Bytes(
        override val type: String,
        override val name: String,
        override val value: String,
        val valueDecoded: List<ValueDecoded>?
    ) : Param()

    data class Value(
        override val type: String,
        override val name: String,
        override val value: Any
    ) : Param() {

        fun isBytesValue(): Boolean = type.startsWith("bytes")
    }

    object Unknown : Param() {
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
    VALUE,
    MIXED
}

enum class Operation(val id: Int) {
    CALL(0),
    DELEGATE(1)
}

fun getParamItemType(type: String): ParamType {
    val baseType = type.split("[")[0]
    return when  {
        baseType == "address" -> ParamType.ADDRESS
        baseType.startsWith("bytes") -> ParamType.BYTES
        baseType.startsWith("(") -> ParamType.MIXED
        else -> ParamType.VALUE
    }
}
