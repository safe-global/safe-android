package io.gnosis.safe.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.databinding.ActivitySplashBinding
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.base.BaseActivity
import io.gnosis.safe.ui.StartActivity
import javax.inject.Inject

class SplashActivity : BaseActivity() {

    @Inject
    lateinit var viewModel: SplashViewModel

    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        inject()
        Handler().postDelayed({
            startActivity(Intent(this, StartActivity::class.java))
        }, 500)
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this])
            .viewModule(ViewModule(this))
            .build().inject(this)
    }
}
