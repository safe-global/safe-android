package pm.gnosis.tests.utils

import kotlinx.coroutines.test.TestCoroutineDispatcher
import pm.gnosis.heimdall.di.modules.ApplicationModule


val testDispatcher = TestCoroutineDispatcher()

val testAppDispatchers = ApplicationModule.AppCoroutineDispatchers(
    testDispatcher,
    testDispatcher,
    testDispatcher,
    testDispatcher,
    testDispatcher
)
