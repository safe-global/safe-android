package io.gnosis.data.repositories

import android.content.SharedPreferences
import io.gnosis.contracts.BuildConfig
import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.ethereum.Block
import pm.gnosis.ethereum.EthGetStorageAt
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.lang.IllegalStateException
import java.math.BigInteger

class SafeRepository(
    private val safeDao: SafeDao,
    private val preferencesManager: PreferencesManager,
    private val ethereumRepository: EthereumRepository,
    private val transactionServiceApi: TransactionServiceApi
) {

    private val keyFlow = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> offer(key) }
        preferencesManager.prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferencesManager.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun activeSafeFlow() =
        keyFlow
            .filter { it == ACTIVE_SAFE }
            .onStart { emit(ACTIVE_SAFE) }
            .map { getActiveSafe() }
            .conflate()

    suspend fun isSafeAddressUsed(address: Solidity.Address): Boolean = safeDao.loadByAddress(address) != null

    suspend fun getSafes(): List<Safe> = safeDao.loadAll().asList()

    suspend fun addSafe(safe: Safe) = safeDao.insert(safe)

    suspend fun removeSafe(safe: Safe) = safeDao.delete(safe)

    suspend fun isValidSafe(safeAddress: Solidity.Address): Boolean =
        ethereumRepository.request(EthGetStorageAt(from = safeAddress, location = BigInteger.ZERO, block = Block.LATEST)).let { request ->
            isSupported(request.checkedResult("Valid safe check failed").asEthereumAddress())
        }

    suspend fun clearActiveSafe() {
        preferencesManager.prefs.edit {
            remove(ACTIVE_SAFE)
        }
    }

    suspend fun setActiveSafe(safe: Safe) {
        preferencesManager.prefs.edit {
            putString(ACTIVE_SAFE, safe.address.asEthereumAddressString())
        }
    }

    suspend fun getActiveSafe(): Safe? =
        preferencesManager.prefs.getString(ACTIVE_SAFE, null)
            ?.asEthereumAddress()
            ?.let { address ->
                getSafeBy(address)
            }

    suspend fun getSafeInfo(safeAddress: Solidity.Address): SafeInfo =
        transactionServiceApi.getSafeInfo(safeAddress.asEthereumAddressChecksumString()).let {
            SafeInfo(it.address, it.nonce, it.threshold, it.owners, it.masterCopy)
        }

    private suspend fun getSafeBy(address: Solidity.Address): Safe? = safeDao.loadByAddress(address)

    companion object {

        private const val ACTIVE_SAFE = "prefs.string.active_safe"

        val SAFE_MASTER_COPY_0_0_2 = BuildConfig.SAFE_MASTER_COPY_0_0_2.asEthereumAddress()!!
        val SAFE_MASTER_COPY_0_1_0 = BuildConfig.SAFE_MASTER_COPY_0_1_0.asEthereumAddress()!!
        val SAFE_MASTER_COPY_1_0_0 = BuildConfig.SAFE_MASTER_COPY_1_0_0.asEthereumAddress()!!
        val SAFE_MASTER_COPY_1_1_1 = BuildConfig.SAFE_MASTER_COPY_1_1_1.asEthereumAddress()!!

        fun masterCopyVersion(masterCopy: Solidity.Address): String =
            supportedContracts[masterCopy] ?: throw IllegalStateException("Unsupported mastercopy version")

        fun isSupported(masterCopy: Solidity.Address?) =
            supportedContracts.containsKey(masterCopy)

        private val supportedContracts = mapOf(
            SAFE_MASTER_COPY_0_0_2 to "0.0.2",
            SAFE_MASTER_COPY_0_1_0 to "0.1.0",
            SAFE_MASTER_COPY_1_0_0 to "1.0.0",
            SAFE_MASTER_COPY_1_1_1 to "1.1.1"
        )

        fun isSettingsMethod(methodName: String?): Boolean = settingMethodNames.contains(methodName)

        private val settingMethodNames = listOf(
            "setFallbackHandler",
            "addOwnerWithThreshold",
            "removeOwner",
            "swapOwner",
            "changeThreshold",
            "changeMasterCopy",
            "enableModule",
            "disableModule"
        )
    }
}
