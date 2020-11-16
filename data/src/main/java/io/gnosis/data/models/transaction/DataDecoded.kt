package io.gnosis.data.models.transaction

import pm.gnosis.model.Solidity
import java.math.BigInteger

data class DataDecoded(
    val method: String,
    val parameters: List<Param>?
)

data class ValueDecoded(
    val operation: Operation,
    val to: Solidity.Address,
    val value: BigInteger,
    val data: String,
    val dataDecoded: DataDecoded?
)

sealed class Param {
    abstract val type: String
    abstract val name: String
    abstract val value: Any?

    data class AddressParam(
        override val type: String,
        override val name: String,
        override val value: Solidity.Address
    ) : Param()

    data class ArrayParam(
        override val type: String,
        override val name: String,
        override val value: List<Any>
    ) : Param() {

        fun getItemType(): ParamType {
            val baseType = type.split("[")[0]
            return when  {
                baseType == "address" -> ParamType.ADDRESS
                baseType.startsWith("bytes") -> ParamType.BYTES
                else -> ParamType.VALUE
            }
        }
    }

    data class BytesParam(
        override val type: String,
        override val name: String,
        override val value: String,
        val valueDecoded: List<ValueDecoded>?
    ) : Param()

    data class ValueParam(
        override val type: String,
        override val name: String,
        override val value: Any
    ) : Param() {

        fun isBytesValue(): Boolean = type.startsWith("bytes")
    }

    object UnknownParam : Param() {
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
