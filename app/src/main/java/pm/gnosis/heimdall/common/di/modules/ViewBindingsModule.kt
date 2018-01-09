package pm.gnosis.heimdall.common.di.modules

import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.helpers.EtherGasStationGasPriceHelper
import pm.gnosis.heimdall.helpers.GasPriceHelper

@Module
abstract class ViewBindingsModule {
    /*
        Helpers
     */
    @Binds
    @ForView
    abstract fun bindsGasPriceHelper(helper: EtherGasStationGasPriceHelper): GasPriceHelper
}
