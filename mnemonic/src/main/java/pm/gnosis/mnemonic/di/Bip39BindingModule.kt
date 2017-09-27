package pm.gnosis.mnemonic.di

import dagger.Binds
import dagger.Module
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.mnemonic.Bip39Generator

@Module
abstract class Bip39BindingModule {
    @Binds
    abstract fun bindBip39(bip39Generator: Bip39Generator): Bip39
}
