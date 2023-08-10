package io.gnosis.safe.di.modules

import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.backend.rpc.RpcClient
import io.gnosis.data.db.daos.ChainDao
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.repositories.*
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.workers.WorkRepository
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
    fun provideChainRepository(
        chainDao: ChainDao,
        gatewayApi: GatewayApi
    ): ChainInfoRepository {
        return ChainInfoRepository(chainDao, gatewayApi)
    }

    @Provides
    @Singleton
    fun providesEthereumRepository(ethereumRpcConnector: EthereumRpcConnector): EthereumRepository =
        RpcEthereumRepository(ethereumRpcConnector, BuildConfig.BLOCKCHAIN_NET_URL)

    @Provides
    @Singleton
    fun providesRpcClient(ethereumRepository: EthereumRepository): RpcClient =
        RpcClient(ethereumRepository)

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
    fun providesUnstoppableRepository(): UnstoppableDomainsRepository =
        UnstoppableDomainsRepository()

    @Provides
    @Singleton
    fun providesTokenRepository(gatewayApi: GatewayApi): TokenRepository =
        TokenRepository(gatewayApi)

    @Provides
    @Singleton
    fun providesTransactionRepository(gatewayApi: GatewayApi): TransactionRepository =
        TransactionRepository(gatewayApi)

    @Provides
    @Singleton
    fun providesWorkRepository(workManager: WorkManager): WorkRepository =
        WorkRepository(workManager)
}
