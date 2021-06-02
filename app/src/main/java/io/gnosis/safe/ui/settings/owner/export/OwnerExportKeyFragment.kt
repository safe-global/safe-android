package io.gnosis.safe.ui.settings.owner.export

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerExportKeyBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import kotlinx.coroutines.launch
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.svalinn.common.utils.withArgs
import timber.log.Timber
import javax.inject.Inject

class OwnerExportKeyFragment : BaseViewBindingFragment<FragmentOwnerExportKeyBinding>() {

    override fun screenId() = ScreenId.OWNER_EXPORT_KEY

    @Inject
    lateinit var qrCodeGenerator: QrCodeGenerator

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerExportKeyBinding =
        FragmentOwnerExportKeyBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val key = requireArguments()[ARGS_KEY] as String
        val showTitle = requireArguments()[ARGS_SHOW_TITLE] as Boolean

        with(binding) {
            title.visible(showTitle)
            value.text = key
            lifecycleScope.launch {
                val keyQr = runCatching {
                    qrCodeGenerator.generateQrCode(
                        key,
                        512,
                        512,
                        Color.WHITE
                    )
                }.onFailure { Timber.e(it) }
                    .getOrNull()
                qrCode.setImageBitmap(keyQr)
            }

        }
    }

    companion object {

        private const val ARGS_KEY = "args.string.key"
        private const val ARGS_SHOW_TITLE = "args.boolean.show_title"

        fun newInstance(key: String, showTitle: Boolean): OwnerExportKeyFragment {
            return OwnerExportKeyFragment().withArgs(Bundle().apply {
                putString(ARGS_KEY, key)
                putBoolean(ARGS_SHOW_TITLE, showTitle)
            }) as OwnerExportKeyFragment
        }
    }
}
