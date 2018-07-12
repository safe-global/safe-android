package pm.gnosis.heimdall.ui.addressbook

import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import android.support.annotation.ColorInt
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.Result

abstract class AddressBookContract : ViewModel() {
    abstract fun observeAddressBook(): Flowable<Adapter.Data<AddressBookEntry>>
    abstract fun observeAddressBookEntry(address: Solidity.Address): Flowable<AddressBookEntry>
    abstract fun loadAddressBookEntry(address: Solidity.Address): Single<AddressBookEntry>
    abstract fun addAddressBookEntry(address: String, name: String, description: String = ""): Observable<Result<AddressBookEntry>>
    abstract fun updateAddressBookEntry(address: Solidity.Address, name: String, description: String = ""): Observable<Result<AddressBookEntry>>
    abstract fun deleteAddressBookEntry(address: Solidity.Address): Observable<Result<Solidity.Address>>
    abstract fun generateQrCode(address: Solidity.Address, @ColorInt color: Int): Observable<Result<Bitmap>>

    internal class NameIsBlankException : Exception()
    internal class AddressAlreadyAddedException : Exception()
}
