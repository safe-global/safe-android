package io.gnosis.data.repositories

import com.unstoppabledomains.config.network.model.Network
import com.unstoppabledomains.exceptions.ns.NamingServiceException
import com.unstoppabledomains.resolution.Resolution
import com.unstoppabledomains.resolution.naming.service.NamingServiceType
import io.gnosis.data.BuildConfig
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

class BackgroundTask : Callable<String> {
    var address = ""
    var domain: String;
    private val library: Resolution = Resolution.builder()
            .chainId(NamingServiceType.CNS, Network.RINKEBY)
            .infura(NamingServiceType.CNS, BuildConfig.INFURA_API_KEY)
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
    suspend fun resolve(domain: String): Solidity.Address {
        val futureTask: FutureTask<String> = FutureTask<String>(BackgroundTask(domain))
        val t = Thread(futureTask)
        t.start();
        return futureTask.get().asEthereumAddress()!!;
    }
}