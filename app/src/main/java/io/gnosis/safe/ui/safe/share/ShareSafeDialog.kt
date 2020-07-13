package io.gnosis.safe.ui.safe.share

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.R
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.modules.ViewModule
import javax.inject.Inject

class ShareSafeDialog : DialogFragment() {

    @Inject
    lateinit var viewModel: ShareSafeViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        inject()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NO_FRAME, 0)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_share_safe, null)
        return AlertDialog.Builder(requireContext()).setView(view).create()
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(requireContext()))
            .applicationComponent(HeimdallApplication[requireContext()])
            .build()
            .inject(this)
    }
}
