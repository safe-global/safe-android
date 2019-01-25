package pm.gnosis.heimdall.data.db

import androidx.room.TypeConverter
import pm.gnosis.utils.hexAsBigIntegerOrNull
import pm.gnosis.utils.toHexString
import java.math.BigInteger

class BigIntegerConverter {
    @TypeConverter
    fun fromHexString(hexString: String?) = hexString?.hexAsBigIntegerOrNull()

    @TypeConverter
    fun toHexString(value: BigInteger?): String? = value?.toHexString()
}
