package pm.gnosis.heimdall.ui.addressbook

import android.database.sqlite.SQLiteConstraintException
import android.support.annotation.ColorInt
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.trimWhitespace
import javax.inject.Inject

class AddressBookViewModel @Inject constructor(
    private val addressBookRepository: AddressBookRepository,
    private val qrCodeGenerator: QrCodeGenerator
) : AddressBookContract() {

    override fun observeAddressBook() = addressBookRepository
        .observeAddressBook().scanToAdapterData({ it.address })

    override fun observeAddressBookEntry(address: Solidity.Address) =
        addressBookRepository.observeAddressBookEntry(address)

    override fun loadAddressBookEntry(address: Solidity.Address): Single<AddressBookEntry> =
        addressBookRepository.loadAddressBookEntry(address)

    override fun addAddressBookEntry(address: String, name: String, description: String) =
        Observable
            .fromCallable { checkAddressEntryData(name, address) }
            .flatMap {
                addressBookRepository.addAddressBookEntry(it, name.trimWhitespace(), description)
                    .andThen(Observable.just(AddressBookEntry(it, name.trimWhitespace(), description)))
                    .onErrorResumeNext { t: Throwable -> Observable.error(if (t is SQLiteConstraintException) AddressAlreadyAddedException() else t) }
            }
            .mapToResult()

    override fun updateAddressBookEntry(
        oldAddress: Solidity.Address,
        name: String,
        newAddress: String,
        description: String
    ): Observable<Result<AddressBookEntry>> =
        Observable
            .fromCallable { checkAddressEntryData(name, newAddress) }
            .flatMap {
                addressBookRepository.updateAddressBookEntry(oldAddress, newAddress.asEthereumAddress()!!, name)
                    .andThen(Observable.just(AddressBookEntry(it, name.trimWhitespace(), description)))
                    .onErrorResumeNext { t: Throwable -> Observable.error(if (t is SQLiteConstraintException) AddressAlreadyAddedException() else t) }
            }
            .mapToResult()

    private fun checkAddressEntryData(name: String, address: String): Solidity.Address {
        if (name.isBlank()) throw NameIsBlankException()
        return address.asEthereumAddress() ?: throw InvalidAddressException(address)
    }

    override fun deleteAddressBookEntry(address: Solidity.Address) =
        addressBookRepository
            .deleteAddressBookEntry(address)
            .andThen(Observable.just(address))
            .mapToResult()

    override fun generateQrCode(address: Solidity.Address, @ColorInt color: Int) =
        Observable
            .fromCallable { address.asEthereumAddressString() }
            .flatMapSingle { qrCodeGenerator.generateQrCode(it, backgroundColor = color) }
            .mapToResult()
}
