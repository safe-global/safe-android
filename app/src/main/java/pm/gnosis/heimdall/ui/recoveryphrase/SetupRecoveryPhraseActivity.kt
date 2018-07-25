package pm.gnosis.heimdall.ui.recoveryphrase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.util.DisplayMetrics
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_setup_recovery_phrase.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import timber.log.Timber
import javax.inject.Inject

abstract class SetupRecoveryPhraseActivity<VM : SetupRecoveryPhraseContract> : ViewModelActivity<VM>() {
    override fun screenId(): ScreenId = ScreenId.CONFIRM_RECOVERY_PHRASE
    
    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    @Inject
    lateinit var adapter: SetupRecoveryPhraseAdapter

    private var recoveryPhraseDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_setup_recovery_phrase)

        layout_setup_recovery_phrase_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
            adapter = this@SetupRecoveryPhraseActivity.adapter
            addItemDecoration(RecoveryPhraseItemDecoration(this@SetupRecoveryPhraseActivity, R.dimen.horizontal_offset, R.dimen.vertical_offset))
        }

        recoveryPhraseDisposable = viewModel.generateRecoveryPhrase()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { words ->
                adapter.setWords(words)
            }, onError = {
                Timber.e(it)
                finish()
            })
    }

    override fun onStart() {
        super.onStart()
        disposables += Single.fromCallable {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels
        }
            .flatMap { viewModel.loadScaledBackgroundResource(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { layout_setup_recovery_phrase_waves.setImageBitmap(it) }, onError = Timber::e)

        disposables += toolbarHelper.setupShadow(layout_setup_recovery_phrase_toolbar_shadow, layout_setup_recovery_phrase_scroll_view)

        disposables += layout_setup_recovery_phrase_next.clicks()
            .subscribeBy(
                onNext = { _ -> viewModel.getRecoveryPhrase()?.let { onConfirmedRecoveryPhrase(it) } },
                onError = Timber::e
            )

        disposables += layout_setup_recovery_phrase_back_button.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)
    }

    abstract fun onConfirmedRecoveryPhrase(recoveryPhrase: String)

    override fun layout() = R.layout.layout_setup_recovery_phrase

    override fun onDestroy() {
        super.onDestroy()
        recoveryPhraseDisposable?.dispose()
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, SetupRecoveryPhraseActivity::class.java)
    }
}
