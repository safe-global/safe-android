package io.gnosis.data.repositories

import android.content.SharedPreferences
import io.gnosis.contracts.BuildConfig
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.Safe
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
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
    private val preferencesManager: PreferencesManager,
    private val ethereumRepository: EthereumRepository
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

    private suspend fun getSafeBy(address: Solidity.Address): Safe? = safeDao.loadByAddress(address)

    companion object {

        private const val ACTIVE_SAFE = "prefs.string.active_safe"

        val SAFE_MASTER_COPY_0_0_2 = BuildConfig.SAFE_MASTER_COPY_0_0_2.asEthereumAddress()!!
        val SAFE_MASTER_COPY_0_1_0 = BuildConfig.SAFE_MASTER_COPY_0_1_0.asEthereumAddress()!!
        val SAFE_MASTER_COPY_1_0_0 = BuildConfig.SAFE_MASTER_COPY_1_0_0.asEthereumAddress()!!
        val SAFE_MASTER_COPY_1_1_1 = BuildConfig.SAFE_MASTER_COPY_1_1_1.asEthereumAddress()!!

        fun isSupported(masterCopy: Solidity.Address?) =
            supportedContracts.contains(masterCopy)

        private val supportedContracts = listOf(
            SAFE_MASTER_COPY_0_0_2,
            SAFE_MASTER_COPY_0_1_0,
            SAFE_MASTER_COPY_1_0_0,
            SAFE_MASTER_COPY_1_1_1
        )
    }
}
