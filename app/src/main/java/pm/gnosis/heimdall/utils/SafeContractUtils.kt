package pm.gnosis.heimdall.utils

import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress

object SafeContractUtils {

    fun checkForUpdate(masterCopy: Solidity.Address?) =
        when (masterCopy) {
            safeMasterCopy_0_0_2, safeMasterCopy_0_1_0, safeMasterCopy_1_0_0 -> safeMasterCopy_1_1_1
            else -> null
        }

    fun isSupported(masterCopy: Solidity.Address?) =
        supportedContracts.contains(masterCopy)

    fun currentMasterCopy() = safeMasterCopy_1_1_1

    private val safeMasterCopy_0_0_2 = BuildConfig.SAFE_MASTER_COPY_0_0_2.asEthereumAddress()!!
    private val safeMasterCopy_0_1_0 = BuildConfig.SAFE_MASTER_COPY_0_1_0.asEthereumAddress()!!
    private val safeMasterCopy_1_0_0 = BuildConfig.SAFE_MASTER_COPY_1_0_0.asEthereumAddress()!!
    val safeMasterCopy_1_1_1 = BuildConfig.SAFE_MASTER_COPY_1_1_1.asEthereumAddress()!!

    private val supportedContracts = listOf(
        safeMasterCopy_0_0_2,
        safeMasterCopy_0_1_0,
        safeMasterCopy_1_0_0,
        safeMasterCopy_1_1_1
    )
}
