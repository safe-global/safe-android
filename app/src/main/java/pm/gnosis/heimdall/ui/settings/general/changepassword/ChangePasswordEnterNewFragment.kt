package pm.gnosis.heimdall.ui.settings.general.changepassword

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_password_change_enter_new.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.helpers.PasswordHelper
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.security.EncryptionManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class ChangePasswordEnterNewFragment : BaseFragment() {
    @Inject
    lateinit var encryptionManager: EncryptionManager

    @Inject
    lateinit var viewModel: ChangePasswordContract


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var root = inflater.inflate(R.layout.layout_password_change_enter_new, container, false)
        return root
    }

    @SuppressLint("RestrictedApi")
    override fun onStart() {
        super.onStart()

        layout_password_change_enter_new.showKeyboardForView()

        layout_password_change_validation_info.text = PasswordHelper.Validator.getValidationInfo(context!!)

        disposables += viewModel.confirmEvents()
            .switchMapSingle {
                viewModel.changePassword(
                    layout_password_change_enter_new.text.toString(),
                    layout_password_change_enter_new_repeat.text.toString()
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult {
                Timber.e(it)
                snackbar(layout_password_change_enter_new, R.string.unknown_error)
            }

        disposables += layout_password_change_enter_new.textChanges()
            .skipInitialValue()
            .debounce(500, TimeUnit.MILLISECONDS)
            .switchMapSingle {
                viewModel.validatePassword(it.toString(), layout_password_change_enter_new_repeat.text.toString())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {
                PasswordHelper.Handler.applyToView(
                    layout_password_change_enter_new,
                    layout_password_change_validation_info,
                    it
                )
                PasswordHelper.Handler.resetView(
                    layout_password_change_enter_new_repeat,
                    layout_password_change_enter_new_repeat_info
                )
            }, onError = Timber::e)

        disposables += layout_password_change_enter_new_repeat.textChanges()
            .debounce(500, TimeUnit.MILLISECONDS)
            .switchMapSingle {
                viewModel.validateRepeat(layout_password_change_enter_new.text.toString(), it.toString())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {
                PasswordHelper.Handler.applyToView(
                    layout_password_change_enter_new_repeat,
                    layout_password_change_enter_new_repeat_info,
                    if (it) null else getString(R.string.password_doesnt_match),
                    it
                )
            }, onError = Timber::e)
    }

    override fun onResume() {
        super.onResume()
        layout_password_change_enter_new.text = null
        layout_password_change_enter_new_repeat.text = null
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(component)
            .build()
            .inject(this)
    }

    companion object {
        fun newInstance() = ChangePasswordEnterNewFragment()
    }
}
