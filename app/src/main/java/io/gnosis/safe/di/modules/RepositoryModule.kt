package io.gnosis.safe.di.modules

import com.unstoppabledomains.config.network.model.Network
import com.unstoppabledomains.resolution.DomainResolution
import com.unstoppabledomains.resolution.Resolution
import com.unstoppabledomains.resolution.TickerVersion
import com.unstoppabledomains.resolution.dns.DnsRecord
import com.unstoppabledomains.resolution.dns.DnsRecordsType
import com.unstoppabledomains.resolution.naming.service.NamingServiceType
import dagger.Module
import dagger.Provides
import io.gnosis.data.BuildConfig
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.repositories.*
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.ethereum.rpc.EthereumRpcConnector
import pm.gnosis.ethereum.rpc.RpcEthereumRepository
import pm.gnosis.svalinn.common.PreferencesManager
import timber.log.Timber
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
        val firstWord = BuildConfig.BLOCKCHAIN_NET_URL.removePrefix("https://").split(".").first()
        val network = Network.valueOf(firstWord.toUpperCase())
        return try {
            Resolution.builder()
                .chainId(NamingServiceType.CNS, network)
                .infura(NamingServiceType.CNS, BuildConfig.INFURA_API_KEY)
                .build()
        } catch (throwable: Throwable) {
            Timber.e(throwable, "Error initializing UnstoppableDomains")
            object : DomainResolution {
                override fun getOwner(domain: String?): String {
                    TODO("Not yet implemented")
                }

                override fun email(domain: String?): String {
                    TODO("Not yet implemented")
                }

                override fun getEmail(domain: String?): String {
                    TODO("Not yet implemented")
                }

                override fun namehash(domain: String?): String {
                    TODO("Not yet implemented")
                }

                override fun owner(domain: String?): String {
                    TODO("Not yet implemented")
                }

                override fun addr(domain: String?, ticker: String?): String {
                    TODO("Not yet implemented")
                }

                override fun getNetwork(type: NamingServiceType?): Network {
                    TODO("Not yet implemented")
                }

                override fun ipfsHash(domain: String?): String {
                    TODO("Not yet implemented")
                }

                override fun getMultiChainAddress(domain: String?, ticker: String?, chain: String?): String {
                    TODO("Not yet implemented")
                }

                override fun isSupported(domain: String?): Boolean {
                    TODO("Not yet implemented")
                }

                override fun getDns(domain: String?, types: MutableList<DnsRecordsType>?): MutableList<DnsRecord> {
                    TODO("Not yet implemented")
                }

                override fun getAddress(domain: String?, ticker: String?): String {
                    TODO("Not yet implemented")
                }

                override fun getIpfsHash(domain: String?): String {
                    TODO("Not yet implemented")
                }

                override fun getUsdt(domain: String?, version: TickerVersion?): String {
                    TODO("Not yet implemented")
                }

                override fun getNamehash(domain: String?): String {
                    TODO("Not yet implemented")
                }

            }
        }
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
