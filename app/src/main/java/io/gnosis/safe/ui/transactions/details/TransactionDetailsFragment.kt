package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import javax.inject.Inject

class TransactionDetailsFragment : BaseViewBindingFragment<FragmentTransactionDetailsBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_DETAILS

    @Inject
    lateinit var viewModel: TransactionDetailsViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionDetailsBinding =
        FragmentTransactionDetailsBinding.inflate(inflater, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {

            backButton.setOnClickListener {
                Navigation.findNavController(root).navigateUp()
            }

            refresh.setOnRefreshListener {
                //TODO: add on refresh listener
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->

        })
    }
}
