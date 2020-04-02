package io.gnosis.heimdall.ui.splash

import android.os.Bundle
import io.gnosis.heimdall.HeimdallApplication
import io.gnosis.heimdall.R
import io.gnosis.heimdall.di.components.DaggerViewComponent
import io.gnosis.heimdall.di.modules.ViewModule
import io.gnosis.heimdall.ui.base.BaseActivity

import javax.inject.Inject

class SplashActivity : BaseActivity() {

    @Inject
    lateinit var viewModel: SplashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_splash_screen)
        inject()
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this])
            .viewModule(ViewModule(this))
            .build().inject(this)
    }
}
