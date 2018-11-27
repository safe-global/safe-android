package pm.gnosis.heimdall.ui.recoveryphrase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import android.widget.TextView
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_confirm_recovery_phrase.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.setCompoundDrawableResource
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.utils.words
import timber.log.Timber
import javax.inject.Inject

abstract class ConfirmRecoveryPhraseActivity<VM : ConfirmRecoveryPhraseContract> : ViewModelActivity<VM>() {
    override fun screenId() = ScreenId.CONFIRM_RECOVERY_PHRASE

    private var randomWordsDisposable: Disposable? = null

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    @Inject
    lateinit var adapter: ConfirmRecoveryPhraseAdapter

    private var scrollRunnable: Runnable? = null

    override fun layout() = R.layout.layout_confirm_recovery_phrase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setup(intent.getStringExtra(EXTRA_RECOVERY_PHRASE))

        layout_confirm_recovery_phrase_submit.setCompoundDrawableResource(right = R.drawable.ic_arrow_forward_24dp)

        layout_confirm_recovery_phrase_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
            adapter = this@ConfirmRecoveryPhraseActivity.adapter
            addItemDecoration(RecoveryPhraseItemDecoration(this@ConfirmRecoveryPhraseActivity, R.dimen.horizontal_offset, R.dimen.vertical_offset))
        }

        randomWordsDisposable = viewModel.loadRandomPositions()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = ::onRandomPositions, onError = Timber::e)
    }

    private fun onRandomPositions(randomPositions: List<Int>) {
        if (randomPositions.size != ConfirmRecoveryPhraseContract.SELECTABLE_WORDS) {
            finish()
            return
        }

        val recoveryPhraseWords = viewModel.getRecoveryPhrase().words()

        layout_confirm_recovery_phrase_word_1.text = recoveryPhraseWords[randomPositions[0]]
        layout_confirm_recovery_phrase_word_1.isClickable = true
        layout_confirm_recovery_phrase_word_2.text = recoveryPhraseWords[randomPositions[1]]
        layout_confirm_recovery_phrase_word_2.isClickable = true
        layout_confirm_recovery_phrase_word_3.text = recoveryPhraseWords[randomPositions[2]]
        layout_confirm_recovery_phrase_word_3.isClickable = true
        layout_confirm_recovery_phrase_word_4.text = recoveryPhraseWords[randomPositions[3]]
        layout_confirm_recovery_phrase_word_4.isClickable = true

        adapter.setWords(recoveryPhraseWords, randomPositions)
    }

    override fun onStart() {
        super.onStart()
        disposables.addAll(
            subscribeWordSelection(layout_confirm_recovery_phrase_word_1),
            subscribeWordSelection(layout_confirm_recovery_phrase_word_2),
            subscribeWordSelection(layout_confirm_recovery_phrase_word_3),
            subscribeWordSelection(layout_confirm_recovery_phrase_word_4)
        )

        disposables += adapter.observeSelectedCount()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = { selectedCount ->
                bottomBarEnabled(selectedCount == ConfirmRecoveryPhraseContract.SELECTABLE_WORDS)
            }, onError = Timber::e)

        disposables += layout_confirm_recovery_phrase_submit.clicks()
            .switchMapSingle { viewModel.getIncorrectPositions(adapter.getWords()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onIncorrectPositions, onError = Timber::e)

        disposables += layout_confirm_recovery_phrase_back_button.clicks()
            .subscribeBy(onNext = { finish() }, onError = Timber::e)

        disposables += toolbarHelper.setupShadow(layout_confirm_recovery_phrase_toolbar_shadow, layout_confirm_recovery_phrase_scroll_view)
    }

    private fun onIncorrectPositions(incorrectPositions: Set<Int>) {
        if (incorrectPositions.isEmpty()) {
            isRecoveryPhraseConfirmed()
        } else {
            adapter.setIncorrectPositions(incorrectPositions)
            snackbar(layout_confirm_recovery_phrase_coordinator, R.string.confirm_recovery_phrase_incorrect_sequence)
        }
    }

    abstract fun isRecoveryPhraseConfirmed()

    private fun subscribeWordSelection(wordView: TextView) =
        wordView.clicks().subscribeBy(
            onNext = {
                scrollToWord(adapter.pushWord(wordView.text.toString()))
                setWord(isEnabled = false, wordView = wordView)
            },
            onError = Timber::e
        )

    override fun onBackPressed() {
        if (adapter.getSelectedCount() == 0) super.onBackPressed()
        else {
            val (poppedWord, nextActiveIndex) = adapter.popWord()
            when (poppedWord) {
                layout_confirm_recovery_phrase_word_1.text -> setWord(isEnabled = true, wordView = layout_confirm_recovery_phrase_word_1)
                layout_confirm_recovery_phrase_word_2.text -> setWord(isEnabled = true, wordView = layout_confirm_recovery_phrase_word_2)
                layout_confirm_recovery_phrase_word_3.text -> setWord(isEnabled = true, wordView = layout_confirm_recovery_phrase_word_3)
                layout_confirm_recovery_phrase_word_4.text -> setWord(isEnabled = true, wordView = layout_confirm_recovery_phrase_word_4)
            }
            scrollToWord(nextActiveIndex)
        }
    }

    private fun scrollToWord(wordPosition: Int) {
        scrollRunnable?.let { layout_confirm_recovery_phrase_scroll_view.removeCallbacks(it) }
        layout_confirm_recovery_phrase_recycler_view.layoutManager.findViewByPosition(wordPosition)?.let {
            scrollRunnable = Runnable { layout_confirm_recovery_phrase_scroll_view.requestChildFocus(it, it) }
            layout_confirm_recovery_phrase_scroll_view.postDelayed(scrollRunnable, SCROLL_DELAY_MS)
        } ?: run {
            // View not yet present, scroll recycler view
            layout_confirm_recovery_phrase_recycler_view.scrollToPosition(wordPosition)
        }
    }

    private fun setWord(isEnabled: Boolean, wordView: TextView) {
        wordView.isClickable = isEnabled
        wordView.setTextColor(getColorCompat(if (isEnabled) R.color.word_recovery_phrase_picker else R.color.word_recovery_phrase_picker_disabled))
    }

    protected fun bottomBarEnabled(enable: Boolean) {
        layout_confirm_recovery_phrase_submit.isEnabled = enable
        layout_confirm_recovery_phrase_bottom_bar_container.setBackgroundColor(getColorCompat(if (enable) R.color.azure else R.color.bluey_grey))
    }

    override fun onDestroy() {
        super.onDestroy()
        randomWordsDisposable?.dispose()
    }

    companion object {
        private const val SCROLL_DELAY_MS = 500L
        const val EXTRA_RECOVERY_PHRASE = "extra.string.recovery_phrase"

        fun createIntent(context: Context, recoveryPhrase: String) = Intent(context, ConfirmRecoveryPhraseActivity::class.java).apply {
            putExtra(EXTRA_RECOVERY_PHRASE, recoveryPhrase)
        }
    }
}
