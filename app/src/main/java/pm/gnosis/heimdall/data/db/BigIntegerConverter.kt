package pm.gnosis.heimdall.data.db

import android.arch.persistence.room.TypeConverter
import pm.gnosis.utils.hexAsBigIntegerOrNull
import java.math.BigInteger

class BigIntegerConverter {
    @TypeConverter
    fun fromHexString(hexString: String) = hexString.hexAsBigIntegerOrNull()

    @TypeConverter
    fun toHexString(value: BigInteger): String = value.toString(16)
}
