package io.gnosis.safe.ui.transactions

import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test

class TransactionsViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()

    private lateinit var transactionsViewModel: TransactionsViewModel

    @Test
    fun `init - (no active safe change) should emit Loading`() {
        val testObserver = TestLiveDataObserver<TransactionsState>()
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        transactionsViewModel = TransactionsViewModel(safeRepository = safeRepository, appDispatchers = appDispatchers)

        transactionsViewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(true, viewAction is BaseStateViewModel.ViewAction.Loading)
            assertEquals(BaseStateViewModel.ViewAction.Loading(true), viewAction)
            assertEquals(safe, null)
        }
        coVerify(exactly = 1) { safeRepository.activeSafeFlow() }
    }
}
