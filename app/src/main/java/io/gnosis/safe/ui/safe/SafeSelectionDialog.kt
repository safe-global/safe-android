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
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Safe
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.R
import io.gnosis.safe.databinding.DialogSafeSelectionBinding
import io.gnosis.safe.databinding.ItemAddSafeBinding
import io.gnosis.safe.databinding.ItemSafeBinding
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseBottomSheetDialogFragment
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.UnsupportedViewType
import io.gnosis.safe.utils.asMiddleEllipsized
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddressString
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
            list.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            list.adapter = adapter
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->

            when(state) {
                is SafeSelectionState.SafeListState -> {
                    adapter.setItems(state.listItems, state.activeSafe)
                }
                is SafeSelectionState.AddSafeState -> {

                    state.viewAction?.let {
                        val action = it as BaseStateViewModel.ViewAction.NavigateTo
                        Navigation.findNavController(activity as FragmentActivity, R.id.safe_overview_root).navigate(action.navDirections)
                        dismiss()
                    }
                }
            }
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

sealed class SafeSelectionState : BaseStateViewModel.State {

    data class SafeListState(
        val listItems: List<Any>,
        val activeSafe: Safe?,
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeSelectionState()

    data class AddSafeState(
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeSelectionState()
}


class SafeSelectionViewModel @Inject constructor(
    repositories: Repositories,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeSelectionState>(appDispatchers), SafeSelectionAdapter.OnSafeSelectionItemClickedListener {

    private val safeRepository = repositories.safeRepository()

    private val items: MutableList<Any> = mutableListOf()
    private var activeSafe: Safe? = null

    override val state: LiveData<SafeSelectionState> = liveData {
        for (event in stateChannel.openSubscription())
            emit(event)
    }

    override fun initialState(): SafeSelectionState = SafeSelectionState.SafeListState(
        listOf(AddSafeHeader()), null, null
    )

    fun loadSafes() {
        safeLaunch {
            val safes = safeRepository.getSafes()
            activeSafe = safeRepository.getActiveSafe()

            items.clear()
            items.add(AddSafeHeader())
            items.addAll(safes)

            updateState { SafeSelectionState.SafeListState(items, activeSafe, null) }
        }
    }

    fun selectSafe(safe: Safe) {
        safeLaunch {
            safeRepository.setActiveSafe(safe)
            updateState { SafeSelectionState.SafeListState(items, safe, null) }
        }
    }

    fun addSafe() {
        safeLaunch {
            updateState {
                SafeSelectionState.AddSafeState(
                    ViewAction.NavigateTo(
                        SafeOverviewFragmentDirections.actionSafeOverviewFragmentToAddSafeNav()
                    )
                )
            }
        }
    }

    override fun onSafeClicked(safe: Safe) {
        selectSafe(safe)
    }

    override fun onAddSafeClicked() {
        addSafe()
    }
}

class SafeSelectionAdapter(
    private val clickListener: WeakReference<OnSafeSelectionItemClickedListener>
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
        when (holder) {
            is AddSafeHeaderViewHolder -> holder.bind()
            is SafeItemViewHolder -> {
                val safe = items[position] as Safe

                holder.bind(safe, safe == activeSafe)

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseSafeSelectionViewHolder {
        return when (SafeSelectionViewTypes.values()[viewType]) {
            SafeSelectionViewTypes.HEADER_ADD_SAFE -> AddSafeHeaderViewHolder(
                ItemAddSafeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ), clickListener
            )
            SafeSelectionViewTypes.SAFE -> SafeItemViewHolder(
                ItemSafeBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                clickListener
            )
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

    interface OnSafeSelectionItemClickedListener {
        fun onSafeClicked(safe: Safe)
        fun onAddSafeClicked()
    }
}

abstract class BaseSafeSelectionViewHolder(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root)

class AddSafeHeader

class AddSafeHeaderViewHolder(
    private val binding: ItemAddSafeBinding,
    private val clickListener: WeakReference<SafeSelectionAdapter.OnSafeSelectionItemClickedListener>
) : BaseSafeSelectionViewHolder(binding) {

    fun bind() {
        binding.root.setOnClickListener {
            clickListener.get()?.onAddSafeClicked()
        }
    }
}

class SafeItemViewHolder(
    private val binding: ItemSafeBinding,
    private val clickListener: WeakReference<SafeSelectionAdapter.OnSafeSelectionItemClickedListener>
) : BaseSafeSelectionViewHolder(binding) {

    fun bind(safe: Safe, selected: Boolean) {
        binding.safeName.text = safe.localName
        binding.safeAddress.text = safe.address.asEthereumAddressString().asMiddleEllipsized(4)
        binding.safeImage.setAddress(safe.address)
        binding.safeSelection.visible(selected, View.INVISIBLE)
        binding.root.setOnClickListener {
            clickListener.get()?.onSafeClicked(safe)
        }
    }
}


