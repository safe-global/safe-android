package io.gnosis.data.repositories

import androidx.annotation.VisibleForTesting
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.Safe
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class SafeRepository(
    private val safeDao: SafeDao,
    private val preferenceManager: PreferencesManager
) {

    suspend fun getSafes(): List<Safe> = safeDao.loadAll().asList()

    suspend fun addSafe(safe: Safe) = safeDao.insert(safe)

    suspend fun removeSafe(safe: Safe) = safeDao.delete(safe)

    suspend fun setActiveSafe(safe: Safe) {
        preferenceManager.prefs.edit {
            putString(ACTIVE_SAFE, safe.address.asEthereumAddressString())
        }
    }

    suspend fun getActiveSafe(): Safe? {
        return preferenceManager.prefs.getString(ACTIVE_SAFE, null)?.asEthereumAddress()?.let { address ->
            getSafeBy(address)
        }
    }

    private suspend fun getSafeBy(address: Solidity.Address): Safe? {
        return safeDao.loadByAddress(address)
    }

    companion object {

        @VisibleForTesting
        const val ACTIVE_SAFE = "prefs.string.active_safe"
    }
}
