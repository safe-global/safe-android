package pm.gnosis.heimdall.common.di.module

import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.common.util.QrCodeGenerator
import pm.gnosis.heimdall.common.util.ZxingQrCodeGenerator
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.impl.SimpleEthereumJsonRpcRepository
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.impls.DefaultMultisigRepository
import pm.gnosis.heimdall.data.repositories.impls.RoomTokenRepository
import javax.inject.Singleton

@Module
abstract class ApplicationBindingsModule {
    @Binds
    @Singleton
    abstract fun bindsEthereumJsonRepository(repository: SimpleEthereumJsonRpcRepository): EthereumJsonRpcRepository

    @Binds
    @Singleton
    abstract fun bindsMultisigRepository(repository: DefaultMultisigRepository): MultisigRepository

    @Binds
    @Singleton
    abstract fun bindsQrCodeGenerator(generator: ZxingQrCodeGenerator): QrCodeGenerator

    @Binds
    @Singleton
    abstract fun bindsTokenRepository(repository: RoomTokenRepository): TokenRepository
}
