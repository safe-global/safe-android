package io.gnosis.safe.ui.splash

import android.os.Bundle
import android.os.Handler
import android.view.View
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
                is SplashViewModel.ShowButton -> {
                    binding.continueButton.visibility = View.VISIBLE
                    binding.continueButton.setOnClickListener {
                        viewModel.onStartClicked()
                    }
                }
            }
        })

        Handler().postDelayed(
            {
                viewModel.skipSplashScreen()
            }, 500
        )
    }
}
