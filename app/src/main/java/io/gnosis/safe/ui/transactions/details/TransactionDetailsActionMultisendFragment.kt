package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Chain
import io.gnosis.data.models.transaction.ValueDecoded
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsActionMultisendBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.MultisendActionView
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.ParamSerializer
import io.gnosis.safe.utils.toColor
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class TransactionDetailsActionMultisendFragment : BaseViewBindingFragment<FragmentTransactionDetailsActionMultisendBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_DETAILS_ACTION_LIST

    private val navArgs by navArgs<TransactionDetailsActionMultisendFragmentArgs>()
    private val chain by lazy { navArgs.chain }
    private val decodedValues by lazy { paramSerializer.deserializeDecodedValues(navArgs.decodedValues) }
    private val addressInfoIndex by lazy { paramSerializer.deserializeAddressInfoIndex(navArgs.addressInfoIndex) }

    @Inject
    lateinit var balanceFormatter: BalanceFormatter

    @Inject
    lateinit var paramSerializer: ParamSerializer

    @Inject
    lateinit var viewModel: TransactionDetailsActionViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionDetailsActionMultisendBinding =
        FragmentTransactionDetailsActionMultisendBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {

            chainRibbon.text = chain.name
            chainRibbon.setTextColor(chain.textColor.toColor(requireContext(), R.color.white))
            chainRibbon.setBackgroundColor(chain.backgroundColor.toColor(requireContext(), R.color.primary))

            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer {
            when(val viewAction = it.viewAction) {
                is BaseStateViewModel.ViewAction.Loading -> {
                    if (!viewAction.isLoading) {
                        updateUi(decodedValues, it.addressInfoIndex)
                    }
                }
            }
        })
        viewModel.extendAddressInfoIndexWithLocalData(addressInfoIndex)
    }

    private fun updateUi(decodedValues: List<ValueDecoded>?, addressInfoIndex: Map<String, AddressInfo>? = null) {

        with(binding) {

            content.removeAllViews()

            decodedValues?.let {
                it.forEachIndexed { index, value ->
                    val action = value.dataDecoded?.method ?: getString(R.string.tx_multisend_action, index + 1)
                    val item = getMultisendActionItem(
                        chain = chain,
                        address = value.to,
                        showChainPrefix = viewModel.isChainPrefixCopyEnabled(),
                        method = action,
                        addressInfoIndex = addressInfoIndex
                    )
                    item.setOnClickListener {
                        findNavController().navigate(
                            TransactionDetailsActionMultisendFragmentDirections.actionTransactionDetailsActionMultisendFragmentToTransactionDetailsActionFragment(
                                chain,
                                action,
                                value.data,
                                value.dataDecoded?.let { paramSerializer.serializeDecodedData(it) },
                                value.to.asEthereumAddressString(),
                                balanceFormatter.formatAmount(value.value, false, chain.currency.decimals, chain.currency.symbol),
                                paramSerializer.serializeAddressInfoIndex(addressInfoIndex)
                            )
                        )
                    }
                    content.addView(item)
                }
            }
        }
    }

    private fun getMultisendActionItem(
        chain: Chain,
        address: Solidity.Address,
        showChainPrefix: Boolean,
        method: String,
        addressInfoIndex: Map<String, AddressInfo>? = null
    ): MultisendActionView {
        val item = MultisendActionView(requireContext())
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, 0)
        item.layoutParams = layoutParams

        val addressInfo = addressInfoIndex?.get(address.asEthereumAddressChecksumString())
        if (addressInfo != null) {
            item.setData(chain, address, showChainPrefix, method, addressInfo.name)
            item.loadKnownAddressLogo(addressInfo.logoUri, address)
        } else {
            item.setData(chain, address, showChainPrefix, method)
        }

        return item
    }
}
