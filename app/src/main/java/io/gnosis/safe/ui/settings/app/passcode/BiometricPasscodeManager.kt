package io.gnosis.safe.ui.settings.app.passcode

import android.content.Context
import androidx.biometric.BiometricManager

fun Context.canAuthenticateUsingBiometrics() = BiometricManager.from(this)
    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
