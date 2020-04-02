package io.gnosis.heimdall.ui.base

import android.view.View
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.svalinn.common.utils.snackbar

/**
 * View should be root view for notifications
 */
fun View.handleViewAction(action: BaseStateViewModel.ViewAction?, closeScreenAction: () -> Unit) {
    when (action) {
//        is BaseStateViewModel.ViewAction.ShowError -> snackbar(this, action.error)
        is BaseStateViewModel.ViewAction.StartActivity -> context.startActivity(action.intent)
        is BaseStateViewModel.ViewAction.CloseScreen -> closeScreenAction()
    }
}
