package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_confirm_safe_recovery_phrase.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject


class ConfirmSafeRecoveryPhraseActivity : ViewModelActivity<ConfirmSafeRecoveryPhraseContract>() {
    override fun screenId() = ScreenId.CONFIRM_SAFE_RECOVERY_PHRASE

    // TODO Span count
    private val layoutManager = GridLayoutManager(this, 4, GridLayoutManager.VERTICAL, false)

    // Default composite disposable gets cleared on onStop while clicks need to survive the lifetime of the activity
    private var wordClickDisposables = CompositeDisposable()

    @Inject
    lateinit var adapter: ConfirmSafeRecoveryPhraseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val encryptedMnemonic = intent.getStringExtra(EXTRA_ENCRYPTED_MNEMONIC)
        val chromeExtensionAddress = intent.getStringExtra(EXTRA_CHROME_EXTENSION_ADDRESS).asEthereumAddress()
        if (encryptedMnemonic == null || chromeExtensionAddress == null) {
            finish(); return
        }

        disposables += viewModel.setup(encryptedMnemonic, chromeExtensionAddress)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = ::onSuccessfulSetup, onError = { finish() })

        layout_confirm_safe_recovery_phrase_selected_words.apply {
            setHasFixedSize(true)
            layoutManager = this@ConfirmSafeRecoveryPhraseActivity.layoutManager
            adapter = this@ConfirmSafeRecoveryPhraseActivity.adapter
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_safe_recovery_phrase_finish.clicks()
            .flatMapSingle {
                viewModel.createSafe()
                    .doOnSubscribe {
                        layout_confirm_safe_recovery_phrase_progress_bar.visibility = View.VISIBLE
                        bottomBarEnabled(false)
                    }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onSafeCreated, onError = ::onSafeCreationError)

        disposables += layout_confirm_safe_recovery_phrase_back.clicks()
            .subscribeBy(onNext = { finish() }, onError = Timber::e)
    }

    private fun onIsCorrectSequence(isCorrectSequence: Boolean) {
        bottomBarEnabled(isCorrectSequence)
    }

    private fun onIsCorrectSequenceError(throwable: Throwable) {
        Timber.e(throwable)
        bottomBarEnabled(false)
    }

    private fun bottomBarEnabled(enable: Boolean) {
        layout_safe_recovery_phrase_finish.isEnabled = enable
        layout_safe_recovery_phrase_bottom_bar.setBackgroundColor(getColorCompat(if (enable) R.color.azure else R.color.bluey_grey))
    }

    private fun onSafeCreated(txHash: BigInteger) {
        startActivity(SafeMainActivity.createIntent(this, txHash))
    }

    private fun onSafeCreationError(throwable: Throwable) {
        Timber.e(throwable)
        layout_confirm_safe_recovery_phrase_progress_bar.visibility = View.INVISIBLE
        bottomBarEnabled(true)
        toast(R.string.unknown_error)
    }

    private fun onSuccessfulSetup(words: List<String>) {
        words.forEachIndexed { index, word ->
            when {
                index < 3 -> addWordEntry(word, layout_confirm_safe_recovery_phrase_first_column)
                index < 6 -> addWordEntry(word, layout_confirm_safe_recovery_phrase_second_column)
                index < 9 -> addWordEntry(word, layout_confirm_safe_recovery_phrase_third_column)
                index < 12 -> addWordEntry(word, layout_confirm_safe_recovery_phrase_fourth_column)
            }
        }

        // When the adapter data changes we check if it has the correct sequence
        wordClickDisposables += adapter.observeWords()
            .doOnNext { bottomBarEnabled(false) }
            .flatMapSingle { viewModel.isCorrectSequence(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onIsCorrectSequence, onError = ::onIsCorrectSequenceError)
    }

    private fun addWordEntry(word: String, viewGroup: ViewGroup) =
        (layoutInflater.inflate(R.layout.layout_mnemonic_word_picker, viewGroup, false) as TextView).apply {
            text = word
            wordClickDisposables += clicks().subscribeBy(onNext = {
                adapter.addWord(word)
                layoutManager.scrollToPosition(adapter.itemCount - 1)
            }, onError = Timber::e)
            viewGroup.addView(this)
        }

    override fun layout() = R.layout.layout_confirm_safe_recovery_phrase

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onDestroy() {
        super.onDestroy()
        wordClickDisposables.clear()
    }

    companion object {
        private const val EXTRA_ENCRYPTED_MNEMONIC = "extra.string.encrypted_mnemonic"
        private const val EXTRA_CHROME_EXTENSION_ADDRESS = "extra.string.chrome_extension_address"

        fun createIntent(context: Context, encryptedMnemonic: String, chromeExtensionAddress: Solidity.Address) =
            Intent(context, ConfirmSafeRecoveryPhraseActivity::class.java).apply {
                putExtra(EXTRA_ENCRYPTED_MNEMONIC, encryptedMnemonic)
                putExtra(EXTRA_CHROME_EXTENSION_ADDRESS, chromeExtensionAddress.asEthereumAddressString())
            }
    }
}
