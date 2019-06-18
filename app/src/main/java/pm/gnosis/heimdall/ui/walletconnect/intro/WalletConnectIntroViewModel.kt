package pm.gnosis.heimdall.ui.walletconnect.intro

import pm.gnosis.heimdall.data.repositories.BridgeRepository
import javax.inject.Inject

class WalletConnectIntroViewModel @Inject constructor(
    private val bridgeRepository: BridgeRepository
): WalletConnectIntroContract() {
    override fun markIntroDone() = bridgeRepository.markIntroDone()
}