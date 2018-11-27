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
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.safe.create.CreateSafeIntroActivity
import pm.gnosis.heimdall.ui.safe.recover.safe.CheckSafeActivity
import pm.gnosis.heimdall.ui.safe.recover.safe.RecoverSafeIntroActivity
import timber.log.Timber
import javax.inject.Inject


class NoSafesFragment : BaseFragment() {
    @Inject
    lateinit var eventTracker: EventTracker

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_no_safes, container, false)

    override fun onStart() {
        super.onStart()
        eventTracker.submit(Event.ScreenView(ScreenId.NO_SAFES))

        disposables += layout_no_safes_add_safe_button.clicks()
            .subscribeBy(onNext = { startActivity(CreateSafeIntroActivity.createIntent(context!!)) }, onError = Timber::e)

        disposables += layout_no_safes_recover_safe.clicks()
            .subscribeBy(onNext = { startActivity(RecoverSafeIntroActivity.createIntent(context!!)) }, onError = Timber::e)
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
            .applicationComponent(component)
            .viewModule(ViewModule(context!!))
            .build().inject(this)
    }
}
