package io.gnosis.safe.ui.settings.owner

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.PublishViewModel
import pm.gnosis.mnemonic.Bip39
import javax.inject.Inject

class OwnerSeedPhraseViewModel
@Inject constructor(
    private val bip39Generator: Bip39,
    appDispatchers: AppDispatchers
) : PublishViewModel<ImportOwnerKeyState>(appDispatchers) {

    fun validate(seedPhraseOrKey: String) {
        if (isPrivateKey(seedPhraseOrKey)) {
            validatePrivateKey(seedPhraseOrKey)
        } else {
            validateSeedPhrase(seedPhraseOrKey)
        }
    }

    internal fun isPrivateKey(seedPhraseOrKey: String): Boolean {
        val input = removeHexPrefix(seedPhraseOrKey)

        val pattern = "^[0-9a-fA-F]{64}$".toRegex()
        pattern.find(input)?.let {
            return true
        }

        return false
    }

    internal fun validatePrivateKey(key: String) {
        val input = removeHexPrefix(key)

        if (input == "0000000000000000000000000000000000000000000000000000000000000000" || !isPrivateKey(input)) {
            safeLaunch {
                updateState { ImportOwnerKeyState.Error(InvalidPrivateKey) }
            }
        } else {
            safeLaunch {
                updateState { ImportOwnerKeyState.ValidKeySubmitted(input) }
            }
        }
    }

    private fun removeHexPrefix(key: String): String = key.replace("0x", "")

    fun validateSeedPhrase(seedPhrase: String) {
        val cleanedUpSeedPhrase = cleanupSeedPhrase(seedPhrase)
        runCatching { bip39Generator.validateMnemonic(cleanedUpSeedPhrase) }
            .onFailure { safeLaunch { updateState { ImportOwnerKeyState.Error(InvalidSeedPhrase) } } }
            .onSuccess { mnemonic ->
                safeLaunch {
                    updateState {
                        if (cleanedUpSeedPhrase == mnemonic) {
                            ImportOwnerKeyState.ValidSeedPhraseSubmitted(mnemonic)
                        } else {
                            ImportOwnerKeyState.Error(InvalidSeedPhrase)
                        }
                    }
                }
            }
    }

    private fun cleanupSeedPhrase(seedPhrase: String): String {
        return seedPhrase.split("\\s+?|\\p{Punct}+?".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(separator = " ", transform = String::toLowerCase)
    }

}

object InvalidSeedPhrase : Throwable()
object InvalidPrivateKey : Throwable()

sealed class ImportOwnerKeyState(
    override var viewAction: BaseStateViewModel.ViewAction? = null
) : BaseStateViewModel.State {

    data class ValidSeedPhraseSubmitted(val validSeedPhrase: String) : ImportOwnerKeyState()
    data class ValidKeySubmitted(val key: String) : ImportOwnerKeyState()
    data class Error(val throwable: Throwable) : ImportOwnerKeyState(BaseStateViewModel.ViewAction.ShowError(throwable))
}
