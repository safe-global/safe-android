package pm.gnosis.heimdall.di.components

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.squareup.picasso.Picasso
import dagger.Component
import pm.gnosis.eip712.EIP712JsonParser
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.modules.ApplicationBindingsModule
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.di.modules.ApplicationModule.AppCoroutineDispatchers
import pm.gnosis.heimdall.di.modules.InterceptorsModule
import pm.gnosis.heimdall.di.modules.ViewModelFactoryModule
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.helpers.AppInitManager
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.CrashTracker
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.services.BridgeService
import pm.gnosis.heimdall.services.HeimdallFirebaseService
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.security.EncryptionManager
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ApplicationModule::class,
        ApplicationBindingsModule::class,
        InterceptorsModule::class,
        ViewModelFactoryModule::class
    ]
)
interface ApplicationComponent {
    fun application(): Application

    @ApplicationContext
    fun context(): Context

    fun crashTracker(): CrashTracker
    fun eventTracker(): EventTracker

    fun appInitManager(): AppInitManager

    fun viewModelFactory(): ViewModelProvider.Factory

    fun encryptionManager(): EncryptionManager
    fun qrCodeGenerator(): QrCodeGenerator
    fun addressHelper(): AddressHelper
    fun toolbarHelper(): ToolbarHelper
    fun picasso(): Picasso
    fun eip712JsonParser(): EIP712JsonParser

    fun appDispatchers(): AppCoroutineDispatchers

    // Base injects
    fun inject(activity: BaseActivity)

    fun inject(service: BridgeService)
    fun inject(service: HeimdallFirebaseService)
}
