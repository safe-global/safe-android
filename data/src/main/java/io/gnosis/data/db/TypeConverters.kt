package io.gnosis.data.db

import androidx.room.TypeConverter
import io.gnosis.data.models.Chain
import io.gnosis.data.models.RpcAuthentication
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigIntegerOrNull
import pm.gnosis.utils.toHexString
import java.math.BigInteger

class SolidityAddressConverter {

    @TypeConverter
    fun fromHexString(address: String) = address.asEthereumAddress()!!

    @TypeConverter
    fun toHexString(address: Solidity.Address): String = address.asEthereumAddressString()
}

class BigIntegerConverter {
    @TypeConverter
    fun fromHexString(hexString: String?) = hexString?.hexAsBigIntegerOrNull()

    @TypeConverter
    fun toHexString(value: BigInteger?): String? = value?.toHexString()
}

class RpcAuthenticationConverter {
    @TypeConverter
    fun toInt(rpcAuthentication: RpcAuthentication) = rpcAuthentication.value

    @TypeConverter
    fun fromInt(rpcAuthenticationValue: Int) = RpcAuthentication.from(rpcAuthenticationValue)
}

class ChainFeaturesConverter {
    @TypeConverter
    fun toString(features: List<Chain.Feature>) = features?.joinToString(",")

    @TypeConverter
    fun fromString(featuresString: String) = featuresString.split(",").map { Chain.Feature.valueOf(it) }
}
