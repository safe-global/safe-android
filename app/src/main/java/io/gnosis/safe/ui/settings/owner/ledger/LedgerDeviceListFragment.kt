package io.gnosis.safe.ui.settings.owner.ledger

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentLedgerDeviceListBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pm.gnosis.svalinn.common.utils.visible
import java.math.BigInteger
import javax.inject.Inject

class LedgerDeviceListFragment : BaseViewBindingFragment<FragmentLedgerDeviceListBinding>(), LedgerDeviceListAdapter.DeviceListener {

    enum class Mode {
        ADDRESS_SELECTION,
        CONFIRMATION,
        REJECTION
    }

    private val navArgs by navArgs<LedgerDeviceListFragmentArgs>()
    private val mode by lazy { Mode.valueOf(navArgs.mode) }
    private val owner by lazy { navArgs.owner }
    private val safeTxHash by lazy { navArgs.safeTxHash }

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

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackNavigation()
            }
        })

        adapter = LedgerDeviceListAdapter(this)

        with(binding) {
            backButton.setOnClickListener {
                onBackNavigation()
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
                        if (action.isLoading) {
                            binding.emptyPlaceholder.visible(false)
                            showLoading()
                        } else {
                            hideLoading()
                            if (adapter.itemCount == 0) {
                                binding.action.visible(false)
                                binding.emptyPlaceholder.visible(true)
                            }
                        }
                    }
                    is DeviceFound -> {
                        hideLoading()
                        binding.emptyPlaceholder.visible(false)
                        binding.action.text = getString(R.string.ledger_select_device)
                        adapter.updateDeviceData(action.results)
                    }
                    is DeviceConnected -> {

                        when (mode) {
                            Mode.ADDRESS_SELECTION ->
                                findNavController().navigate(LedgerDeviceListFragmentDirections.actionLedgerDeviceListFragmentToLedgerTabsFragment())
                            Mode.CONFIRMATION ->
                                findNavController().navigate(
                                    LedgerDeviceListFragmentDirections.actionLedgerDeviceListFragmentToLedgerSignDialog(
                                        owner!!,
                                        safeTxHash!!
                                    )
                                )
                            Mode.REJECTION ->
                                findNavController().navigate(
                                    LedgerDeviceListFragmentDirections.actionLedgerDeviceListFragmentToLedgerSignDialog(
                                        owner!!,
                                        safeTxHash!!,
                                        false
                                    )
                                )
                        }
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

    private fun onBackNavigation() {
        viewModel.disconnectFromDevice()
        findNavController().navigateUp()
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
        lifecycleScope.launch {
            viewModel.connectToDevice(requireContext(), position)
            // prevent multiple connect attempts due to multiple clicks
            delay(5000)
        }
    }
}
