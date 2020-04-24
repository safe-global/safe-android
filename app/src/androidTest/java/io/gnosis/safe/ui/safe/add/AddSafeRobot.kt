package io.gnosis.safe.ui.safe.add

import io.gnosis.safe.R
import io.gnosis.safe.ui.BaseRobot
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString

fun submitSafeAddress(block: AddSafeRobot.() -> Unit) = AddSafeRobot().apply { block() }

class AddSafeRobot : BaseRobot() {

    fun setSafeAddress(address: Solidity.Address) {
        fillEditText(R.id.add_safe_address_input_entry, address.asEthereumAddressString())
    }

    fun submitClick() {
//        clickButton(R.id.add_safe_address_input_button)
    }
}
