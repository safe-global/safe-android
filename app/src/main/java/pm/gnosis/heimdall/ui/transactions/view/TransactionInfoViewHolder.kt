package pm.gnosis.heimdall.ui.transactions.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleObserver
import com.gojuno.koptional.Optional
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction

interface TransactionInfoViewHolder : LifecycleObserver {
    fun loadTransaction(): Single<SafeTransaction>
    fun loadAssetChange(): Single<Optional<ERC20TokenWithBalance>>

    fun inflate(inflater: LayoutInflater, root: ViewGroup)

    fun detach()
}
