package io.gnosis.safe.ui.safe.share

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import javax.inject.Inject

class ShareSafeDialog : DialogFragment() {

    @Inject
    lateinit var viewModel: ShareSafeViewModel
}
