package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.transaction.DataDecoded
import io.gnosis.data.models.transaction.Param
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsActionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.*
import io.gnosis.safe.utils.ParamSerializer
import io.gnosis.safe.utils.toColor
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
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
                content.addView(requireContext().getTransferItem(chain, it, amount ?: "", addressInfoIndex?.get(it.asEthereumAddressChecksumString())))
                content.addView(requireContext().getDivider())
            }
        }
        data?.let {
            binding.content.addView(requireContext().getDataItem(getString(R.string.tx_details_data), it))
        }

        decodedDto?.let {
            with(binding) {

                if (it.parameters?.size ?: 0 > 0) {
                    content.addView(requireContext().getDivider())
                }

                it.parameters?.forEach {
                    when (it) {
                        is Param.Address -> {
                            content.addView(requireContext().getLabeledAddressItem(chain, "${it.name}(${it.type}):", it.value, addressInfoIndex?.get(it.value.asEthereumAddressChecksumString())))
                        }
                        is Param.Array -> {
                            content.addView(requireContext().getArrayItem(chain, "${it.name}(${it.type}):", it.value, it.getItemType(), it.type, addressInfoIndex))
                        }
                        is Param.Bytes -> {
                            content.addView(requireContext().getDataItem("${it.name}(${it.type}):", it.value))
                        }
                        is Param.Value -> {
                            if (it.isBytesValue()) {
                                content.addView(requireContext().getDataItem("${it.name}(${it.type}):", it.value as String))
                            } else {
                                content.addView(requireContext().getLabeledValueItem("${it.name}(${it.type}):", it.value.toString()))
                            }
                        }
                    }
                }
            }
        }
    }
}
