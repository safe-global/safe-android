package io.gnosis.safe.ui.assets

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.utils.MnemonicKeyAndAddressDerivator
import io.gnosis.safe.utils.OwnerKeyHandler
import kotlinx.coroutines.flow.collect
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.utils.toHexString
import timber.log.Timber
import javax.inject.Inject

class SafeBalancesViewModel @Inject constructor(
        private val safeRepository: SafeRepository,
        private val encryptionManager: EncryptionManager,
        private val preferencesManager: PreferencesManager,
        private val mnemonicAddressDerivator: MnemonicKeyAndAddressDerivator,
        appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeBalancesState>(appDispatchers) {

    override fun initialState(): SafeBalancesState = SafeBalancesState.SafeLoading(null)

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState {
                    SafeBalancesState.ActiveSafe(safe, null)
                }
            }
        }

//        preferencesManager.prefs.edit { remove("encryption_manager.string.password_encrypted_app_key") }
//        preferencesManager.prefs.edit { remove("encryption_manager.string.password_checksum") }

        mnemonicAddressDerivator.initialize("creek banner employ mix teach sunny sure mutual pole mom either lion")
        val privateKey = mnemonicAddressDerivator.keyForIndex(0)
        val ownerKeyHandler = OwnerKeyHandler(encryptionManager, preferencesManager)
        Timber.i("---> privateKey.plain: ${privateKey.toHexString()}")
        ownerKeyHandler.storeKey(privateKey)

        val key = ownerKeyHandler.retrieveKey()
        Timber.i("--->        key.plain: ${privateKey.toHexString()}")

        if (privateKey != key) {
              throw RuntimeException("Keys different")
        }

    }
}

sealed class SafeBalancesState : BaseStateViewModel.State {

    data class SafeLoading(
            override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeBalancesState()

    data class ActiveSafe(
            val safe: Safe?,
            override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeBalancesState()
}
