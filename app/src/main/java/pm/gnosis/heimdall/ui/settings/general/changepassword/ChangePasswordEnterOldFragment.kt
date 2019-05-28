package pm.gnosis.heimdall.ui.settings.general.changepassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_password_change_enter_old.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.settings.general.changepassword.ChangePasswordContract.State.INVALID_PASSWORD
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.security.EncryptionManager
import timber.log.Timber
import javax.inject.Inject


class ChangePasswordEnterOldFragment : BaseFragment() {

    @Inject
    lateinit var encryptionManager: EncryptionManager

    @Inject
    lateinit var viewModel: ChangePasswordContract


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_password_change_enter_old, container, false)
    }

    override fun onResume() {
        super.onResume()
        layout_password_change_enter_old_current.text = null
    }

    override fun onStart() {
        super.onStart()

        layout_password_change_enter_old_current.requestFocus()

        disposables += viewModel.confirmEvents()
            .switchMapSingle {
                viewModel.confirmPassword(
                    layout_password_change_enter_old_current.text.toString())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(::applyState) {
                Timber.e(it)
                snackbar(layout_password_change_enter_old_current, R.string.unknown_error)
            }

        disposables += layout_password_change_enter_old_current.textChanges()
            .subscribeBy { layout_password_change_enter_old_current_info.text = null }

    }

    private fun applyState(viewState: ChangePasswordContract.ViewState) {
        when (viewState.state) {
            INVALID_PASSWORD -> {
                layout_password_change_enter_old_current_info.text = getString(R.string.error_wrong_credentials)
            }
        }
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(component)
            .build()
            .inject(this)
    }

    companion object {
        fun newInstance() = ChangePasswordEnterOldFragment()
    }
}
