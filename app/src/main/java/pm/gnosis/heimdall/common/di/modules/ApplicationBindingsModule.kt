package pm.gnosis.heimdall.common.di.modules

import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.common.utils.QrCodeGenerator
import pm.gnosis.heimdall.common.utils.ZxingQrCodeGenerator
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.impls.SimpleEthereumJsonRpcRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.impls.DefaultGnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.impls.DefaultSettingsRepository
import pm.gnosis.heimdall.data.repositories.impls.DefaultTokenRepository
import pm.gnosis.heimdall.data.repositories.impls.IpfsTransactionDetailsRepository
import javax.inject.Singleton

@Module
abstract class ApplicationBindingsModule {
    @Binds
    @Singleton
    abstract fun bindsEthereumJsonRepository(repository: SimpleEthereumJsonRpcRepository): EthereumJsonRpcRepository

    @Binds
    @Singleton
    abstract fun bindsTransactionDetailRepository(repository: IpfsTransactionDetailsRepository): TransactionDetailsRepository

    @Binds
    @Singleton
    abstract fun bindsSafeRepository(repository: DefaultGnosisSafeRepository): GnosisSafeRepository

    @Binds
    @Singleton
    abstract fun bindsQrCodeGenerator(generator: ZxingQrCodeGenerator): QrCodeGenerator

    @Binds
    @Singleton
    abstract fun bindsTokenRepository(repository: DefaultTokenRepository): TokenRepository


    @Binds
    @Singleton
    abstract fun bindsSettingsRepository(repository: DefaultSettingsRepository): SettingsRepository
}
