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
import io.gnosis.safe.ui.safe.terms.TermsViewModel
import kotlinx.android.synthetic.main.activity_splash.*
import javax.inject.Inject

class SplashActivity : BaseActivity() {

    @Inject
    lateinit var viewModel: TermsViewModel

    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        inject()
        viewModel.termsBottomSheetDialog = TermsBottomSheetDialog(this)
        continue_button.setOnClickListener {
            viewModel.checkTerms {
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
