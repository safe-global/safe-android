package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry

interface AddressBookRepository {
    fun addAddressBookEntry(address: Solidity.Address, name: String, description: String = ""): Completable
    fun observeAddressBook(): Flowable<List<AddressBookEntry>>
    fun observeAddressBookEntry(address: Solidity.Address): Flowable<AddressBookEntry>
    fun loadAddressBookEntry(address: Solidity.Address): Single<AddressBookEntry>
    fun deleteAddressBookEntry(address: Solidity.Address): Completable
    fun updateAddressBookEntry(address: Solidity.Address, name: String): Completable
}
