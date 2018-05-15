package pm.gnosis.heimdall.ui.safe.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_no_safes.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.safe.add.AddSafeActivity
import timber.log.Timber


class NoSafesFragment: BaseFragment() {
    override fun inject(component: ApplicationComponent) { }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_no_safes, container, false)

    override fun onStart() {
        super.onStart()
        disposables += layout_no_safes_add_safe_button.clicks().subscribeBy(onNext = {
            startActivity(AddSafeActivity.createIntent(context!!))
        }, onError = Timber::e)
    }

}
