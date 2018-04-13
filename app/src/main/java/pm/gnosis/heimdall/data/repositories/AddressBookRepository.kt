package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry

interface AddressBookRepository {
    fun addAddressBookEntry(address: Solidity.Address, name: String, description: String = ""): Completable
    fun observeAddressBook(): Flowable<List<AddressBookEntry>>
    fun observeAddressBookEntry(address: Solidity.Address): Flowable<AddressBookEntry>
    fun deleteAddressBookEntry(address: Solidity.Address): Completable
}
