package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.AddressBookEntryDb
import pm.gnosis.heimdall.data.db.models.fromDB
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.models.AddressBookEntry
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAddressBookRepository @Inject constructor(
        appDb: ApplicationDb
) : AddressBookRepository {

    private val addressBookDao = appDb.addressBookDao()

    override fun addAddressBookEntry(address: BigInteger, name: String, description: String): Completable =
            Completable.fromCallable {
                addressBookDao.insertAddressBookEntry(AddressBookEntryDb(address, name, description))
            }.subscribeOn(Schedulers.io())

    override fun observeAddressBook(): Flowable<List<AddressBookEntry>> =
            addressBookDao.observeAddressBook()
                    .map { it.map { it.fromDB() } }
                    .subscribeOn(Schedulers.io())

    override fun observeAddressBookEntry(address: BigInteger): Flowable<AddressBookEntry> =
            addressBookDao.observeAddressBookEntry(address)
                    .map { it.fromDB() }
                    .subscribeOn(Schedulers.io())

    override fun deleteAddressBookEntry(address: BigInteger): Completable =
            Completable.fromCallable {
                addressBookDao.deleteAddressBookEntry(address)
            }.subscribeOn(Schedulers.io())
}
