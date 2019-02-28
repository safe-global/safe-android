package pm.gnosis.heimdall.utils

import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.eip712.Literal712
import pm.gnosis.eip712.Struct712
import pm.gnosis.eip712.Struct712Parameter
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase

fun Struct712.toJson(name: String, indent: Int = 0): String {

    val result = StringBuilder()
    for(i in 0 until indent)
        result.append("    ")
    result.append("\"${name}\" : {")
    parameters.forEach {

        result.append("\n")
        if (it.type is Literal712) {

            for(i in 0 until indent + 1)
                result.append("    ")

            result.append("\"${it.name}\" : ")
            val value = (it.type as Literal712).value
            when(value) {
                is Solidity.Address -> result.append("\"${value.asEthereumAddressChecksumString()}\"")
                is Solidity.String -> result.append("\"${value.value}\"")
                is Solidity.UInt256 -> result.append("${value.value}")
                else -> result.append("${value}\n")
            }
        } else {
            result.append((it.type as Struct712).toJson(it.name, indent + 1))
        }
    }

    result.append("\n")
    for(i in 0 until indent)
        result.append("    ")
    result.append("},")

    return result.toString()
}

fun Struct712Parameter.getValue(): Any? {
    val value: SolidityBase.Type? = if (this.type is Literal712)  (this.type as Literal712).value else null
    return when(value) {
        is Solidity.Address -> value
        is Solidity.String -> value.value
        else -> value
    }
}