package io.gnosis.safe.ui.base

import android.content.Intent

abstract class ViewState

data class Loading(val isLoading: Boolean) : ViewState()
data class ShowError(val error: Throwable) : ViewState()
data class StartActivity(val intent: Intent) : ViewState()
object CloseScreen : ViewState()
object NavigateUp : ViewState()
