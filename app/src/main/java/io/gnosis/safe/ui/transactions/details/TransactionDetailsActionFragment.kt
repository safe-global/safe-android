package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.LayoutParams
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.ParamDto
import io.gnosis.data.backend.dto.ParamType
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsActionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.LabeledAddressItem
import io.gnosis.safe.ui.transactions.details.view.LabeledArrayItem
import io.gnosis.safe.ui.transactions.details.view.LabeledValueItem
import io.gnosis.safe.ui.transactions.details.view.TxDataView
import io.gnosis.safe.utils.ParamSerializer
import io.gnosis.safe.utils.dpToPx
import pm.gnosis.model.Solidity
import pm.gnosis.utils.removeHexPrefix
import javax.inject.Inject

class TransactionDetailsActionFragment : BaseViewBindingFragment<FragmentTransactionDetailsActionBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_DETAILS_ACTION

    private val navArgs by navArgs<TransactionDetailsActionFragmentArgs>()
    private val decodedData by lazy { paramSerializer.unserializeDecodedData(navArgs.decodedData) }

    @Inject
    lateinit var paramSerializer: ParamSerializer

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionDetailsActionBinding =
        FragmentTransactionDetailsActionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }
        updateUi(decodedData)
    }

    private fun updateUi(decodedDto: DataDecodedDto?, address: Solidity.Address? = null, amount: String? = null) {

        binding.content.removeAllViews()

        decodedDto?.let {
            with(binding) {
                title.text = it.method
                it.parameters?.forEach {
                    when (it) {
                        is ParamDto.AddressParam -> {
                            content.addView(getLabeledAddressItem("${it.name}(${it.type}):", it.value))
                        }
                        is ParamDto.ArrayParam -> {
                            content.addView(getArrayItem("${it.name}(${it.type}):", it.value, it.getItemType()))
                        }
                        is ParamDto.BytesParam -> {
                            content.addView(getDataItem("${it.name}(${it.type}):", it.value))
                        }
                        is ParamDto.ValueParam -> {
                            content.addView(getLabeledValueItem("${it.name}(${it.type}):", it.value.toString()))
                        }
                    }
                }
            }
        }
    }

    private fun getArrayItem(name: String, value: List<Any>, paramType: ParamType): LabeledArrayItem {
        val item = LabeledArrayItem(requireContext())
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, 0)
        item.layoutParams = layoutParams
        item.label = name
        item.showArray(value, paramType)
        return item
    }

    private fun getDataItem(name: String, value: String): TxDataView {
        val item = TxDataView(requireContext())
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.setMargins(dpToPx(16), dpToPx(16), dpToPx(16), 0)
        item.layoutParams = layoutParams
        item.setData(value, value.removeHexPrefix().length / 2, name)
        return item
    }

    private fun getLabeledAddressItem(name: String, value: Solidity.Address): LabeledAddressItem {
        val item = LabeledAddressItem(requireContext())
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, 0)
        item.layoutParams = layoutParams
        item.label = name
        item.address = value
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
}
