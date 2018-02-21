package pm.gnosis.heimdall.ui.addressbook.helpers

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_address_item.view.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.common.utils.visible
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.utils.asEthereumAddressStringOrNull
import timber.log.Timber
import java.math.BigInteger

interface ViewFactory {
    fun get(): View
}

class InflatingViewFactory(private val inflater: LayoutInflater, private val container: ViewGroup, private val layout: Int) : ViewFactory {
    override fun get() = inflater.inflate(layout, container, false)!!
}

class InflatedViewFactory(private val view: View) : ViewFactory {
    override fun get() = view
}

open class AddressInfoViewHolder(val lifecycleOwner: LifecycleOwner, viewFactory: ViewFactory) : View.OnAttachStateChangeListener, LifecycleObserver {

    private val addressBookRepository: AddressBookRepository
    private val disposables = CompositeDisposable()
    val view = viewFactory.get()

    var currentAddress: BigInteger? = null
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

    fun bind(address: BigInteger) {
        currentAddress = address
        view.apply {
            layout_address_item_icon.setAddress(address)
            layout_address_item_value.text = address.asEthereumAddressStringOrNull()
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
