package pm.gnosis.heimdall.ui.safe.recover.safe

import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract

abstract class RecoverSafeRecoveryPhraseContract : InputRecoveryPhraseContract() {
    abstract fun setup(safeOwner: AccountsRepository.SafeOwner?)
}
