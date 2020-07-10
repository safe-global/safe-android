package io.gnosis.safe.ui.safe.settings.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewMastercopyItemBinding
import io.gnosis.safe.utils.asMiddleEllipsized
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddressString

class MasterCopyItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewMastercopyItemBinding.inflate(LayoutInflater.from(context), this) }

    var address: Solidity.Address? = null
        set(value) {
            with(binding) {
                blockies.setAddress(value)
                setVersionName(value)
                address.text = value?.asEthereumAddressString()?.asMiddleEllipsized(4)
                binding.root.setOnClickListener {
                    context.openUrl(
                        context.getString(
                            R.string.etherscan_address_url,
                            value?.asEthereumAddressChecksumString()
                        )
                    )
                }
            }
            field = value
        }

    private fun setVersionName(address: Solidity.Address?) {
        with(binding) {
            if (SafeRepository.masterCopyVersion(address).isNullOrBlank()) {
                versionName.text = context.getString(R.string.safe_settings_unknown)
                versionInfo.visible(false)
            } else {
                versionName.text = SafeRepository.masterCopyVersion(address)
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
            }
        }
    }

    private fun buildVersionInfoView(address: Solidity.Address?): VersionInfoView =
        if (SafeRepository.isLatestVersion(address)) VersionInfoView(
            R.drawable.ic_check,
            R.color.safe_green,
            R.string.safe_settings_master_copy_up_to_date
        )
        else VersionInfoView(
            R.drawable.ic_error,
            R.color.tomato,
            R.string.safe_settings_master_copy_upgrade_available
        )

    private data class VersionInfoView(
        @DrawableRes val leftDrawable: Int,
        @ColorRes val infoColor: Int,
        @StringRes val infoText: Int
    )
}
