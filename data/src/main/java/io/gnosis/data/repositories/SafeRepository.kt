package io.gnosis.data.repositories

import android.content.SharedPreferences
import io.gnosis.contracts.BuildConfig
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.Chain
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
import pm.gnosis.utils.nullOnThrow
import java.math.BigInteger

class SafeRepository(
    private val safeDao: SafeDao,
    private val preferencesManager: PreferencesManager,
    private val gatewayApi: GatewayApi
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

    suspend fun isSafeAddressUsed(safe: Safe): Boolean = safeDao.loadByAddressAndChainId(safe.address, safe.chainId) != null

    suspend fun getSafes(): List<Safe> = safeDao.loadAllWithChainData().map {
        val safe = it.safe
        safe.chain = it.chain?.apply {
            currency = it.currency ?: Chain.Currency.DEFAULT_CURRENCY
        }
            ?: Chain.DEFAULT_CHAIN
        safe
    }

    suspend fun getSafesForChain(chainId: BigInteger): List<Safe> = safeDao.loadAllByChain(chainId).map {
        val safe = it.safe
        safe.chain = it.chain!!
        safe
    }

    suspend fun getSafeCount(): Int = safeDao.safeCount()

    suspend fun saveSafe(safe: Safe) = safeDao.insert(safe)

    suspend fun removeSafe(safe: Safe) = safeDao.delete(safe)

    suspend fun clearActiveSafe() {
        preferencesManager.prefs.edit {
            remove(ACTIVE_SAFE)
        }
    }

    suspend fun setActiveSafe(safe: Safe) {
        preferencesManager.prefs.edit {
            putString(ACTIVE_SAFE, "${safe.address.asEthereumAddressString()};${safe.localName};${safe.chainId}")
        }
    }

    suspend fun getActiveSafe(): Safe? {
        val activeSafeData = preferencesManager.prefs.getString(ACTIVE_SAFE, null)?.split(";")
        val address = activeSafeData?.get(0)?.asEthereumAddress()
        return address?.let {
            val chainId = nullOnThrow { activeSafeData[2].toBigInteger() }
            // safes from old version won't have chain id saved
            if (chainId != null) getSafeBy(it, chainId) else getSafeBy(it)
        }
    }

    suspend fun getSafeBy(address: Solidity.Address): Safe? {
        val safeWithChainData = safeDao.loadByAddressWithChainData(address)
        val safe = safeWithChainData?.safe
        safe?.let {
            it.chain = safeWithChainData.chain?.apply {
                currency = safeWithChainData.currency ?: Chain.Currency.DEFAULT_CURRENCY
            }
                ?: Chain.DEFAULT_CHAIN
        }
        return safe
    }

    suspend fun getSafeBy(address: Solidity.Address, chainId: BigInteger): Safe? {
        val safeWithChainData = safeDao.loadByAddressWithChainData(address, chainId)
        val safe = safeWithChainData?.safe
        safe?.apply {
            chain = safeWithChainData.chain?.apply {
                currency = safeWithChainData.currency ?: Chain.Currency.DEFAULT_CURRENCY
            }
                ?: Chain.DEFAULT_CHAIN
        }
        return safe
    }

    suspend fun getSafeMetas(): List<SafeMetaData> = safeDao.getMetas()

    suspend fun getSafeMeta(address: Solidity.Address): SafeMetaData? = safeDao.getMeta(address)

    suspend fun saveSafeMeta(safeMeta: SafeMetaData) = safeDao.saveMeta(safeMeta)

    suspend fun getSafeStatus(safe: Safe): SafeStatus {

        val safeInfo = gatewayApi.getSafeInfo(address = safe.address.asEthereumAddressChecksumString(), chainId = safe.chainId)

        val supportedContracts = setOf(
            SAFE_IMPLEMENTATION_1_0_0,
            SAFE_IMPLEMENTATION_1_1_1,
            SAFE_IMPLEMENTATION_1_2_0
        )

        return when {
            safeInfo != null && safeInfo.implementation.value in supportedContracts -> SafeStatus.VALID
            safeInfo != null -> SafeStatus.NOT_SUPPORTED
            else -> SafeStatus.INVALID
        }
    }

    suspend fun getSafeInfo(safe: Safe): SafeInfo =
        gatewayApi.getSafeInfo(address = safe.address.asEthereumAddressChecksumString(), chainId = safe.chainId)

    suspend fun clearUserData() {
        getSafes().forEach {
            removeSafe(it)
        }
        clearActiveSafe()
    }

    companion object {

        private const val ACTIVE_SAFE = "prefs.string.active_safe"

        val SAFE_IMPLEMENTATION_0_0_2 = BuildConfig.SAFE_IMPLEMENTATION_0_0_2.asEthereumAddress()!!
        val SAFE_IMPLEMENTATION_0_1_0 = BuildConfig.SAFE_IMPLEMENTATION_0_1_0.asEthereumAddress()!!
        val SAFE_IMPLEMENTATION_1_0_0 = BuildConfig.SAFE_IMPLEMENTATION_1_0_0.asEthereumAddress()!!
        val SAFE_IMPLEMENTATION_1_1_1 = BuildConfig.SAFE_IMPLEMENTATION_1_1_1.asEthereumAddress()!!
        val SAFE_IMPLEMENTATION_1_2_0 = BuildConfig.SAFE_IMPLEMENTATION_1_2_0.asEthereumAddress()!!

        val DEFAULT_FALLBACK_HANDLER = BuildConfig.DEFAULT_FALLBACK_HANDLER.asEthereumAddress()!!

        fun isLatestVersion(address: Solidity.Address?): Boolean = address == SAFE_IMPLEMENTATION_1_1_1 || address == SAFE_IMPLEMENTATION_1_2_0

        const val METHOD_SET_FALLBACK_HANDLER = "setFallbackHandler"
        const val METHOD_ADD_OWNER_WITH_THRESHOLD = "addOwnerWithThreshold"
        const val METHOD_REMOVE_OWNER = "removeOwner"
        const val METHOD_SWAP_OWNER = "swapOwner"
        const val METHOD_CHANGE_THRESHOLD = "changeThreshold"
        const val METHOD_CHANGE_MASTER_COPY = "changeMasterCopy"
        const val METHOD_ENABLE_MODULE = "enableModule"
        const val METHOD_DISABLE_MODULE = "disableModule"

    }

//    suspend fun getChainInfo(): List<ChainInfo> = gatewayApi.loadChainInfo().results

}

enum class SafeStatus {
    VALID,
    INVALID,
    NOT_SUPPORTED
}
