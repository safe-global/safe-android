package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.models.transaction.ValueDecoded
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsActionMultisendBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.MultisendActionView
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.ParamSerializer
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class TransactionDetailsActionMultisendFragment : BaseViewBindingFragment<FragmentTransactionDetailsActionMultisendBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_DETAILS_ACTION_LIST

    private val navArgs by navArgs<TransactionDetailsActionMultisendFragmentArgs>()
    private val decodedValues by lazy { paramSerializer.deserializeDecodedValues(navArgs.decodedValues) }

    @Inject
    lateinit var balanceFormatter: BalanceFormatter

    @Inject
    lateinit var paramSerializer: ParamSerializer

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionDetailsActionMultisendBinding =
        FragmentTransactionDetailsActionMultisendBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }
        updateUi(decodedValues)
    }

    private fun updateUi(decodedValues: List<ValueDecoded>?) {

        with(binding) {

            content.removeAllViews()

            decodedValues?.let {
                it.forEachIndexed { index, value ->
                    val action = value.dataDecoded?.method ?: getString(R.string.tx_multisend_action, index + 1)
                    val item = getMultisendActionItem(value.to, action)
                    item.setOnClickListener {
                        findNavController().navigate(
                            TransactionDetailsActionMultisendFragmentDirections.actionTransactionDetailsActionMultisendFragmentToTransactionDetailsActionFragment(
                                action,
                                value.data,
                                value.dataDecoded?.let { paramSerializer.serializeDecodedData(it) },
                                value.to.asEthereumAddressString(),
                                balanceFormatter.formatAmount(value.value, false)
                            )
                        )
                    }
                    content.addView(item)
                }
            }
        }
    }

    private fun getMultisendActionItem(address: Solidity.Address, method: String): MultisendActionView {
        val item = MultisendActionView(requireContext())
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, 0)
        item.layoutParams = layoutParams
        item.setData(address, method)
        return item
    }
}
