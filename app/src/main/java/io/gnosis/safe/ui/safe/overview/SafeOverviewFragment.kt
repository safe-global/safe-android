package io.gnosis.safe.ui.safe.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
//import io.gnosis.safe.databinding.FragmentSafeOverviewBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import javax.inject.Inject

//class SafeOverviewFragment : BaseFragment<FragmentSafeOverviewBinding>() {
//    @Inject
//    lateinit var viewModel: SafeOverviewViewModel
//
//    override fun inject(component: ViewComponent) {
//        component.inject(this)
//    }
//
//    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSafeOverviewBinding =
//        FragmentSafeOverviewBinding.inflate(inflater, container, false)
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        viewModel.termsBottomSheetDialog = TermsBottomSheetDialog(this.requireContext())
//        binding.addSafeButton.setOnClickListener {
//            viewModel.checkTerms {
//                findNavController().navigate(SafeOverviewFragmentDirections.actionSafeOverviewFragmentToAddSafeNav())
//            }
//        }
//    }
//}
