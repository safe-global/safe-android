package io.gnosis.safe.ui.settings.owner.ledger

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentLedgerDeviceListBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.visible
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
                viewModel.scanForDevices(this@LedgerDeviceListFragment, ::requestMissingLocationPermission)
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
                        binding.emptyPlaceholder.visible(false)
                        showLoading()
                    }
                    is DeviceFound -> {
                        hideLoading()
                        binding.emptyPlaceholder.visible(false)
                        binding.action.text = getString(R.string.ledger_select_device)
                        adapter.updateDeviceData(action.results)
                    }
                    is DeviceConnected -> {
                        //TODO: navigate to address selection; pass action.device in EXTRA_DEVICE
                    }
                    is ShowError -> {
                        hideLoading()
                        if (adapter.itemCount == 0) {
                            binding.action.visible(false)
                            binding.emptyPlaceholder.visible(true)
                        }
                    }
                }
            }
        })

        viewModel.scanForDevices(this, ::requestMissingLocationPermission)
    }

    override fun onResume() {
        super.onResume()

        // ConnectionManager.registerListener(connectionEventListener)
    }

    private fun showLoading() {
        if (adapter.itemCount == 0) {
            binding.action.text = getString(R.string.ledger_device_search)
            binding.action.visible(true)
            binding.progress.visible(true)
            binding.refresh.isRefreshing = false
        }
    }

    private fun hideLoading() {
        binding.progress.visible(false)
        binding.refresh.isRefreshing = false
    }

    private fun showPlaceholder() {
        if (adapter.itemCount == 0) {
            binding.action.visible(false)
            binding.emptyPlaceholder.visible(true)
        }
    }

    private fun requestMissingLocationPermission() {
        viewModel.requestLocationPermission(this)
    }

    private fun handleMissingLocationPermission() {
        viewModel.scanError()
    }

    private fun handleBluetoothDisabled() {
        viewModel.scanError()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.handleResult(this, ::handleBluetoothDisabled, ::requestMissingLocationPermission, requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        viewModel.handlePermissionResult(
            this,
            ::handleMissingLocationPermission,
            ::requestMissingLocationPermission,
            requestCode,
            permissions,
            grantResults
        )
    }

    override fun onDeviceClick(position: Int) {
        viewModel.connectToDevice(requireContext(), position)
    }
}
