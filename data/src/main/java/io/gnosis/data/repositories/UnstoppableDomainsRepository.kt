package io.gnosis.data.repositories

import com.unstoppabledomains.config.network.model.Network
import com.unstoppabledomains.resolution.Resolution
import com.unstoppabledomains.resolution.naming.service.NamingServiceType
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

    class FactorialTask : Callable<String> {
        var address = ""
        var domain: String;
        private val library: Resolution = Resolution.builder()
                .infura(NamingServiceType.ENS, Network.MAINNET, "213fff28936343858ca9c5115eff1419")
                .infura(NamingServiceType.CNS, Network.MAINNET, "213fff28936343858ca9c5115eff1419")
                .build();

        constructor(domain: String)  {
            this.domain = domain;
        }

        override fun call(): String {
            address = library.getAddress(domain, "eth");
            return address
        }
    }




class UnstoppableDomainsRepository() {

    private val library: Resolution = Resolution.builder()
            .infura(NamingServiceType.ENS, Network.MAINNET, "213fff28936343858ca9c5115eff1419")
            .infura(NamingServiceType.CNS, Network.MAINNET, "213fff28936343858ca9c5115eff1419")
            .build();

    suspend fun resolve(domain: String): Solidity.Address? {
        val futureTask: FutureTask<String> = FutureTask<String>(FactorialTask(domain))
        val t = Thread(futureTask)
        t.start();
        try {
            var result = futureTask.get();
            return result.asEthereumAddress()!!;
        } catch(error: Exception) {
            print(error.message);
            return null;
        }
    }
}