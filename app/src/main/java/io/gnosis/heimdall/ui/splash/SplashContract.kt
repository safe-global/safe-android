package io.gnosis.heimdall.ui.splash

sealed class ViewAction
object StartMain : ViewAction()
object StartPasswordSetup : ViewAction()
object StartUnlock : ViewAction()
