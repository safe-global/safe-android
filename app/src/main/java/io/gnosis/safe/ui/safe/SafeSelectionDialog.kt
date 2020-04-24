package io.gnosis.safe.ui.safe

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Safe
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.databinding.DialogSafeSelectionBinding
import io.gnosis.safe.databinding.ItemAddSafeBinding
import io.gnosis.safe.databinding.ItemSafeBinding
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.adapter.UnsupportedViewType
import io.gnosis.safe.ui.base.BaseBottomSheetDialogFragment
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.svalinn.common.utils.visible
import java.lang.ref.WeakReference
import javax.inject.Inject

class SafeSelectionDialog : BaseBottomSheetDialogFragment<DialogSafeSelectionBinding>() {

    @Inject
    lateinit var viewModel: SafeSelectionViewModel

    @Inject
    lateinit var adapter: SafeSelectionAdapter

    override fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(HeimdallApplication[context!!])
            .build()
            .inject(this)
    }


    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSafeSelectionBinding =
        DialogSafeSelectionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            list.layoutManager = LinearLayoutManager(context)
            list.adapter = adapter
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            adapter.setItems(state.listItems, state.activeSafe)
        })

        viewModel.loadSafes()
    }

    companion object {

        private val TAG = SafeSelectionDialog::class.java.simpleName

        fun show(context: Context) {
            val dialog = SafeSelectionDialog()
            dialog.show((context as FragmentActivity).supportFragmentManager, TAG)
        }
    }
}

data class SafeSelectionState(
    val listItems: List<Any>,
    val activeSafe: Safe?,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State


class SafeSelectionViewModel @Inject constructor(
    repositories: Repositories
) : BaseStateViewModel<SafeSelectionState>(), SafeSelectionAdapter.OnSafeClickedListener {

    private val safeRepository = repositories.safeRepository()

    private var safes: List<Safe> = listOf()

    override val state: LiveData<SafeSelectionState> = liveData {
        for (event in stateChannel.openSubscription())
            emit(event)
    }

    override fun initialState(): SafeSelectionState = SafeSelectionState(
        listOf(AddSafeHeader()), null, null
    )

    fun loadSafes() {
        safeLaunch {
            safes = safeRepository.getSafes()
            val activeSafe = safeRepository.getActiveSafe()

            val listItems = mutableListOf<Any>()
            listItems.add(AddSafeHeader())
            listItems.addAll(safes)

            updateState { SafeSelectionState(listItems, activeSafe, null) }
        }
    }

    override fun onSafeClicked(safe: Safe?) {
        safeLaunch {
            updateState { SafeSelectionState(safes, safe, null) }
        }
    }
}

class SafeSelectionAdapter(
    private val safeClickListener: WeakReference<OnSafeClickedListener>
) : RecyclerView.Adapter<BaseSafeSelectionViewHolder>() {

    private val items = mutableListOf<Any>()

    var activeSafe: Safe? = null
        set(value) {
            field = value
            notifyAllChanged()
        }

    fun setItems(items: List<Any>, activeSafe: Safe?) {
        this.activeSafe = activeSafe
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: BaseSafeSelectionViewHolder, position: Int) {
        when(holder) {
            is AddSafeHeaderViewHolder -> holder.bind()
            is SafeItemViewHolder -> {
                val safe = items[position] as Safe

                holder.bind(safe, safe == activeSafe)

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseSafeSelectionViewHolder {
        return when (SafeSelectionViewTypes.values()[viewType]) {
            SafeSelectionViewTypes.HEADER_ADD_SAFE -> AddSafeHeaderViewHolder(ItemAddSafeBinding.inflate(LayoutInflater.from(parent.context)))
            SafeSelectionViewTypes.SAFE -> SafeItemViewHolder(ItemSafeBinding.inflate(LayoutInflater.from(parent.context)), safeClickListener)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when (item) {
            is AddSafeHeader -> SafeSelectionViewTypes.HEADER_ADD_SAFE.ordinal
            is Safe -> SafeSelectionViewTypes.SAFE.ordinal
            else -> throw UnsupportedViewType(item.toString())
        }
    }

    override fun getItemCount() = items.size

    fun notifyAllChanged() {
        notifyItemRangeChanged(0, items.size)
    }

    enum class SafeSelectionViewTypes {
        HEADER_ADD_SAFE,
        SAFE
    }

    interface OnSafeClickedListener {
        fun onSafeClicked(safe: Safe?)
    }
}

abstract class BaseSafeSelectionViewHolder(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root)

class AddSafeHeader

class AddSafeHeaderViewHolder(
    private val binding: ItemAddSafeBinding
) : BaseSafeSelectionViewHolder(binding) {

    fun bind() {
        binding.root.setOnClickListener {
            it.findNavController()
        }
    }
}

class SafeItemViewHolder(
    private val binding: ItemSafeBinding,
    private val safeClickListener: WeakReference<SafeSelectionAdapter.OnSafeClickedListener>
) : BaseSafeSelectionViewHolder(binding) {

    fun bind(safe: Safe, selected: Boolean) {
        //binding.safeAddress.text = safe.address.asEthereumAddressString()!!//owner.formatEthAddress(binding.root.context)
        binding.safeImage.setAddress(safe.address)
        binding.safeSelection.visible(selected)
        binding.root.setOnClickListener {
            safeClickListener.get()?.onSafeClicked(safe)
        }
    }
}


