package pm.gnosis.heimdall.helpers

import android.widget.TextView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import pm.gnosis.blockies.BlockiesImageView
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.utils.shortChecksumString
import pm.gnosis.heimdall.views.AddressTooltip
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressHelper @Inject constructor(
    private val addressBookRepository: AddressBookRepository
) {
    fun buildAddressInfoSingle(
        addressView: TextView,
        nameView: TextView,
        address: Solidity.Address
    ) =
        Single.fromCallable {
            address.shortChecksumString() to address.asEthereumAddressChecksumString()
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { (displayAddress, fullAddress) ->
                addressView.text = displayAddress
                addressView.setOnClickListener {
                    AddressTooltip(addressView.context, fullAddress).showAsDropDown(addressView)
                }
            }
            .flatMap {
                if (address == MULTI_SEND_LIB || address == MULTI_SEND_OLD_LIB)
                    Single.just(addressView.context.getString(R.string.multi_send_contract))
                else
                    addressBookRepository.loadAddressBookEntry(address).map { it.name }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                nameView.visible(true)
                nameView.text = it
            }

    fun populateAddressInfo(
        addressView: TextView,
        nameView: TextView,
        imageView: BlockiesImageView?,
        address: Solidity.Address
    ): List<Disposable> {
        imageView?.setAddress(address)
        return listOf(buildAddressInfoSingle(addressView, nameView, address).subscribeBy(onError = Timber::e))
    }

    companion object {
        private val MULTI_SEND_LIB = BuildConfig.MULTI_SEND_ADDRESS.asEthereumAddress()!!
        private val MULTI_SEND_OLD_LIB = BuildConfig.MULTI_SEND_OLD_ADDRESS.asEthereumAddress()!!
    }
}
