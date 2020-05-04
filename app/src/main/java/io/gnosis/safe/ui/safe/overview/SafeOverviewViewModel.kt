package io.gnosis.safe.ui.safe.overview

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import javax.inject.Inject

class SafeOverviewViewModel
@Inject
constructor(
    val preferencesManager: PreferencesManager
) : ViewModel() {

    fun setTermsAgreed(value: Boolean) {
        preferencesManager.prefs.edit {
            putBoolean(TERMS_AGREED, value)
        }
    }

    fun getTermsAgreed(): Boolean = preferencesManager.prefs.getBoolean(TERMS_AGREED, false)

    companion object {
        private const val TERMS_AGREED = "prefs.boolean.terms_agreed"
    }
    class TermsBottomSheetDialog(context: Context) : BottomSheetDialog(context)

}
