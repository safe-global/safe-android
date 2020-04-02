package io.gnosis.heimdall.ui.base

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import io.gnosis.heimdall.BuildConfig
import io.gnosis.heimdall.HeimdallApplication
import io.gnosis.heimdall.di.components.DaggerViewComponent
import io.gnosis.heimdall.di.components.ViewComponent
import io.gnosis.heimdall.di.modules.ViewModule

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HeimdallApplication[this].inject(this)
        if (!BuildConfig.DEBUG) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    protected fun viewComponent(): ViewComponent =
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this])
            .viewModule(ViewModule(this))
            .build()
}
