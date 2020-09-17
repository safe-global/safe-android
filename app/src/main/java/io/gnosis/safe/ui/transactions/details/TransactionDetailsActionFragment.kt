package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsActionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
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
        decodedDto?.let {
            with(binding) {
                title.text = it.method
                it.parameters?.forEach {

                }
            }
        }
    }
}
