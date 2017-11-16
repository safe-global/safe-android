package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import pm.gnosis.models.AddressBookEntry
import java.math.BigInteger

interface AddressBookRepository {
    fun addAddressBookEntry(address: BigInteger, name: String, description: String = ""): Completable
    fun observeAddressBook(): Flowable<List<AddressBookEntry>>
    fun observeAddressBookEntry(address: BigInteger): Flowable<AddressBookEntry>
    fun deleteAddressBookEntry(address: BigInteger): Completable
}
