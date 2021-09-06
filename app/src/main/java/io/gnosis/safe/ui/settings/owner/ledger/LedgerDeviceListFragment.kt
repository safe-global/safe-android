package io.gnosis.safe.ui.settings.owner.ledger

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentLedgerDeviceListBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.updates.UpdatesFragment
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class LedgerDeviceListFragment : BaseViewBindingFragment<FragmentLedgerDeviceListBinding>(), LedgerDeviceListAdapter.DeviceListener {

    override fun screenId() = ScreenId.LEDGER_DEVICE_LIST

    override suspend fun chainId(): BigInteger? = null

    lateinit var adapter: LedgerDeviceListAdapter

    @Inject
    lateinit var viewModel: LedgerDeviceListViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLedgerDeviceListBinding =
        FragmentLedgerDeviceListBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LedgerDeviceListAdapter(this)

        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
            refresh.setOnRefreshListener {
                viewModel.searchForDevices()
            }
            val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            dividerItemDecoration.setDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.divider)!!)
            devices.addItemDecoration(dividerItemDecoration)
            devices.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            devices.adapter = adapter
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            state.viewAction?.let { action ->
                when (action) {
                    is Loading -> {
                        binding.progress.visible(true)
                    }
                    is ShowError -> {

                    }
                }
            }
        })

        if (viewModel.setupConnection(this)) {
            viewModel.searchForDevices()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LedgerController.REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {

            } else {
                viewModel.searchForDevices()
            }
        }
    }

    override fun onDeviceClick() {
        TODO("Not yet implemented")
    }
}




