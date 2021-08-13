package io.gnosis.safe.ui.settings.chain

import androidx.paging.PagingData
import io.gnosis.data.models.Chain
import io.gnosis.safe.MainCoroutineScopeRule
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.settings.chain.paging.ChainPagingProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChainSelectionViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val chainPager = mockk<ChainPagingProvider>()

    private lateinit var viewModel: ChainSelectionViewModel

    @Test
    fun `init - should emit Loading`() {

        viewModel = ChainSelectionViewModel(chainPager, appDispatchers)
        val testObserver = TestLiveDataObserver<ChainSelectionState>()
        viewModel.state.observeForever(testObserver)

        testObserver.assertValues(
            ChainSelectionState(Loading(true))
        )
    }

    @Test
    fun `loadChains - should emit ShowChains`() {

        coEvery { chainPager.getChainsStream() } returns flow { emit(PagingData.empty<Chain>()) }

        viewModel = ChainSelectionViewModel(chainPager, appDispatchers)
        val testObserver = TestLiveDataObserver<ChainSelectionState>()
        viewModel.state.observeForever(testObserver)

        viewModel.loadChains()

        with(testObserver.values()) {
            assertEquals(ChainSelectionState(Loading(true)), this[0])
            assertTrue(this[1].viewAction is ShowChains)
        }
    }
}
