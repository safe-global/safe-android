package io.gnosis.safe.ui.settings.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewMastercopyItemBinding
import io.gnosis.safe.utils.abbreviateEthAddress
import io.gnosis.safe.utils.implementationVersion
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible

class MasterCopyItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewMastercopyItemBinding.inflate(LayoutInflater.from(context), this) }

    fun setAddress(value: Solidity.Address?, showUpdateAvailable: Boolean = true) {
        with(binding) {
            blockies.setAddress(value)
            setVersionName(value, showUpdateAvailable)
            address.text = value?.asEthereumAddressChecksumString()?.abbreviateEthAddress()

            binding.link.setOnClickListener {
                context.openUrl(
                    context.getString(
                        R.string.etherscan_address_url,
                        value?.asEthereumAddressChecksumString()
                    )
                )
            }

            binding.root.setOnClickListener {
                value?.let {
                    context.copyToClipboard(context.getString(R.string.address_copied), value.asEthereumAddressChecksumString()) {
                        snackbar(view = root, textId = R.string.copied_success)
                    }
                }
            }
        }
    }

    private fun setVersionName(address: Solidity.Address?, showUpdateAvailable: Boolean) {
        with(binding) {
            if (address?.implementationVersion() == null) {
                implementationVersionName.text = context.getString(R.string.unknown_implementation_version)
                versionInfo.visible(false, View.INVISIBLE)
            } else {
                implementationVersionName.text = context.getString(address.implementationVersion() ?: R.string.unknown_implementation_version)
                if (showUpdateAvailable) {
                    versionInfo.apply {
                        visible(true)
                        val versionInfoView = buildVersionInfoView(address)
                        setCompoundDrawablesWithIntrinsicBounds(
                            ResourcesCompat.getDrawable(resources, versionInfoView.leftDrawable, context.theme),
                            null,
                            null,
                            null
                        )
                        setTextColor(ResourcesCompat.getColor(resources, versionInfoView.infoColor, context.theme))
                        setText(versionInfoView.infoText)
                    }
                } else {
                    versionInfo.visible(false, View.INVISIBLE)
                }
            }
        }
    }

    private fun buildVersionInfoView(address: Solidity.Address?): VersionInfoView =
        if (SafeRepository.isLatestVersion(address)) VersionInfoView(
            R.drawable.ic_check,
            R.color.primary,
            R.string.safe_settings_master_copy_up_to_date
        )
        else VersionInfoView(
            R.drawable.ic_error,
            R.color.error,
            R.string.safe_settings_master_copy_upgrade_available
        )

    private data class VersionInfoView(
        @DrawableRes val leftDrawable: Int,
        @ColorRes val infoColor: Int,
        @StringRes val infoText: Int
    )
}
