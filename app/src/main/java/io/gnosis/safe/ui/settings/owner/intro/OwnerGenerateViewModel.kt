package io.gnosis.safe.ui.settings.owner.intro

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.utils.toSignatureString
import io.gnosis.safe.R
import io.gnosis.safe.utils.MnemonicKeyAndAddressDerivator
import kotlinx.coroutines.launch
import pm.gnosis.crypto.KeyPair
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.hexToByteArray
import timber.log.Timber
import javax.inject.Inject

class OwnerGenerateViewModel
@Inject constructor(
    private val bip39: Bip39,
    private val derivator: MnemonicKeyAndAddressDerivator,
    private val credentialsRepository: CredentialsRepository
) : ViewModel() {

    var ownerData: GeneratedOwnerData? = null
        private set

    init {
        generateOwner()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun generateOwner() {
        viewModelScope.launch {
            var counter = 0
            while (true) {
                counter++
                val mnemonic = bip39.generateMnemonic(languageId = R.id.english)
                derivator.initialize(mnemonic)
                val key = derivator.keyForIndex(0)
                Timber.i("---> $counter: mnemonic: $mnemonic")
                val address = derivator.addressesForPage(0, 1)[0]
                credentialsRepository.saveOwner(address, key, "no name")
                val owner = credentialsRepository.owner(address)!!
                val decryptedKey = credentialsRepository.decryptKey(owner.privateKey!!)
                try {
//                    CheckedKeyPair.fromPrivate(key)
//                    CheckedKeyPair.fromPrivate(decryptedKey)

                    val signature = KeyPair
                        .fromPrivate(decryptedKey)
                        .sign("0x112233445566778899".hexToByteArray())
                        .toSignatureString()
                        .addHexPrefix()

                    Timber.i("---> $counter: signature: $signature")

                } catch (e: java.lang.IllegalArgumentException) {
                    Timber.e("---> $counter: mnemonic: $mnemonic FAILED!!!!")

                    e.printStackTrace()
                    throw RuntimeException(e)
                }
                credentialsRepository.removeOwner(owner)
            }
        }
    }
}

data class GeneratedOwnerData(
    val mnemonic: String,
    val address: String,
    val key: String
)
