package pm.gnosis.heimdall.utils

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.isAtEnd(threshold: Int = 0): Boolean {
    return (layoutManager as? LinearLayoutManager)?.isAtEnd(threshold) ?: false
}

fun LinearLayoutManager.isAtEnd(threshold: Int = 0): Boolean {
    val itemCount = itemCount
    val visibleItemCount = childCount
    val firstVisiblePosition = findFirstVisibleItemPosition()

    return firstVisiblePosition + visibleItemCount > itemCount - threshold
}

interface SwipeableViewHolder {
    fun swipeableView(): View

    fun onSwiped()
}

class SwipeableTouchHelperCallback: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val uiUtil = ItemTouchHelper.Callback.getDefaultUIUtil()

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        (viewHolder as? SwipeableViewHolder)?.onSwiped()
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        (viewHolder as? SwipeableViewHolder)?.let {
            uiUtil.onSelected(viewHolder.swipeableView())
        } ?: run {
            super.onSelectedChanged(viewHolder, actionState)
        }
    }

    override fun onChildDrawOver(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        (viewHolder as? SwipeableViewHolder)?.let {
            uiUtil.onDraw(c, recyclerView, viewHolder.swipeableView(), dX, dY, actionState, isCurrentlyActive)
        } ?: run {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        (viewHolder as? SwipeableViewHolder)?.let {
            uiUtil.onDraw(c, recyclerView, viewHolder.swipeableView(), dX, dY, actionState, isCurrentlyActive)
        } ?: run {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    fun clearView(viewHolder: SwipeableViewHolder) {
        uiUtil.clearView(viewHolder.swipeableView())
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as? SwipeableViewHolder)?.let {
            clearView(viewHolder)
        } ?: run {
            super.clearView(recyclerView, viewHolder)
        }
    }
}