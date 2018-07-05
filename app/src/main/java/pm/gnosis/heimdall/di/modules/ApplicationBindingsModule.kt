package pm.gnosis.heimdall.di.modules

import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.impls.*
import pm.gnosis.heimdall.helpers.*
import pm.gnosis.heimdall.reporting.CrashTracker
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.impl.FabricCrashTracker
import pm.gnosis.heimdall.reporting.impl.FabricEventTracker
import pm.gnosis.heimdall.ui.transactions.view.helpers.DefaultSubmitTransactionHelper
import pm.gnosis.heimdall.ui.transactions.view.helpers.DefaultTransactionViewHolderBuilder
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper
import pm.gnosis.heimdall.ui.transactions.view.helpers.TransactionViewHolderBuilder
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

    // This is unscoped so it will get recreated each time it is injected
    @Binds
    abstract fun bindsSubmitTransactionHelper(helper: DefaultSubmitTransactionHelper): SubmitTransactionHelper

    // This is unscoped so it will get recreated each time it is injected
    @Binds
    abstract fun bindsTransactionViewHolderBuilder(helper: DefaultTransactionViewHolderBuilder): TransactionViewHolderBuilder

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
    abstract fun bindsPushServiceRepository(repository: DefaultPushServiceRepository): PushServiceRepository

    @Binds
    @Singleton
    abstract fun bindsSafeRepository(repository: DefaultGnosisSafeRepository): GnosisSafeRepository

    @Binds
    @Singleton
    abstract fun bindsShortcutRepository(repository: DefaultShortcutRepository): ShortcutRepository

    @Binds
    @Singleton
    abstract fun bindsTokenRepository(repository: DefaultTokenRepository): TokenRepository

    @Binds
    @Singleton
    abstract fun bindsTransactionRepository(repository: DefaultTransactionExecutionRepository): TransactionExecutionRepository

    @Binds
    @Singleton
    abstract fun bindsTransactionInfoRepository(repository: DefaultTransactionInfoRepository): TransactionInfoRepository
}
