package io.gnosis.safe

import io.gnosis.safe.ui.base.AppDispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher

val appDispatchers = AppDispatchers(UnconfinedTestDispatcher(), UnconfinedTestDispatcher())
