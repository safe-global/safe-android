package io.gnosis.safe.ui.settings.owner

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.mnemonic.Bip39Generator
import javax.inject.Inject

class ImportOwnerKeyViewModel
@Inject constructor(
    private val bip39Generator: Bip39Generator,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ImportOwnerKeyState>(appDispatchers) {

    override fun initialState(): ImportOwnerKeyState = ImportOwnerKeyState.AwaitingInput

    fun validate(seedPhrase: String) {
        val cleanedUpSeedPhrase = cleanupSeedPhrase(seedPhrase)
        runCatching { bip39Generator.validateMnemonic(cleanedUpSeedPhrase) }
            .onFailure { safeLaunch { updateState { ImportOwnerKeyState.Error(ViewAction.ShowError(InvalidSeedPhrase)) } } }
            .onSuccess { mnemonic ->
                safeLaunch {
                    updateState {
                        if (mnemonic == cleanedUpSeedPhrase) {
                            ImportOwnerKeyState.ValidSeedPhraseSubmitted(cleanedUpSeedPhrase)
                        } else {
                            ImportOwnerKeyState.Error(ViewAction.ShowError(InvalidSeedPhrase))
                        }
                    }
                }
            }
    }

    private fun cleanupSeedPhrase(seedPhrase: String): String {
        return seedPhrase.split("\\s+?|\\p{Punct}+?".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
    }

}

object InvalidSeedPhrase : Throwable()

sealed class ImportOwnerKeyState : BaseStateViewModel.State {

    override var viewAction: BaseStateViewModel.ViewAction? = null

    object AwaitingInput : ImportOwnerKeyState()
    data class ValidSeedPhraseSubmitted(val validSeedPhrase: String) : ImportOwnerKeyState()
    data class Error(override var viewAction: BaseStateViewModel.ViewAction?) : ImportOwnerKeyState()
}
