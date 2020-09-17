package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.LayoutParams
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.ParamDto
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsActionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.LabeledAddressItem
import io.gnosis.safe.ui.transactions.details.view.LabeledValueItem
import io.gnosis.safe.ui.transactions.details.view.TxDataView
import pm.gnosis.model.Solidity
import javax.inject.Inject

class TransactionDetailsActionFragment : BaseViewBindingFragment<FragmentTransactionDetailsActionBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_DETAILS_ACTION

    private val navArgs by navArgs<TransactionDetailsActionFragmentArgs>()
    private val decodedDataString by lazy { navArgs.decodedData }

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
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }
        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (it.viewAction) {
                is Loading -> {
                    updateUi(it.dataDecoded)
                }
            }
        })
        viewModel.getDecodedData(decodedDataString)
    }

    private fun updateUi(decodedDto: DataDecodedDto?) {
        binding.content.removeAllViews()
        decodedDto?.let {
            with(binding) {
                title.text = it.method
                it.parameters?.forEach {
                    when (it) {
                        is ParamDto.AddressParam -> {
                            content.addView(getLabeledAddressItem("${it.name}(${it.type}):", it.value))
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

    private fun getDataItem(name: String, value: String): TxDataView {
        val item = TxDataView(requireContext())
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, 0)
        item.layoutParams = layoutParams
        item.setData(value, value.length / 2, name)
        return item
    }

    private fun getLabeledAddressItem(name: String, value: Solidity.Address): LabeledAddressItem {
        val item = LabeledAddressItem(requireContext())
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, 0)
        item.layoutParams = layoutParams
        item.name = name
        item.address = value
        return item
    }

    private fun getLabeledValueItem(name: String, value: String): LabeledValueItem {
        val item = LabeledValueItem(requireContext())
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, 0)
        item.layoutParams = layoutParams
        item.name = name
        item.value = value
        return item
    }
}
