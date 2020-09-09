package io.gnosis.data.repositories

import android.content.SharedPreferences
import io.gnosis.contracts.BuildConfig
import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.models.SafeMetaData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class SafeRepository(
    private val safeDao: SafeDao,
    private val preferencesManager: PreferencesManager,
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

    suspend fun getSafes(): List<Safe> = safeDao.loadAll()

    suspend fun getSafeCount(): Int = safeDao.safeCount()

    suspend fun saveSafe(safe: Safe) = safeDao.insert(safe)

    suspend fun removeSafe(safe: Safe) = safeDao.delete(safe)

    suspend fun isValidSafe(safeAddress: Solidity.Address): Boolean =
        runCatching {
            transactionServiceApi.getSafeInfo(safeAddress.asEthereumAddressChecksumString())
        }.exceptionOrNull() == null

    suspend fun clearActiveSafe() {
        preferencesManager.prefs.edit {
            remove(ACTIVE_SAFE)
        }
    }

    suspend fun setActiveSafe(safe: Safe) {
        preferencesManager.prefs.edit {
            putString(ACTIVE_SAFE, "${safe.address.asEthereumAddressString()};${safe.localName}")
        }
    }

    suspend fun getActiveSafe(): Safe? =
        preferencesManager.prefs.getString(ACTIVE_SAFE, null)?.split(";")?.get(0)
            ?.asEthereumAddress()
            ?.let { address ->
                getSafeBy(address)
            }

    suspend fun getSafeInfo(safeAddress: Solidity.Address): SafeInfo =
        transactionServiceApi.getSafeInfo(safeAddress.asEthereumAddressChecksumString()).let {
            SafeInfo(it.address, it.nonce, it.threshold, it.owners, it.masterCopy, it.modules, it.fallbackHandler)
        }

    suspend fun getSafeBy(address: Solidity.Address): Safe? = safeDao.loadByAddress(address)

    suspend fun getSafeMetas(): List<SafeMetaData> = safeDao.getMetas()

    suspend fun getSafeMeta(address: Solidity.Address): SafeMetaData? = safeDao.getMeta(address)

    suspend fun saveSafeMeta(safeMeta: SafeMetaData) = safeDao.saveMeta(safeMeta)

    companion object {

        private const val ACTIVE_SAFE = "prefs.string.active_safe"

        val SAFE_MASTER_COPY_0_0_2 = BuildConfig.SAFE_MASTER_COPY_0_0_2.asEthereumAddress()!!
        val SAFE_MASTER_COPY_0_1_0 = BuildConfig.SAFE_MASTER_COPY_0_1_0.asEthereumAddress()!!
        val SAFE_MASTER_COPY_1_0_0 = BuildConfig.SAFE_MASTER_COPY_1_0_0.asEthereumAddress()!!
        val SAFE_MASTER_COPY_1_1_1 = BuildConfig.SAFE_MASTER_COPY_1_1_1.asEthereumAddress()!!

        val DEFAULT_FALLBACK_HANDLER = BuildConfig.DEFAULT_FALLBACK_HANDLER.asEthereumAddress()!!
        const val DEFAULT_FALLBACK_HANDLER_DISPLAY_STRING = "DefaultFallbackHandler"
        const val SAFE_MASTER_COPY_UNKNOWN_DISPLAY_STRING = "Unknown"

        fun masterCopyVersion(masterCopy: Solidity.Address?): String? = supportedContracts[masterCopy]

        fun isLatestVersion(address: Solidity.Address?): Boolean = address == SAFE_MASTER_COPY_1_1_1

        private val supportedContracts = mapOf(
            SAFE_MASTER_COPY_0_0_2 to "0.0.2",
            SAFE_MASTER_COPY_0_1_0 to "0.1.0",
            SAFE_MASTER_COPY_1_0_0 to "1.0.0",
            SAFE_MASTER_COPY_1_1_1 to "1.1.1"
        )

        const val METHOD_SET_FALLBACK_HANDLER = "setFallbackHandler"
        const val METHOD_ADD_OWNER_WITH_THRESHOLD = "addOwnerWithThreshold"
        const val METHOD_REMOVE_OWNER = "removeOwner"
        const val METHOD_SWAP_OWNER = "swapOwner"
        const val METHOD_CHANGE_THRESHOLD = "changeThreshold"
        const val METHOD_CHANGE_MASTER_COPY = "changeMasterCopy"
        const val METHOD_ENABLE_MODULE = "enableModule"
        const val METHOD_DISABLE_MODULE = "disableModule"

    }
}
