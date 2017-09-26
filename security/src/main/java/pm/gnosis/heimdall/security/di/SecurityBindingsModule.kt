package pm.gnosis.heimdall.security.di

import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.security.impls.AesEncryptionManager
import javax.inject.Singleton

@Module
abstract class SecurityBindingsModule {
    @Binds
    @Singleton
    abstract fun bindsEncryptionManager(manager: AesEncryptionManager): EncryptionManager
}