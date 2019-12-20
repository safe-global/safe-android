package pm.gnosis.heimdall.data.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.utils.parseToBigInteger
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.toHexString
import java.math.BigInteger

class WeiAdapter {
    @ToJson
    fun toJson(wei: Wei): String =
        StringBuilder("0x").append(wei.value.toString(16)).toString()

    @FromJson
    fun fromJson(wei: String): Wei {
        return Wei(wei.parseToBigInteger())
    }
}

class HexNumberAdapter {
    @ToJson
    fun toJson(@HexNumber hexNumber: BigInteger): String = hexNumber.toHexString()

    @FromJson
    @HexNumber
    fun fromJson(hexNumber: String): BigInteger = hexNumber.hexAsBigInteger()
}

class DecimalNumberAdapter {
    @ToJson
    fun toJson(@DecimalNumber bigInteger: BigInteger): String = bigInteger.toString()

    @FromJson
    @DecimalNumber
    fun fromJson(decimalNumber: String): BigInteger = decimalNumber.toBigInteger()
}

class DefaultNumberAdapter {
    @ToJson
    fun toJson(hexNumber: BigInteger): String = hexNumber.toHexString()

    @FromJson
    fun fromJson(hexNumber: String): BigInteger = hexNumber.hexAsBigInteger()
}

class SolidityAddressAdapter {
    @ToJson
    fun toJson(address: Solidity.Address): String = address.asEthereumAddressChecksumString()

    @FromJson
    fun fromJson(address: String): Solidity.Address = address.asEthereumAddress()!!
}

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class HexNumber

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class DecimalNumber
