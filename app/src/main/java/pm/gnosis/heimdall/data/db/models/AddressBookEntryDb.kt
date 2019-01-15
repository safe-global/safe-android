package pm.gnosis.heimdall.data.db.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry

@Entity(tableName = AddressBookEntryDb.TABLE_NAME)
data class AddressBookEntryDb(
    @PrimaryKey
    @ColumnInfo(name = COL_ADDRESS)
    val address: Solidity.Address,

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
