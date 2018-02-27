package pm.gnosis.heimdall.ui.addressbook

import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import android.support.annotation.ColorInt
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class AddressBookContract : ViewModel() {
    abstract fun observeAddressBook(): Flowable<Adapter.Data<AddressBookEntry>>
    abstract fun observeAddressBookEntry(address: BigInteger): Flowable<AddressBookEntry>
    abstract fun addAddressBookEntry(address: String, name: String, description: String = ""): Observable<Result<AddressBookEntry>>
    abstract fun deleteAddressBookEntry(address: BigInteger): Observable<Result<BigInteger>>
    abstract fun generateQrCode(address: BigInteger, @ColorInt color: Int): Observable<Result<Bitmap>>

    internal class NameIsBlankException : Exception()
    internal class AddressAlreadyAddedException : Exception()
}
