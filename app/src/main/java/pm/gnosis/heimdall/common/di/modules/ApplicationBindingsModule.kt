package pm.gnosis.heimdall.common.di.modules

import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.common.utils.QrCodeGenerator
import pm.gnosis.heimdall.common.utils.ZxingQrCodeGenerator
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.impls.SimpleEthereumJsonRpcRepository
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.impls.*
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.heimdall.helpers.SimpleSignatureStore
import pm.gnosis.heimdall.reporting.CrashTracker
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.impl.FabricCrashTracker
import pm.gnosis.heimdall.reporting.impl.FabricEventTracker
import javax.inject.Singleton

@Module
abstract class ApplicationBindingsModule {

    @Binds
    @Singleton
    abstract fun bindsCrashTracker(tracker: FabricCrashTracker): CrashTracker

    @Binds
    @Singleton
    abstract fun bindsEventTracker(tracker: FabricEventTracker): EventTracker

    /*
        Helpers
     */

    @Binds
    abstract fun bindsSignatureStore(helper: SimpleSignatureStore): SignatureStore

    /*
        Repositories
     */

    @Binds
    @Singleton
    abstract fun bindsAddressBookRepository(repository: DefaultAddressBookRepository): AddressBookRepository

    @Binds
    @Singleton
    abstract fun bindsEthereumJsonRepository(repository: SimpleEthereumJsonRpcRepository): EthereumJsonRpcRepository

    @Binds
    @Singleton
    abstract fun bindsQrCodeGenerator(generator: ZxingQrCodeGenerator): QrCodeGenerator

    @Binds
    @Singleton
    abstract fun bindsSafeRepository(repository: DefaultGnosisSafeRepository): GnosisSafeRepository

    @Binds
    @Singleton
    abstract fun bindsSettingsRepository(repository: DefaultSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindsTokenRepository(repository: DefaultTokenRepository): TokenRepository

    @Binds
    @Singleton
    abstract fun bindsTransactionRepository(repository: GnosisSafeTransactionRepository): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindsTransactionDetailRepository(repository: IpfsTransactionDetailsRepository): TransactionDetailsRepository
}
