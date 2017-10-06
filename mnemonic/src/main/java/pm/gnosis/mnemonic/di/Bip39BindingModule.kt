package pm.gnosis.mnemonic.di

import dagger.Binds
import dagger.Module
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.mnemonic.Bip39Generator
import javax.inject.Singleton

@Module
abstract class Bip39BindingModule {
    @Binds
    @Singleton
    abstract fun bindBip39(bip39Generator: Bip39Generator): Bip39
}
