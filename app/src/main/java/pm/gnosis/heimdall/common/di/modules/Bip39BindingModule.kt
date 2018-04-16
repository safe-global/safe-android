package pm.gnosis.heimdall.common.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.mnemonic.Bip39Generator
import pm.gnosis.mnemonic.android.DefaultWordListProvider
import pm.gnosis.svalinn.common.di.ApplicationContext

@Module
class Bip39BindingModule {
    @Provides
    fun providesBip39(@ApplicationContext context: Context): Bip39 =
        Bip39Generator(DefaultWordListProvider(context))
}
