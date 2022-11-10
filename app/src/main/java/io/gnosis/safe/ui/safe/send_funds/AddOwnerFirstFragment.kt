package io.gnosis.safe.ui.safe.send_funds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentAddOwnerFirstBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import java.math.BigInteger

class AddOwnerFirstFragment : BaseViewBindingFragment<FragmentAddOwnerFirstBinding>() {

    override fun screenId() = ScreenId.ASSETS_COINS_TRANSFER_ADD_OWNER

    override suspend fun chainId(): BigInteger? = null

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddOwnerFirstBinding =
        FragmentAddOwnerFirstBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
            addOwnerButton.setOnClickListener {
                tracker.logTransferAddOwnerClicked()
                findNavController().navigateUp()
                findNavController().navigate(R.id.action_to_add_owner)
            }
        }
    }
}
