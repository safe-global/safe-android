package pm.gnosis.heimdall.ui.addressbook

import androidx.room.EmptyResultSetException
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.annotation.ColorInt
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
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
    @ApplicationContext private val context: Context,
    private val addressBookRepository: AddressBookRepository,
    private val safeRepository: GnosisSafeRepository,
    private val qrCodeGenerator: QrCodeGenerator
) : AddressBookContract() {

    private val errorHandler = SimpleLocalizedException.Handler.Builder(context)
        .add({ it is AddressAlreadyAddedException }, R.string.address_already_in_address_book)
        .add({ it is NameIsBlankException }, R.string.name_cannot_be_blank)
        .add({ it is InvalidAddressException }, R.string.invalid_ethereum_address)
        .build()


    override fun observeAddressBook() = addressBookRepository
        .observeAddressBook().scanToAdapterData({ it.address })

    override fun observeAddressBookEntry(address: Solidity.Address) =
        addressBookRepository.observeAddressBookEntry(address)

    override fun loadAddressBookEntry(address: Solidity.Address): Single<AddressBookEntry> =
        addressBookRepository.loadAddressBookEntry(address)

    override fun addAddressBookEntry(address: String, name: String, description: String) =
        Observable
            .fromCallable {
                if (name.isBlank()) throw NameIsBlankException()
                address.asEthereumAddress() ?: throw InvalidAddressException(address)
            }
            .flatMap {
                addressBookRepository.addAddressBookEntry(it, name.trimWhitespace(), description)
                    .andThen(Observable.just(AddressBookEntry(it, name.trimWhitespace(), description)))
                    .onErrorResumeNext { t: Throwable -> Observable.error(if (t is SQLiteConstraintException) AddressAlreadyAddedException() else t) }
            }
            .onErrorResumeNext { t: Throwable -> errorHandler.observable(t) }
            .mapToResult()

    override fun updateAddressBookEntry(
        address: Solidity.Address,
        name: String,
        description: String
    ): Observable<Result<AddressBookEntry>> =
        Observable
            .fromCallable {
                if (name.isBlank()) throw NameIsBlankException()
                address
            }
            .flatMap {
                addressBookRepository.updateAddressBookEntry(it, name)
                    .andThen(Observable.just(AddressBookEntry(it, name.trimWhitespace(), description)))
                    .onErrorResumeNext { t: Throwable -> Observable.error(if (t is SQLiteConstraintException) AddressAlreadyAddedException() else t) }
            }
            .onErrorResumeNext { t: Throwable -> errorHandler.observable(t) }
            .mapToResult()

    override fun deleteAddressBookEntry(address: Solidity.Address) =
        checkDeletion(address)
            .andThen(addressBookRepository.deleteAddressBookEntry(address))
            .andThen(Observable.just(address))
            .mapToResult()

    private fun checkDeletion(address: Solidity.Address): Completable =
        safeRepository.loadAbstractSafe(address)
            // We have a Safe for that address ... we should not delete this address
            .map<Unit> { throw SimpleLocalizedException(context.getString(R.string.cannot_delete_safe_from_address_book)) }
            // If no Safe was found we can delete the address else propagate the error
            .onErrorResumeNext {
                when (it) {
                    is EmptyResultSetException -> Single.just(Unit)
                    else -> Single.error(it)
                }
            }
            .ignoreElement()

    override fun generateQrCode(address: Solidity.Address, @ColorInt color: Int) =
        Observable
            .fromCallable { address.asEthereumAddressString() }
            .flatMapSingle { qrCodeGenerator.generateQrCode(it, backgroundColor = color) }
            .mapToResult()
}
