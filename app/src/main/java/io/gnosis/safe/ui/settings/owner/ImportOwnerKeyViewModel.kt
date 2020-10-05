package io.gnosis.safe.ui.settings.owner

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.mnemonic.Bip39Generator
import javax.inject.Inject

class ImportOwnerKeyViewModel
@Inject constructor(
    private val bip39Generator: Bip39Generator,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ImportOwnerKeyState>(appDispatchers) {

    override fun initialState(): ImportOwnerKeyState = ImportOwnerKeyState.AwaitingInput

    fun validate(seedPhrase: String) {
        runCatching { bip39Generator.validateMnemonic(seedPhrase) == seedPhrase }
            .getOrElse { false }
            .also { valid ->
                safeLaunch {
                    updateState {
                        if (valid) {
                            ImportOwnerKeyState.ValidSeedPhraseSubmitted
                        } else {
                            ImportOwnerKeyState.Error(ViewAction.ShowError(InvalidSeedPhrase))
                        }
                    }
                }
            }
    }

}

object InvalidSeedPhrase : Throwable()

sealed class ImportOwnerKeyState : BaseStateViewModel.State {

    override var viewAction: BaseStateViewModel.ViewAction? = null

    object AwaitingInput : ImportOwnerKeyState()
    object ValidSeedPhraseSubmitted : ImportOwnerKeyState()
    data class Error(override var viewAction: BaseStateViewModel.ViewAction?) : ImportOwnerKeyState()
}
