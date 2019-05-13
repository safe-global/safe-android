package pm.gnosis.heimdall.helpers

import android.widget.TextView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import pm.gnosis.blockies.BlockiesImageView
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.utils.asMiddleEllipsized
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressHelper @Inject constructor(
    private val addressBookRepository: AddressBookRepository
) {
    fun populateAddressInfo(
        addressView: TextView,
        nameView: TextView,
        imageView: BlockiesImageView?,
        address: Solidity.Address
    ): List<Disposable> {
        imageView?.setAddress(address)
        return listOf(
            Single.fromCallable {
                address.asEthereumAddressChecksumString()
            }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess {
                    addressView.text = it.asMiddleEllipsized(6)
                }
                .flatMap {
                    addressBookRepository.loadAddressBookEntry(address).map { it.name }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        nameView.visible(true)
                        nameView.text = it
                    },
                    onError = Timber::e
                )
        )
    }
}
