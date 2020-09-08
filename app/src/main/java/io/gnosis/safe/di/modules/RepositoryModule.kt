package io.gnosis.safe.di.modules

import dagger.Module
import dagger.Provides
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.db.daos.Erc20TokenDao
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.repositories.*
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
        transactionServiceApi: TransactionServiceApi
    ): SafeRepository {
        return SafeRepository(safeDao, preferencesManager, transactionServiceApi)
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
    fun providesTokenRepository(
        erc20TokenDao: Erc20TokenDao,
        transactionServiceApi: TransactionServiceApi
    ): TokenRepository =
        TokenRepository(erc20TokenDao, transactionServiceApi)

    @Provides
    @Singleton
    fun providesTransactionRepository(gatewayApi: GatewayApi): TransactionRepository =
        TransactionRepository(gatewayApi)
}
