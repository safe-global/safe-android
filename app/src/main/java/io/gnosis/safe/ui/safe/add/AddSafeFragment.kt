package io.gnosis.safe.ui.safe.add

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.databinding.FragmentAddSafeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.android.synthetic.main.fragment_add_safe.*
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class AddSafeFragment : BaseFragment<FragmentAddSafeBinding>() {

    @Inject
    lateinit var viewModel: AddSafeViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddSafeBinding =
        FragmentAddSafeBinding.inflate(inflater, container, false)

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            nextButton.setOnClickListener {
                addSafeAddressInputLayout.error = null
                addSafeAddressInputLayout.isErrorEnabled = false
                viewModel.submitAddress(addSafeAddressInputEntry.text.toString())
            }
            backButton.setOnClickListener { findNavController().navigateUp() }
            addSafeAddressInputEntry.setOnTouchListener { _, event ->
                val DRAWABLE_LEFT = 0
                val DRAWABLE_TOP = 1
                val DRAWABLE_RIGHT = 2;
                val DRAWABLE_BOTTOM = 3;

                if (event.action == MotionEvent.ACTION_UP &&
                    event.rawX >= (addSafeAddressInputEntry.right - addSafeAddressInputEntry.compoundDrawables[DRAWABLE_RIGHT].bounds.width())
                ) {
                    Toast.makeText(context, "CLICK", Toast.LENGTH_SHORT).show()
                    true
                }
                false
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is CaptureSafe -> {
                    state.viewAction?.let { action ->
                        when (action) {
                            is BaseStateViewModel.ViewAction.NavigateTo -> findNavController().navigate((action.navDirections))
                            is BaseStateViewModel.ViewAction.Loading -> binding.progress.visible(action.isLoading)
                            is BaseStateViewModel.ViewAction.ShowError -> {
                                progress.visible(false)
                                binding.addSafeAddressInputLayout.error = "Invalid address"
                                Timber.e(action.error)
                            }
                        }
                    }
                }
            }
        })
    }
}
