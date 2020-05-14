package io.gnosis.safe.ui.splash

import android.os.Bundle
import androidx.lifecycle.Observer
import io.gnosis.safe.databinding.ActivitySplashBinding
import io.gnosis.safe.ui.base.BaseActivity
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction
import io.gnosis.safe.ui.safe.terms.TermsBottomSheetDialog
import javax.inject.Inject

class SplashActivity : BaseActivity() {

    @Inject
    lateinit var viewModel: SplashViewModel

    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }
    private val termsBottomSheetDialog = TermsBottomSheetDialog()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        viewComponent().inject(this)

        viewModel.state.observe(this, Observer {
            when (val viewAction = it.viewAction) {
                is ViewAction.StartActivity -> {
                    startActivity(viewAction.intent)
                    finish()
                }
                is SplashViewModel.ShowTerms -> {
                    termsBottomSheetDialog.apply {
                        onAgreeClickListener = {
                            viewModel.handleAgreeClicked()
                        }
                    }.show(supportFragmentManager, TermsBottomSheetDialog::class.simpleName)
                }
            }
        })

        binding.continueButton.setOnClickListener {
            viewModel.onStartClicked()
        }
    }
}
