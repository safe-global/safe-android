package pm.gnosis.heimdall.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext

@Module
class ViewModule(val context: Context) {
    @Provides
    @ForView
    @ViewContext
    fun providesContext() = context
}
