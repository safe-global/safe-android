package pm.gnosis.heimdall.ui.transactions.view.viewholders

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.OnLifecycleEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_connect_extension_transaction_info.view.*
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import java.math.BigInteger

class ConnectExtensionViewHolder(
    private val addressHelper: AddressHelper,
    private val extension: Solidity.Address,
    private val safe: Solidity.Address,
    private val safeRepository: GnosisSafeRepository
) : TransactionInfoViewHolder {
    private var view: View? = null
    private val disposables = CompositeDisposable()

    override fun loadTransaction(): Single<SafeTransaction> =
        safeRepository.loadInfo(safe).singleOrError()
            .map { safeInfo ->
                val newThreshold = safeInfo.requiredConfirmations.toBigInteger() + BigInteger.ONE
                val data = GnosisSafe.AddOwnerWithThreshold.encode(
                    owner = extension,
                    _threshold = Solidity.UInt256(newThreshold)
                )

                SafeTransaction(
                    Transaction(
                        address = safe,
                        data = data
                    ), operation = TransactionExecutionRepository.Operation.CALL
                )
            }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        val view = view ?: return

        addressHelper.populateAddressInfo(
            addressView = view.layout_connect_extension_transaction_info_safe_address,
            nameView = view.layout_connect_extension_transaction_info_safe_name,
            imageView = view.layout_connect_extension_transaction_info_safe_image,
            address = safe
        ).forEach { disposables += it }
    }

    override fun inflate(inflater: LayoutInflater, root: ViewGroup) {
        view = inflater.inflate(R.layout.layout_connect_extension_transaction_info, root, true)
    }

    override fun detach() {
        disposables.clear()
    }
}
