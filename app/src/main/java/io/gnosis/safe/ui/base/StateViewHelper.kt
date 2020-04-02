package io.gnosis.safe.ui.base

import android.view.View

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
