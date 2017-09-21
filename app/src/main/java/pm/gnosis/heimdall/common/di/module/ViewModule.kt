package pm.gnosis.heimdall.common.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.di.ViewContext

@Module
class ViewModule(val context: Context) {
    @Provides
    @ForView
    @ViewContext
    fun providesContext() = context
}
