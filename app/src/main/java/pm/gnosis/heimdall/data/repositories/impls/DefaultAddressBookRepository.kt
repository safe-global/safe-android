package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.AddressBookEntryDb
import pm.gnosis.heimdall.data.db.models.fromDB
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAddressBookRepository @Inject constructor(
    appDb: ApplicationDb
) : AddressBookRepository {

    private val addressBookDao = appDb.addressBookDao()

    override fun addAddressBookEntry(address: Solidity.Address, name: String, description: String): Completable =
        Completable.fromCallable {
            addressBookDao.insertAddressBookEntry(AddressBookEntryDb(address, name, description))
        }.subscribeOn(Schedulers.io())

    override fun observeAddressBook(): Flowable<List<AddressBookEntry>> =
        addressBookDao.observeAddressBook()
            .map { it.map { it.fromDB() } }
            .subscribeOn(Schedulers.io())

    override fun observeAddressBookEntry(address: Solidity.Address): Flowable<AddressBookEntry> =
        addressBookDao.observeAddressBookEntry(address)
            .map { it.fromDB() }
            .subscribeOn(Schedulers.io())

    override fun loadAddressBookEntry(address: Solidity.Address): Single<AddressBookEntry> =
        addressBookDao.loadAddressBookEntry(address)
            .map { it.fromDB() }
            .subscribeOn(Schedulers.io())

    override fun deleteAddressBookEntry(address: Solidity.Address): Completable =
        Completable.fromCallable {
            addressBookDao.deleteAddressBookEntry(address)
        }.subscribeOn(Schedulers.io())
}
