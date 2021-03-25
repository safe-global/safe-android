package io.gnosis.safe.di.modules

import com.unstoppabledomains.config.network.model.Network
import com.unstoppabledomains.resolution.DomainResolution
import com.unstoppabledomains.resolution.Resolution
import com.unstoppabledomains.resolution.naming.service.NamingServiceType
import dagger.Module
import dagger.Provides
import io.gnosis.data.BuildConfig
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.repositories.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.ethereum.rpc.EthereumRpcConnector
import pm.gnosis.ethereum.rpc.RpcEthereumRepository
import pm.gnosis.svalinn.common.PreferencesManager
import javax.inject.Singleton

@Module
class RepositoryModule {

    @Provides
    @Singleton
    fun provideSafeRepository(
        safeDao: SafeDao,
        preferencesManager: PreferencesManager,
        gatewayApi: GatewayApi
    ): SafeRepository {
        return SafeRepository(safeDao, preferencesManager, gatewayApi)
    }

    @Provides
    @Singleton
    fun providesEthereumRepository(ethereumRpcConnector: EthereumRpcConnector): EthereumRepository =
        RpcEthereumRepository(ethereumRpcConnector)

    @Provides
    @Singleton
    fun providesEnsNormalizer(): EnsNormalizer = IDNEnsNormalizer()

    @Provides
    @Singleton
    fun providesEnsRepository(
        ethereumRepository: EthereumRepository,
        ensNormalizer: EnsNormalizer
    ): EnsRepository =
        EnsRepository(ensNormalizer, ethereumRepository)

    @Provides
    @Singleton
    fun providesDomainResolutionLibrary(): DomainResolution {
        val firstWord = BuildConfig.BLOCKCHAIN_NET_URL.removePrefix("https://").split(".").first();
        val network = Network.valueOf(firstWord.toUpperCase());
        return Resolution.builder()
                .chainId(NamingServiceType.CNS, network)
                .infura(NamingServiceType.CNS, BuildConfig.INFURA_API_KEY)
                .build()
    }

    @Provides
    @Singleton
    fun providesUnstoppableRepository(
            domainResolutionLibrary: DomainResolution
    ): UnstoppableDomainsRepository =
            UnstoppableDomainsRepository(domainResolutionLibrary)

    @Provides
    @Singleton
    fun providesTokenRepository(gatewayApi: GatewayApi): TokenRepository =
        TokenRepository(gatewayApi)


    @Provides
    @Singleton
    fun providesTransactionRepository(gatewayApi: GatewayApi): TransactionRepository =
        TransactionRepository(gatewayApi)
}
