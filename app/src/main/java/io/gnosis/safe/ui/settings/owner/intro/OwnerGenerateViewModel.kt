package io.gnosis.safe.ui.settings.owner.intro

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.gnosis.safe.R
import io.gnosis.safe.utils.MnemonicKeyAndAddressDerivator
import kotlinx.coroutines.launch
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.toHexString
import javax.inject.Inject

class OwnerGenerateViewModel
@Inject constructor(
    private val bip39: Bip39,
    private val derivator: MnemonicKeyAndAddressDerivator
) : ViewModel() {

    var ownerData: GeneratedOwnerData? = null
        private set

    init {
        generateOwner()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun generateOwner() {
        viewModelScope.launch {
            val mnemonic = bip39.generateMnemonic(languageId = R.id.english)
            derivator.initialize(mnemonic)
            val key = derivator.keyForIndex(0).toHexString()
            val address = derivator.addressesForPage(0, 1)[0].asEthereumAddressString()
            ownerData = GeneratedOwnerData(mnemonic, address, key)
        }
    }
}

data class GeneratedOwnerData(
    val mnemonic: String,
    val address: String,
    val key: String
)
