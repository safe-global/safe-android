package io.gnosis.safe.ui.settings.app.passcode


import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.gnosis.safe.R

object BiometricPromptUtils {
    fun createBiometricPrompt(
        fragment: Fragment,
        processSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        authFailed: () -> Unit,
        usePasscode: () -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errCode, errString)
                if (errCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    usePasscode()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                authFailed()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                processSuccess(result)
            }
        }
        return BiometricPrompt(fragment, executor, callback)
    }

    fun createPromptInfo(context: Context): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(context.getString(R.string.biometric_prompt_info_title))
            setSubtitle(context.getString(R.string.biometric_prompt_info_subtitle))
            setDescription(context.getString(R.string.biometric_prompt_info_description))
            setConfirmationRequired(true)
            setNegativeButtonText(context.getString(R.string.biometric_prompt_info_use_app_password))
        }.build()
}
