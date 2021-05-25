package io.gnosis.data.repositories

import com.unstoppabledomains.resolution.DomainResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress

class UnstoppableDomainsRepository(
    private val domainResolutionLibrary: DomainResolution
) {

    suspend fun resolve(domain: String): Solidity.Address {
        val address = withContext(Dispatchers.IO) {
            domainResolutionLibrary.getAddress(domain, "eth")
        };
        return address.asEthereumAddress()!!;
    }
}
