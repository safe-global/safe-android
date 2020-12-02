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

    fun validate(seedPhrase: String) {
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

sealed class ImportOwnerKeyState(
    override var viewAction: BaseStateViewModel.ViewAction? = null
) : BaseStateViewModel.State {

    data class ValidSeedPhraseSubmitted(val validSeedPhrase: String) : ImportOwnerKeyState()
    data class Error(val throwable: Throwable) : ImportOwnerKeyState(BaseStateViewModel.ViewAction.ShowError(throwable))
}
