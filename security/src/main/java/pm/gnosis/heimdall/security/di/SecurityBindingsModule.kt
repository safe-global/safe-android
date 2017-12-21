package pm.gnosis.heimdall.security.di

import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.security.FingerprintHelper
import pm.gnosis.heimdall.security.impls.AesEncryptionManager
import pm.gnosis.heimdall.security.impls.DefaultFingerprintHelper
import javax.inject.Singleton

@Module
abstract class SecurityBindingsModule {
    @Binds
    @Singleton
    abstract fun bindsEncryptionManager(manager: AesEncryptionManager): EncryptionManager

    @Binds
    @Singleton
    abstract fun bindsFingerprintHelper(fingerprintHelper: DefaultFingerprintHelper): FingerprintHelper
}
