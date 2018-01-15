package pm.gnosis.heimdall.ui.settings.security.revealmnemonic

import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import javax.inject.Inject

class RevealMnemonicViewModel @Inject constructor(
        private val accountsRepository: AccountsRepository
) : RevealMnemonicContract() {
    override fun loadMnemonic() = accountsRepository.loadMnemonic()
}
