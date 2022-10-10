package io.gnosis.safe.ui.safe.send_funds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSuccessBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.TransactionsFragmentDirections
import io.gnosis.safe.ui.transactions.TxPagerAdapter

class SuccessFragment : BaseViewBindingFragment<FragmentSuccessBinding>() {

    private val navArgs by navArgs<SuccessFragmentArgs>()
    private val chain by lazy { navArgs.chain }
    private val txId by lazy { navArgs.txId }

    override fun screenId() = ScreenId.ASSETS_COINS_TRANSFER_SUCCESS

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSuccessBinding =
        FragmentSuccessBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // disable default back navigation
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {}
            })
        with(binding) {
            //FIXME: success animation last frame should display final image (frame 88 at the moment)
            lottieSuccess.setMaxFrame(88)
            viewDetailsButton.setOnClickListener {
                findNavController().popBackStack(R.id.assetsFragment, false)
                with(findNavController()) {
                    navigate(R.id.transactionsFragment, Bundle().apply {
                        putInt(
                            "activeTab",
                            TxPagerAdapter.Tabs.QUEUE.ordinal
                        )
                    })
                    navigate(
                        TransactionsFragmentDirections.actionTransactionsFragmentToTransactionDetailsFragment(
                            chain,
                            txId
                        )
                    )
                }
            }
            doneButton.setOnClickListener {
                findNavController().popBackStack(R.id.assetsFragment, false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.lottieSuccess.playAnimation()
    }
}
