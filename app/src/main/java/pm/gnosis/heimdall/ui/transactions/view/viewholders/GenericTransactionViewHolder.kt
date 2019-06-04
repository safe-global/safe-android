package pm.gnosis.heimdall.ui.transactions.view.viewholders

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_generic_transaction_info.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.transactions.builder.GenericTransactionBuilder
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.removeHexPrefix
import pm.gnosis.utils.stringWithNoTrailingZeroes
import java.math.BigInteger

class GenericTransactionViewHolder(
    private val safe: Solidity.Address,
    private val data: TransactionData.Generic,
    private val addressHelper: AddressHelper
) : TransactionInfoViewHolder {

    private val disposables = CompositeDisposable()
    private var view: View? = null

    override fun loadTransaction(): Single<SafeTransaction> =
        Single.fromCallable {
            GenericTransactionBuilder.build(data)
        }.subscribeOn(Schedulers.computation())

    override fun loadAssetChange(): Single<Optional<ERC20TokenWithBalance>> =
        Single.just(if (data.value > BigInteger.ZERO) ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, data.value).toOptional() else None)

    override fun inflate(inflater: LayoutInflater, root: ViewGroup) {
        view = inflater.inflate(R.layout.layout_generic_transaction_info, root, true)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        val view = view ?: return
        view.layout_generic_transaction_info_value.text =
                view.context.getString(R.string.x_ether, Wei(data.value).toEther().stringWithNoTrailingZeroes())
        view.layout_generic_transaction_info_data.text =
                view.context.getString(R.string.x_data_bytes, (data.data?.removeHexPrefix()?.length ?: 0) / 2)

        setupSafeInfo()
        setupToInfo()
    }

    private fun setupSafeInfo() {
        val view = view ?: return
        view.layout_generic_transaction_info_safe_name.visible(false)
        addressHelper.populateAddressInfo(
            view.layout_generic_transaction_info_safe_address,
            view.layout_generic_transaction_info_safe_name,
            view.layout_generic_transaction_info_safe_image,
            safe
        ).forEach { disposables.add(it) }
    }

    private fun setupToInfo() {
        val view = view ?: return
        view.layout_generic_transaction_info_to_name.visible(false)
        addressHelper.populateAddressInfo(
            view.layout_generic_transaction_info_to_address,
            view.layout_generic_transaction_info_to_name,
            view.layout_generic_transaction_info_to_image,
            data.to
        ).forEach { disposables.add(it) }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() {
        disposables.clear()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    override fun detach() {
        stop()
        view = null
    }
}
