package io.gnosis.safe.helpers

import android.widget.TextView
import io.gnosis.safe.utils.formatEthAddress
import pm.gnosis.blockies.BlockiesImageView
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import javax.inject.Inject

// TODO legacy handles name too
class AddressHelper
@Inject constructor() {

    suspend fun populateAddressInfo(
        addressView: TextView,
        imageView: BlockiesImageView?,
        address: Solidity.Address,
        chainPrefix: String?
    ) {
        val (displayAddress, fullAddress) = address.formatEthAddress(
            addressView.context,
            chainPrefix,
            addMiddleLinebreak = false
        ) to address.asEthereumAddressChecksumString()

        addressView.post {
            imageView?.setAddress(address)
            addressView.text = displayAddress
        }
    }
}
