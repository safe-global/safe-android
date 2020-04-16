package io.gnosis.data.repositories

import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import io.gnosis.data.BuildConfig
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.Safe
import pm.gnosis.ethereum.Block
import pm.gnosis.ethereum.EthGetStorageAt
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

class SafeRepository(
    private val safeDao: SafeDao,
    private val preferenceManager: PreferencesManager,
    private val ethereumRepository: EthereumRepository
) {

    suspend fun getSafes(): List<Safe> = safeDao.loadAll().asList()

    suspend fun addSafe(safe: Safe) = safeDao.insert(safe)

    suspend fun removeSafe(safe: Safe) = safeDao.delete(safe)

    @WorkerThread
    suspend fun isValidSafe(safeAddress: Solidity.Address): Boolean =
        ethereumRepository.request(EthGetStorageAt(from = safeAddress, location = BigInteger.ZERO, block = Block.LATEST))
            .blockingFirst()
            .let { result ->
                isSupported(result.checkedResult().asEthereumAddress())
            }


    suspend fun setActiveSafe(safe: Safe) {
        preferenceManager.prefs.edit {
            putString(ACTIVE_SAFE, safe.address.asEthereumAddressString())
        }
    }

    suspend fun getActiveSafe(): Safe? =
        preferenceManager.prefs.getString(ACTIVE_SAFE, null)
            ?.asEthereumAddress()
            ?.let { address ->
                getSafeBy(address)
            }

    private suspend fun getSafeBy(address: Solidity.Address): Safe? = safeDao.loadByAddress(address)

    companion object {

        @VisibleForTesting
        const val ACTIVE_SAFE = "prefs.string.active_safe"

        val safeMasterCopy_0_0_2 = BuildConfig.SAFE_MASTER_COPY_0_0_2.asEthereumAddress()!!
        val safeMasterCopy_0_1_0 = BuildConfig.SAFE_MASTER_COPY_0_1_0.asEthereumAddress()!!
        val safeMasterCopy_1_0_0 = BuildConfig.SAFE_MASTER_COPY_1_0_0.asEthereumAddress()!!
        val safeMasterCopy_1_1_1 = BuildConfig.SAFE_MASTER_COPY_1_1_1.asEthereumAddress()!!

        fun isSupported(masterCopy: Solidity.Address?) =
            supportedContracts.contains(masterCopy)

        private val supportedContracts = listOf(
            safeMasterCopy_0_0_2,
            safeMasterCopy_0_1_0,
            safeMasterCopy_1_0_0,
            safeMasterCopy_1_1_1
        )
    }
}
