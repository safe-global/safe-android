package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.LayoutParams
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.transaction.DataDecoded
import io.gnosis.data.models.transaction.Param
import io.gnosis.data.models.transaction.ParamType
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsActionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.*
import io.gnosis.safe.utils.ParamSerializer
import io.gnosis.safe.utils.dpToPx
import io.gnosis.safe.utils.shortChecksumString
import io.gnosis.safe.utils.toColor
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.removeHexPrefix
import javax.inject.Inject

class TransactionDetailsActionFragment : BaseViewBindingFragment<FragmentTransactionDetailsActionBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_DETAILS_ACTION

    private val navArgs by navArgs<TransactionDetailsActionFragmentArgs>()
    private val chain by lazy { navArgs.chain }
    private val action by lazy { navArgs.action }
    private val data by lazy { navArgs.data }
    private val decodedData by lazy { navArgs.decodedData?.let { paramSerializer.deserializeDecodedData(it) } }
    private val address by lazy { navArgs.address?.asEthereumAddress() }
    private val amount by lazy { navArgs.amount }
    private val addressInfoIndex by lazy { paramSerializer.deserializeAddressInfoIndex(navArgs.addressInfoIndex) }

    @Inject
    lateinit var paramSerializer: ParamSerializer

    @Inject
    lateinit var viewModel: TransactionDetailsActionViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionDetailsActionBinding =
        FragmentTransactionDetailsActionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {

            chainRibbon.text = chain.name
            chainRibbon.setTextColor(chain.textColor.toColor(requireContext(), R.color.white))
            chainRibbon.setBackgroundColor(chain.backgroundColor.toColor(requireContext(), R.color.primary))

            title.text = action
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer {
            when(val viewAction = it.viewAction) {
                is Loading -> {
                    if (!viewAction.isLoading) {
                        updateUi(decodedData, address, amount, it.addressInfoIndex)
                    }
                }
            }
        })
        
        viewModel.extendAddressInfoIndexWithLocalData(addressInfoIndex)
    }

    private fun updateUi(decodedDto: DataDecoded?, address: Solidity.Address? = null, amount: String? = null, addressInfoIndex: Map<String, AddressInfo>? = null) {

        binding.content.removeAllViews()

        address?.let {
            with(binding) {
                content.addView(getTransferItem(it, amount ?: "", addressInfoIndex?.get(it.asEthereumAddressChecksumString())))
                content.addView(getDivider())
            }
        }
        data?.let {
            binding.content.addView(getDataItem(getString(R.string.tx_details_data), it))
        }

        decodedDto?.let {
            with(binding) {

                if (it.parameters?.size ?: 0 > 0) {
                    content.addView(getDivider())
                }

                it.parameters?.forEach {
                    when (it) {
                        is Param.Address -> {
                            content.addView(getLabeledAddressItem("${it.name}(${it.type}):", it.value, addressInfoIndex?.get(it.value.asEthereumAddressChecksumString())))
                        }
                        is Param.Array -> {
                            content.addView(getArrayItem("${it.name}(${it.type}):", it.value, it.getItemType(), it.type, addressInfoIndex))
                        }
                        is Param.Bytes -> {
                            content.addView(getDataItem("${it.name}(${it.type}):", it.value))
                        }
                        is Param.Value -> {
                            if (it.isBytesValue()) {
                                content.addView(getDataItem("${it.name}(${it.type}):", it.value as String))
                            } else {
                                content.addView(getLabeledValueItem("${it.name}(${it.type}):", it.value.toString()))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getTransferItem(address: Solidity.Address, amount: String, addressInfo: AddressInfo?): TxTransferActionView {
        val item = TxTransferActionView(requireContext())
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.setMargins(0, dpToPx(16), 0, -dpToPx(8))
        item.layoutParams = layoutParams
        item.setActionInfo(
            chain,
            outgoing = true,
            amount = amount,
            logoUri = chain.currency.logoUri,
            address = address,
            addressName = addressInfo?.name,
            addressUri = addressInfo?.logoUri
        )
        return item
    }

    private fun getArrayItem(name: String, value: List<Any>, paramType: ParamType, paramTypeValue: String, addressInfoIndex: Map<String, AddressInfo>?): LabeledArrayItem {
        val item = LabeledArrayItem(requireContext())
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, 0)
        item.layoutParams = layoutParams
        item.label = name
        item.showArray(chain ,value, paramType, paramTypeValue, addressInfoIndex)
        return item
    }

    private fun getDataItem(name: String, value: String): TxDataView {
        val item = TxDataView(requireContext())
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.setMargins(dpToPx(16), dpToPx(16), dpToPx(16), 0)
        item.layoutParams = layoutParams
        val size = value.removeHexPrefix().length / 2
        item.setData(value, size, name)
        return item
    }

    private fun getLabeledAddressItem(name: String, value: Solidity.Address, addressInfo: AddressInfo?): View {
        var item: View
        if (addressInfo == null) {
            item = LabeledAddressItem(requireContext())
            item.label = name
            item.setAddress(chain, value)
        } else {
            item = LabeledNamedAddressItem(requireContext())
            item.label = name
            item.setAddress(chain, value)
            // only old imported owner keys could have empty names
            item.name = if (addressInfo.name.isNullOrBlank()) {
                getString(R.string.settings_app_imported_owner_key_default_name, value.shortChecksumString())
            } else {
                addressInfo.name
            }
            addressInfo.logoUri?.let { item.loadKnownAddressLogo(it, value)  }
        }
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, 0)
        item.layoutParams = layoutParams
        return item
    }

    private fun getLabeledValueItem(name: String, value: String): LabeledValueItem {
        val item = LabeledValueItem(requireContext())
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, 0)
        item.layoutParams = layoutParams
        item.label = name
        item.value = value
        return item
    }

    private fun getDivider(): View {
        val item = View(requireContext())
        val height = resources.getDimension(R.dimen.item_separator_height).toInt()
        val layoutParams = LayoutParams(MATCH_PARENT, height)
        layoutParams.setMargins(0, dpToPx(16), 0, 0)
        item.layoutParams = layoutParams
        item.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.separator))
        return item
    }
}
