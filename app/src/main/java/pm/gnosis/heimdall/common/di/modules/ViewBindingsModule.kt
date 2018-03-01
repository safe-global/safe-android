package pm.gnosis.heimdall.common.di.modules

import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.helpers.EtherGasStationGasPriceHelper
import pm.gnosis.heimdall.helpers.GasPriceHelper
import pm.gnosis.svalinn.common.di.ForView

@Module
abstract class ViewBindingsModule {
    /*
        Helpers
     */
    @Binds
    @ForView
    abstract fun bindsGasPriceHelper(helper: EtherGasStationGasPriceHelper): GasPriceHelper
}
