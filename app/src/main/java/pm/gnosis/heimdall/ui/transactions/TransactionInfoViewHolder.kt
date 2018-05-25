package pm.gnosis.heimdall.ui.transactions

import android.arch.lifecycle.LifecycleObserver
import android.view.LayoutInflater
import android.view.ViewGroup
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction

interface TransactionInfoViewHolder : LifecycleObserver {
    fun loadTransaction(): Single<SafeTransaction>
    fun inflate(inflater: LayoutInflater, root: ViewGroup)

    fun detach()
}
