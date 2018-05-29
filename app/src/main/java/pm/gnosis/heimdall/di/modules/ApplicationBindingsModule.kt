package pm.gnosis.heimdall.di.modules

import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.data.remote.MessageQueueRepository
import pm.gnosis.heimdall.data.remote.PushServiceRepository
import pm.gnosis.heimdall.data.remote.impls.FirebaseMessageQueueRepository
import pm.gnosis.heimdall.data.remote.impls.GnosisSafePushServiceRepository
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.impls.*
import pm.gnosis.heimdall.helpers.*
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

    // This is unscoped so it will get recreated each time it is injected
    @Binds
    abstract fun bindsAddressStore(helper: SimpleAddressStore): AddressStore

    // This is unscoped so it will get recreated each time it is injected
    @Binds
    abstract fun bindsSignatureStore(helper: SimpleSignatureStore): SignatureStore

    @Binds
    @Singleton
    abstract fun bindsLocalNotificationManager(manager: AndroidLocalNotificationManager): LocalNotificationManager

    /*
        Repositories
     */

    @Binds
    @Singleton
    abstract fun bindsAddressBookRepository(repository: DefaultAddressBookRepository): AddressBookRepository

    @Binds
    @Singleton
    abstract fun bindsBillingRepository(repository: PlayBillingRepository): BillingRepository

    @Binds
    @Singleton
    abstract fun bindsMessageQueueRepository(repository: FirebaseMessageQueueRepository): MessageQueueRepository

    @Binds
    @Singleton
    abstract fun bindsPushServiceRepository(repository: GnosisSafePushServiceRepository): PushServiceRepository

    @Binds
    @Singleton
    abstract fun bindsSafeRepository(repository: DefaultGnosisSafeRepository): GnosisSafeRepository

    @Binds
    @Singleton
    abstract fun bindsSafeExtensionRepository(repository: DefaultGnosisSafeExtensionRepository): GnosisSafeExtensionRepository

    @Binds
    @Singleton
    abstract fun bindsSettingsRepository(repository: DefaultSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindsSignaturePushRepository(repository: DefaultSignaturePushRepository): SignaturePushRepository

    @Binds
    @Singleton
    abstract fun bindsTokenRepository(repository: DefaultTokenRepository): TokenRepository

    @Binds
    @Singleton
    abstract fun bindsTransactionRepository(repository: GnosisSafeTransactionRepository): TransactionExecutionRepository

    @Binds
    @Singleton
    abstract fun bindsTransactionInfoRepository(repository: DefaultTransactionInfoRepository): TransactionInfoRepository

    @Binds
    @Singleton
    abstract fun bindsTxExecutorRepository(repository: DefaultTxExecutorRepository): TxExecutorRepository
}
