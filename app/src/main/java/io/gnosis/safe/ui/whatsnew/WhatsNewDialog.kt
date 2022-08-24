package io.gnosis.safe.ui.whatsnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.DialogWhatsNewBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseBottomSheetDialogFragment

class WhatsNewDialog : BaseBottomSheetDialogFragment<DialogWhatsNewBinding>() {

    override fun screenId(): ScreenId? = null

    override fun inject(viewComponent: ViewComponent) {
        viewComponent.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        DialogWhatsNewBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog!!.setOnShowListener {
            val dialog = it as BottomSheetDialog
            val parentLayout =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { it ->
                val behaviour = BottomSheetBehavior.from(it)
                setupFullHeight(it)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        binding.continueButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }
}
