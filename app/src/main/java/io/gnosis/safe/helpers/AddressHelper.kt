package io.gnosis.safe.helpers


import android.widget.TextView
import io.gnosis.safe.utils.shortChecksumString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import pm.gnosis.blockies.BlockiesImageView
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import javax.inject.Inject

//TODO legacy handles name too
class AddressHelper
@Inject constructor() {
    fun populateAddressInfo(
        addressView: TextView,
        imageView: BlockiesImageView?,
        address: Solidity.Address
    ) {

        GlobalScope.launch(Dispatchers.Default) {
            val (displayAddress, fullAddress) = address.shortChecksumString() to address.asEthereumAddressChecksumString()

            addressView.post {
                imageView?.setAddress(address)
                addressView.text = displayAddress
                addressView.setOnClickListener {
                    AddressTooltip(addressView.context, fullAddress).showAsDropDown(addressView)
                }
            }
        }
    }
}
