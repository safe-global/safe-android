package io.gnosis.safe.ui.splash

import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.ActivitySplashBinding
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction
import io.gnosis.safe.ui.base.activity.BaseActivity
import io.gnosis.safe.ui.terms.TermsBottomSheetDialog
import kotlinx.coroutines.launch
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class SplashActivity : BaseActivity() {

    @Inject
    lateinit var viewModel: SplashViewModel

    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }
    private val termsBottomSheetDialog = TermsBottomSheetDialog()
    private lateinit var pushNotificationPermissionLauncher: ActivityResultLauncher<String>

    override fun screenId() = ScreenId.LAUNCH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        pushNotificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

        viewComponent().inject(this)

        viewModel.state.observe(this) {
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
                        onAgreeWithoutSharingUsageDataClickListener = {
                            settingsHandler.trackingAllowed = false
                            settingsHandler.allowTracking(requireContext(), false)
                            viewModel.handleAgreeClicked()
                        }
                    }
                    if (!termsBottomSheetDialog.isAdded) {
                        termsBottomSheetDialog.show(
                            supportFragmentManager,
                            TermsBottomSheetDialog::class.simpleName
                        )
                    }
                }
                is SplashViewModel.ShowButton -> {
                    requestPushPermission()
                    binding.continueButton.visible(true)
                    binding.continueButton.setOnClickListener {
                        viewModel.onStartClicked()
                    }
                }
            }
        }

        Handler().postDelayed(
            {
                viewModel.skipGetStartedButtonWhenTermsAgreed()
            }, 500
        )

        lifecycleScope.launch {
            viewModel.onAppStart()
        }
    }

    private fun requestPushPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pushNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
