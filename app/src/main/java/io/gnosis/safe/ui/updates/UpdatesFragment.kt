package io.gnosis.safe.ui.updates

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentUpdatesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class UpdatesFragment : BaseViewBindingFragment<FragmentUpdatesBinding>() {

    enum class Mode {
        DEPRECATED,
        UPDATE_DEPRECATED_SOON,
        UPDATE_NEW_VERSION
    }

    private val navArgs by navArgs<UpdatesFragmentArgs>()
    private val mode by lazy { Mode.valueOf(navArgs.mode) }

    @Inject
    lateinit var viewModel: UpdatesViewModel

    override fun screenId() = when (mode) {
        Mode.DEPRECATED -> ScreenId.UPDATE_DEPRECATED
        Mode.UPDATE_DEPRECATED_SOON -> ScreenId.UPDATE_DEPRECATED_SOON
        Mode.UPDATE_NEW_VERSION -> ScreenId.UPDATE_NEW_VERSION
    }

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUpdatesBinding =
        FragmentUpdatesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mode != Mode.DEPRECATED) {
                    findNavController().navigateUp()
                }
            }
        })

        val appUpdateManager = AppUpdateManagerFactory.create(requireContext())

        with(binding) {

            updateButton.setOnClickListener {
                // Returns an intent object that you use to check for an update.
                val appUpdateInfoTask = appUpdateManager.appUpdateInfo
                appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                    // Request the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        requireActivity(),
                        UPDATE_REQUEST
                    )
                }
            }

            skip.setOnClickListener {
                findNavController().navigateUp()
            }
            skip.visible(mode != Mode.DEPRECATED)

            updateDescription.setText(
                when (mode) {
                    Mode.DEPRECATED -> R.string.update_desc_deprecated
                    Mode.UPDATE_DEPRECATED_SOON -> R.string.update_desc_deprecated_soon
                    Mode.UPDATE_NEW_VERSION -> R.string.update_desc_new_version
                }
            )
        }

        viewModel.setUpdateShownForVersionFlag()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST) {
            if (resultCode != RESULT_OK) {
                Timber.d("Update flow failed! Result code: $resultCode")
            } else {
                viewModel.updateUpdateInfo()
                viewModel.resetAppStartCount()
                findNavController().navigateUp()
            }
        }
    }

    companion object {
        const val UPDATE_REQUEST = 1000
    }
}
