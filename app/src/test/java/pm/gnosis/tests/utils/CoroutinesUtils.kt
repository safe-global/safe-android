package pm.gnosis.tests.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import pm.gnosis.heimdall.di.modules.ApplicationModule


@ExperimentalCoroutinesApi
val testDispatcher = TestCoroutineDispatcher()

@ExperimentalCoroutinesApi
val testAppDispatchers = ApplicationModule.AppCoroutineDispatchers(
    testDispatcher,
    testDispatcher,
    testDispatcher,
    testDispatcher,
    testDispatcher
)
