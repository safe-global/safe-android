package pm.gnosis.heimdall.data.db.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import pm.gnosis.models.AddressBookEntry
import java.math.BigInteger

@Entity(tableName = AddressBookEntryDb.TABLE_NAME)
data class AddressBookEntryDb(
        @PrimaryKey
        @ColumnInfo(name = COL_ADDRESS)
        val address: BigInteger,

        @ColumnInfo(name = COL_NAME)
        val name: String,

        @ColumnInfo(name = COL_DESCRIPTION)
        val description: String
) {
    companion object {
        const val TABLE_NAME = "address_book"
        const val COL_ADDRESS = "address"
        const val COL_NAME = "name"
        const val COL_DESCRIPTION = "description"
    }
}

fun AddressBookEntryDb.fromDB() = AddressBookEntry(address, name, description)
fun AddressBookEntry.toDb() = AddressBookEntryDb(address, name, description)
