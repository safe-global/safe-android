package io.gnosis.safe.ui.transactions.details

import com.keystone.sdk.KeystoneEthereumSDK
import com.keystone.sdk.KeystoneSDK
import io.gnosis.data.repositories.CredentialsRepository
import pm.gnosis.model.Solidity
import javax.inject.Inject

class KeystoneSignViewModel
@Inject constructor(
    private val credentialsRepository: CredentialsRepository,
) {
    private val sdk = KeystoneSDK()

    fun setSignRequestUREncoder(
        ownerAddress: Solidity.Address,
        safeTxHash: String,
        signType: KeystoneEthereumSDK.DataType
    ) {

    }
}