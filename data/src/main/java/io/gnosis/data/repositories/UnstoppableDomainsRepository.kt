package io.gnosis.data.repositories

import android.os.Handler
import android.os.Looper
import com.unstoppabledomains.config.network.model.Network
import com.unstoppabledomains.resolution.Resolution
import com.unstoppabledomains.resolution.naming.service.NamingServiceType
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import java.util.concurrent.Executors


class UnstoppableDomainsRepository() {

    private val library: Resolution = Resolution.builder()
            .infura(NamingServiceType.ENS, Network.MAINNET, "213fff28936343858ca9c5115eff1419")
            .infura(NamingServiceType.CNS, Network.MAINNET, "213fff28936343858ca9c5115eff1419")
            .build();

    suspend fun resolve(domain: String): Solidity.Address? {
        var address: Solidity.Address? = null;
        runBlocking {

            val getAddressResolution = async {
                var addr = library.getAddress(domain, "eth");
                address = addr.asEthereumAddress()!!
            }
            getAddressResolution.await()

        }
        return address;
    }
}