package io.gnosis.safe.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.databinding.ActivitySplashBinding
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.StartActivity
import io.gnosis.safe.ui.base.BaseActivity
import io.gnosis.safe.ui.safe.terms.TermsBottomSheetDialog
import io.gnosis.safe.ui.safe.terms.TermsChecker
import kotlinx.coroutines.launch
import javax.inject.Inject

class SplashActivity : BaseActivity() {

    @Inject
    lateinit var termsChecker: TermsChecker

    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        inject()

        binding.continueButton.setOnClickListener {
            lifecycleScope.launch {
                if (termsChecker.getTermsAgreed()) {
                    startStartActivity()
                } else {
                    val termsBottomSheetDialog = TermsBottomSheetDialog()
                    termsBottomSheetDialog.show(supportFragmentManager, "TAG")
                    termsBottomSheetDialog.onAgreeClickListener = {
                        lifecycleScope.launch {
                            termsChecker.setTermsAgreed(true)
                        }
                        startStartActivity()
                    }
                }
            }
        }
    }

    private fun startStartActivity() {
        startActivity(Intent(this, StartActivity::class.java))
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this])
            .viewModule(ViewModule(this))
            .build().inject(this)
    }
}
