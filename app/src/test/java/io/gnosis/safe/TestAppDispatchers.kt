package io.gnosis.safe

import io.gnosis.safe.ui.base.AppDispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher

val appDispatchers = AppDispatchers(TestCoroutineDispatcher(), TestCoroutineDispatcher())
