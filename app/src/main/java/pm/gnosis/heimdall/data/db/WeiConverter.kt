package pm.gnosis.heimdall.data.db

import android.arch.persistence.room.TypeConverter
import pm.gnosis.models.Wei

class WeiConverter {
    @TypeConverter
    fun fromString(string: String) = Wei(string.toBigInteger())

    @TypeConverter
    fun toString(wei: Wei): String = wei.value.toString()
}
