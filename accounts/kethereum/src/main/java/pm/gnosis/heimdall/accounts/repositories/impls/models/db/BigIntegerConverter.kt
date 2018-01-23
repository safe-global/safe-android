package pm.gnosis.heimdall.accounts.repositories.impls.models.db

import android.arch.persistence.room.TypeConverter
import pm.gnosis.utils.hexAsBigIntegerOrNull
import java.math.BigInteger

// TODO: remove duplication (we have the same converter in the app module)
class BigIntegerConverter {
    @TypeConverter
    fun fromHexString(hexString: String) = hexString.hexAsBigIntegerOrNull()

    @TypeConverter
    fun toHexString(value: BigInteger): String = value.toString(16)
}
