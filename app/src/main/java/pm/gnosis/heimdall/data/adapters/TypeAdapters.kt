package pm.gnosis.heimdall.data.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.toHexString
import java.math.BigInteger

class WeiAdapter {
    @ToJson
    fun toJson(wei: Wei): String =
        StringBuilder("0x").append(wei.value.toString(16)).toString()

    @FromJson
    fun fromJson(wei: String): Wei {
        if (wei.startsWith("0x")) {
            return Wei(wei.hexAsBigInteger())
        }
        return Wei(BigInteger(wei))
    }
}

class HexNumberAdapter {
    @ToJson
    fun toJson(hexNumber: BigInteger): String = hexNumber.toHexString()

    @FromJson
    fun fromJson(hexNumber: String): BigInteger = hexNumber.hexAsBigInteger()
}

class SolidityAddressAdapter {
    @ToJson
    fun toJson(address: Solidity.Address): String = address.asEthereumAddressString()

    @FromJson
    fun fromJson(address: String): Solidity.Address = address.asEthereumAddress()!!
}
