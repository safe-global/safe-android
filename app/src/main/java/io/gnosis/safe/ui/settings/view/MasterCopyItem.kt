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
import io.gnosis.data.models.Chain
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.utils.SemVer
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewMastercopyItemBinding
import io.gnosis.safe.utils.BlockExplorer
import io.gnosis.safe.utils.abbreviateEthAddress
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible

class MasterCopyItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewMastercopyItemBinding.inflate(LayoutInflater.from(context), this) }


    fun loadKnownAddressLogo(addressUri: String?, address: Solidity.Address?) {
        binding.blockies.loadKnownAddressLogo(addressUri, address)
    }

    fun setAddress(
        chain: Chain?,
        value: Solidity.Address?,
        showChainPrefix: Boolean,
        copyChainPrefix: Boolean,
        version: String? = null,
        showUpdateAvailable: Boolean = true
    ) {
        with(binding) {
            blockies.setAddress(value)
            setVersionName(value, version, showUpdateAvailable)
            address.text = value
                ?.asEthereumAddressChecksumString()
                ?.abbreviateEthAddress(
                    if (showChainPrefix) chain?.shortName else null
                )
            binding.link.setOnClickListener {
                BlockExplorer.forChain(chain)?.showAddress(context, value)
            }
            binding.root.setOnClickListener {
                value?.let {
                    context.copyToClipboard(
                        context.getString(R.string.address_copied),
                        if (copyChainPrefix && !chain?.shortName.isNullOrBlank()) {
                            "${chain?.shortName}:${value.asEthereumAddressChecksumString()}"
                        } else {
                            value.asEthereumAddressChecksumString()
                        }
                    ) {
                        snackbar(view = root, textId = R.string.copied_success)
                    }
                }
            }
        }
    }

    private fun setVersionName(address: Solidity.Address?, version: String? = null, showUpdateAvailable: Boolean) {
        with(binding) {
            implementationVersionName.text =
                version ?: context.getString(R.string.unknown_implementation_version)
            if (showUpdateAvailable) {
                versionInfo.apply {
                    visible(true)
                    val versionInfoView = buildVersionInfoView(version)
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

    private fun buildVersionInfoView(versionString: String?): VersionInfoView {

        val version = kotlin.runCatching {
            versionString?.let {
                SemVer.parse(it)
            }
        }.getOrNull()

        return if (SafeRepository.isUpToDateVersion(version)) {
            VersionInfoView(
                R.drawable.ic_check,
                R.color.primary,
                R.string.safe_settings_master_copy_up_to_date
            )
        } else {
            VersionInfoView(
                R.drawable.ic_error,
                R.color.error,
                R.string.safe_settings_master_copy_upgrade_available
            )
        }
    }

    private data class VersionInfoView(
        @DrawableRes val leftDrawable: Int,
        @ColorRes val infoColor: Int,
        @StringRes val infoText: Int
    )
}
