package pm.gnosis.heimdall.ui.addressbook.helpers

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_address_item.view.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Provider

open class AddressInfoViewHolder(private val lifecycleOwner: LifecycleOwner, viewProvider: Provider<View>) : View.OnAttachStateChangeListener,
    LifecycleObserver {

    private val addressBookRepository: AddressBookRepository
    private val disposables = CompositeDisposable()
    val view: View = viewProvider.get()

    var currentAddress: Solidity.Address? = null
        private set

    init {
        view.tag = this
        view.addOnAttachStateChangeListener(this)
        // TODO: Non me gusta
        addressBookRepository = HeimdallApplication[view.context].component.addressBookRepository()
    }

    final override fun onViewAttachedToWindow(v: View?) {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    final override fun onViewDetachedFromWindow(v: View?) {
        lifecycleOwner.lifecycle.removeObserver(this)
        stop()
    }

    fun bind(address: Solidity.Address?) {
        disposables.clear()
        currentAddress = address
        view.apply {
            address?.apply { layout_address_item_icon.setAddress(address) } ?: run { layout_address_item_icon.visible(false) }
            layout_address_item_value.text = address?.asEthereumAddressString()
            layout_address_item_value.visible(true)
            layout_address_item_name.visible(false)
        }
        if (view.isAttachedToWindow) onViewAttachedToWindow(view)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    open fun start() {
        val address = currentAddress ?: return
        disposables += addressBookRepository.observeAddressBookEntry(address)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::onAddressInfo, onError = Timber::e)
    }

    protected open fun onAddressInfo(entry: AddressBookEntry) {
        view.layout_address_item_name.visible(true)
        view.layout_address_item_name.text = entry.name
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    open fun stop() {
        disposables.clear()
    }
}
