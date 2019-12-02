package pm.gnosis.heimdall.helpers

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.loggedTry
import pm.gnosis.utils.nullOnThrow

interface NfcHost {
    fun registerNfcCallback(callback: NfcAdapter.ReaderCallback)
    fun unregisterNfcCallback(callback: NfcAdapter.ReaderCallback)
}

interface NfcAdapterProvider {
    fun get(context: Context): NfcAdapter?
}

object DefaultNfcAdapterProvider : NfcAdapterProvider {
    override fun get(context: Context): NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
}

class NfcHelper(
    private val nfcAdapterProvider: NfcAdapterProvider = DefaultNfcAdapterProvider
) {
    private var context: Context? = null
    private var adapter: NfcAdapter? = null
    fun init(context: Context?): Boolean {
        this.context = context
        adapter = context?.let { nullOnThrow { nfcAdapterProvider.get(it) } }
        return adapter != null
    }

    fun enable(activity: Activity?, callback: NfcAdapter.ReaderCallback) {
        val context = context
        when {
            activity is NfcHost -> activity.registerNfcCallback(callback)
            context is NfcHost -> context.registerNfcCallback(callback)
            else -> loggedTry {
                activity?.let {
                    adapter?.enableReaderMode(activity, callback, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
                }
            }
        }
    }

    fun disable(activity: Activity?, callback: NfcAdapter.ReaderCallback) {
        val context = context
        when {
            activity is NfcHost -> activity.unregisterNfcCallback(callback)
            context is NfcHost -> context.unregisterNfcCallback(callback)
            else -> loggedTry {
                activity?.let {
                    adapter?.disableForegroundDispatch(activity)
                }
            }
        }

    }
}


abstract class NfcDialog : DialogFragment() {

    private var nfcHelper = NfcHelper()

    abstract fun nfcCallback(): NfcAdapter.ReaderCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!nfcHelper.init(context)) dismiss()
    }

    override fun onStart() {
        super.onStart()
        nfcHelper.enable(activity, nfcCallback())
    }

    override fun onStop() {
        nfcHelper.disable(activity, nfcCallback())
        super.onStop()
    }

}

class NfcActivityDelegate(
    private val nfcAdapterProvider: NfcAdapterProvider = DefaultNfcAdapterProvider
) : NfcHost {
    private var nfcAdapter: NfcAdapter? = null

    private var tagDelegate: NfcAdapter.ReaderCallback? = null

    private val nfcCallback by lazy {
        NfcAdapter.ReaderCallback {
            tagDelegate?.onTagDiscovered(it)
        }
    }

    fun init(context: Context) {
        nfcAdapter = nullOnThrow { nfcAdapterProvider.get(context) }
    }

    fun enable(activity: Activity) {
        loggedTry {
            nfcAdapter?.enableReaderMode(activity, nfcCallback, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
        }
    }

    fun disable(activity: Activity) {
        loggedTry {
            nfcAdapter?.disableForegroundDispatch(activity)
        }
    }

    override fun registerNfcCallback(callback: NfcAdapter.ReaderCallback) {
        tagDelegate = callback
    }

    override fun unregisterNfcCallback(callback: NfcAdapter.ReaderCallback) {
        if (tagDelegate != callback) return
        tagDelegate = null
    }
}

abstract class NfcActivity(
    private val nfcActivityDelegate: NfcActivityDelegate = NfcActivityDelegate()
) : BaseActivity(), NfcHost by nfcActivityDelegate {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcActivityDelegate.init(this)
    }

    override fun onResume() {
        super.onResume()
        nfcActivityDelegate.enable(this)
    }

    override fun onPause() {
        nfcActivityDelegate.disable(this)
        super.onPause()
    }

}

abstract class NfcViewModelActivity<VM : ViewModel>(
    private val nfcActivityDelegate: NfcActivityDelegate = NfcActivityDelegate()
) : ViewModelActivity<VM>(), NfcHost by nfcActivityDelegate {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcActivityDelegate.init(this)
    }

    override fun onResume() {
        super.onResume()
        nfcActivityDelegate.enable(this)
    }

    override fun onPause() {
        nfcActivityDelegate.disable(this)
        super.onPause()
    }
}
