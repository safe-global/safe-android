package io.gnosis.data.db

import androidx.room.TypeConverter
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigDecimal

class SolidityAddressConverter {

    @TypeConverter
    fun fromHexString(address: String) = address.asEthereumAddress()!!

    @TypeConverter
    fun toHexString(address: Solidity.Address): String = address.asEthereumAddressString()
}

// TODO push to svalinn
class BigDecimalNumberAdapter {
    @ToJson
    fun toJson(@BigDecimalNumber bigDecimal: BigDecimal): String = bigDecimal.toString()

    @FromJson
    @BigDecimalNumber
    fun fromJson(decimalNumber: String): BigDecimal = decimalNumber.toBigDecimal()
}

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class BigDecimalNumber
