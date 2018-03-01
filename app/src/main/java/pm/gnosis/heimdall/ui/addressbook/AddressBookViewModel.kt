package pm.gnosis.heimdall.ui.addressbook

import android.database.sqlite.SQLiteConstraintException
import android.support.annotation.ColorInt
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.*
import pm.gnosis.utils.exceptions.InvalidAddressException
import java.math.BigInteger
import javax.inject.Inject

class AddressBookViewModel @Inject constructor(
    private val addressBookRepository: AddressBookRepository,
    private val qrCodeGenerator: QrCodeGenerator
) : AddressBookContract() {
    override fun observeAddressBook() = addressBookRepository
        .observeAddressBook().scanToAdapterData({ it.address })

    override fun observeAddressBookEntry(address: BigInteger) =
        addressBookRepository.observeAddressBookEntry(address)

    override fun addAddressBookEntry(address: String, name: String, description: String) =
        Observable
            .fromCallable {
                val addressBigInteger = address.hexAsBigIntegerOrNull()
                if (addressBigInteger == null || !addressBigInteger.isValidEthereumAddress()) throw InvalidAddressException(address)
                if (name.isBlank()) throw NameIsBlankException()
            }
            .flatMap {
                addressBookRepository.addAddressBookEntry(address.hexAsBigInteger(), name.trimWhitespace(), description)
                    .andThen(Observable.just(AddressBookEntry(address.hexAsBigInteger(), name.trimWhitespace(), description)))
                    .onErrorResumeNext { t: Throwable -> Observable.error(if (t is SQLiteConstraintException) AddressAlreadyAddedException() else t) }
            }
            .mapToResult()

    override fun deleteAddressBookEntry(address: BigInteger) =
        addressBookRepository
            .deleteAddressBookEntry(address)
            .andThen(Observable.just(address))
            .mapToResult()

    override fun generateQrCode(address: BigInteger, @ColorInt color: Int) =
        Observable
            .fromCallable { address.asEthereumAddressString() }
            .flatMapSingle { qrCodeGenerator.generateQrCode(it, backgroundColor = color) }
            .mapToResult()
}
