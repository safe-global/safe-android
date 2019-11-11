package pm.gnosis.heimdall.ui.base

import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.lifecycle.ViewModel

abstract class NfcViewModelActivity<VM : ViewModel>: ViewModelActivity<VM>() {

    private var adapter: NfcAdapter? = null

    private var tagDelegate: NfcAdapter.ReaderCallback? = null

    private val nfcCallback by lazy {
        NfcAdapter.ReaderCallback {
            tagDelegate?.onTagDiscovered(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = NfcAdapter.getDefaultAdapter(this)
    }

    override fun onResume() {
        super.onResume()
        adapter?.enableReaderMode(this, nfcCallback, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
    }

    override fun onPause() {
        adapter?.disableForegroundDispatch(this)
        super.onPause()
    }

    fun registerNfcCallback(callback: NfcAdapter.ReaderCallback) {
        tagDelegate = callback
    }

    fun unregisterNfcCallback(callback: NfcAdapter.ReaderCallback) {
        if (tagDelegate != callback) return
        tagDelegate = null
    }

}
