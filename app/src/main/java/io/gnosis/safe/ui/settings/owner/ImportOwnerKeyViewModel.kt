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

    fun validate(seedPhrase: String): Boolean {
        val cleanedUpSeedPhrase = cleanupSeedPhrase(seedPhrase)
        return runCatching { bip39Generator.validateMnemonic(cleanedUpSeedPhrase) }
            .onFailure { safeLaunch { updateState { ImportOwnerKeyState.Error(it) } } }
            .getOrDefault(false)
            .let { mnemonic -> cleanedUpSeedPhrase == mnemonic }
            .also { valid -> if (!valid) safeLaunch { updateState { ImportOwnerKeyState.Error(InvalidSeedPhrase) } } }
    }

    fun cleanupSeedPhrase(seedPhrase: String): String {
        return seedPhrase.split("\\s+?|\\p{Punct}+?".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
    }

}

object InvalidSeedPhrase : Throwable()

sealed class ImportOwnerKeyState(
    override var viewAction: BaseStateViewModel.ViewAction? = null
) : BaseStateViewModel.State {

    object AwaitingInput : ImportOwnerKeyState()
    data class Error(val throwable: Throwable) : ImportOwnerKeyState(BaseStateViewModel.ViewAction.ShowError(throwable))
}
