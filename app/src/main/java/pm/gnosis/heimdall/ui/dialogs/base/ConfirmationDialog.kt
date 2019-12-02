package pm.gnosis.heimdall.ui.dialogs.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.screen_confirmation_dialog.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.utils.colorStatusBar


class ConfirmationDialog : BaseDialog() {

    interface OnDismissListener {
        fun onConfirmationDialogDismiss()
    }

    private var dismissListener: OnDismissListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnDismissListener)
            dismissListener = context
    }

    override fun onDetach() {
        super.onDetach()
        dismissListener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.screen_confirmation_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            congratulations_text.text = getString(it.getInt(ARG_TEXT_RES))
            congratulations_check.setImageResource(it.getInt(ARG_IMAGE_RES))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window.colorStatusBar(R.color.white)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        congratulations_continue.setOnClickListener {
            dismissListener?.onConfirmationDialogDismiss()
            dismiss()
        }
    }

    companion object {
        private const val ARG_IMAGE_RES = "argument.int.image_res"
        private const val ARG_TEXT_RES = "argument.int.text_res"
        fun create(@DrawableRes image: Int, @StringRes text: Int) = ConfirmationDialog().apply {
            arguments = Bundle().apply {
                putInt(ARG_IMAGE_RES, image)
                putInt(ARG_TEXT_RES, text)
            }
        }
    }
}