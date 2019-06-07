package pm.gnosis.heimdall.ui.recoveryphrase

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import io.reactivex.Single

abstract class SetupRecoveryPhraseContract : ViewModel() {
    abstract fun generateRecoveryPhrase(): Single<List<String>>
    abstract fun getRecoveryPhrase(): String?
    abstract fun loadScaledBackgroundResource(targetWidth: Int): Single<Bitmap>
}
