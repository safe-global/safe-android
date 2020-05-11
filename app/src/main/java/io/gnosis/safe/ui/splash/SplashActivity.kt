package io.gnosis.safe.ui.splash

import android.content.Intent
import android.os.Bundle
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.databinding.ActivitySplashBinding
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.StartActivity
import io.gnosis.safe.ui.base.BaseActivity
import io.gnosis.safe.ui.safe.terms.TermsBottomSheetDialog
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : BaseActivity() {

    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        inject()

        continue_button.setOnClickListener {
            TermsBottomSheetDialog(this).checkTerms {
                startActivity(Intent(this, StartActivity::class.java))
            }
        }
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this])
            .viewModule(ViewModule(this))
            .build().inject(this)
    }
}
