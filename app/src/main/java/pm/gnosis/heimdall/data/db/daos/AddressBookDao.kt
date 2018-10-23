package pm.gnosis.heimdall.data.db.daos

import android.arch.persistence.room.*
import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.data.db.models.AddressBookEntryDb
import pm.gnosis.model.Solidity

@Dao
interface AddressBookDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAddressBookEntry(addressBookEntryDb: AddressBookEntryDb)

    @Query("SELECT * FROM ${AddressBookEntryDb.TABLE_NAME} ORDER BY ${AddressBookEntryDb.COL_NAME}")
    fun observeAddressBook(): Flowable<List<AddressBookEntryDb>>

    @Query("SELECT * FROM ${AddressBookEntryDb.TABLE_NAME} WHERE ${AddressBookEntryDb.COL_ADDRESS} = :address")
    fun observeAddressBookEntry(address: Solidity.Address): Flowable<AddressBookEntryDb>

    @Query("SELECT * FROM ${AddressBookEntryDb.TABLE_NAME} WHERE ${AddressBookEntryDb.COL_ADDRESS} = :address")
    fun loadAddressBookEntry(address: Solidity.Address): Single<AddressBookEntryDb>

    @Query("DELETE FROM ${AddressBookEntryDb.TABLE_NAME} WHERE ${AddressBookEntryDb.COL_ADDRESS} = :address")
    fun deleteAddressBookEntry(address: Solidity.Address)

    @Update
    fun updateAddressBookEntry(addressBookEntryDb: AddressBookEntryDb)
}
