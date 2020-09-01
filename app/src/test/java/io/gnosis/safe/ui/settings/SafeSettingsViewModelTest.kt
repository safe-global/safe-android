package io.gnosis.safe.ui.settings

import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.*
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.safe.SafeRemoved
import io.gnosis.safe.ui.settings.safe.SafeSettingsState
import io.gnosis.safe.ui.settings.safe.SafeSettingsViewModel
import io.mockk.*
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import timber.log.Timber
import java.math.BigInteger

class SafeSettingsViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()
    private val notificationRepository = mockk<NotificationRepository>()
    private val tracker = mockk<Tracker>()
    private val ensRepository = mockk<EnsRepository>().apply {
        coEvery { reverseResolve(any()) } returns null
    }

    private lateinit var safeSettingsViewModel: SafeSettingsViewModel

    @Test
    fun `init - (no active safe) should emit loading`() = runBlockingTest {
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        val testObserver = TestLiveDataObserver<SafeSettingsState>()

        safeSettingsViewModel = SafeSettingsViewModel(safeRepository, ensRepository, notificationRepository, tracker, appDispatchers)
        safeSettingsViewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(null, ensName)
            assertEquals(null, safeInfo)
            assertEquals(null, safe)
            assertEquals(BaseStateViewModel.ViewAction.Loading(true), viewAction)
        }
        coVerify(exactly = 1) { safeRepository.activeSafeFlow() }
    }

    @Test
    fun `init - (activeSafe change) should load new data`() = runBlockingTest {
        val safe1 = Safe(Solidity.Address(BigInteger.ONE), "safe")
        val safe2 = Safe(Solidity.Address(BigInteger.TEN), "safe")
        val safeInfo1 = SafeInfo(safe1.address, BigInteger.TEN, 2, emptyList(), Solidity.Address(BigInteger.ONE), emptyList(), null)
        val safeInfo2 = SafeInfo(safe2.address, BigInteger.TEN, 2, emptyList(), Solidity.Address(BigInteger.ONE), emptyList(), null)
        val ensName1 = "ens.name"
        val ensName2 = "ens.name"
        coEvery { safeRepository.getSafeInfo(any()) } returnsMany listOf(safeInfo1, safeInfo2)
        coEvery { safeRepository.activeSafeFlow() } returns flowOf(safe1, safe2)
        coEvery { ensRepository.reverseResolve(any()) } returnsMany listOf(ensName1, ensName2)
        val testObserver = TestLiveDataObserver<SafeSettingsState>()

        safeSettingsViewModel = SafeSettingsViewModel(safeRepository, ensRepository, notificationRepository, tracker, appDispatchers)
        safeSettingsViewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(ensName2, this.ensName)
            assertEquals(safeInfo2, this.safeInfo)
            assertEquals(safe2, this.safe)
            assertEquals(BaseStateViewModel.ViewAction.Loading(false), viewAction)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getSafeInfo(safe1.address)
            ensRepository.reverseResolve(safe1.address)
            safeRepository.getSafeInfo(safe2.address)
            ensRepository.reverseResolve(safe2.address)
        }
    }

    @Test
    fun `reload - (activeSafe null) should emit not loading`() = runBlockingTest {
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        coEvery { safeRepository.getActiveSafe() } returns null
        val testObserver = TestLiveDataObserver<SafeSettingsState>()
        safeSettingsViewModel = SafeSettingsViewModel(safeRepository, ensRepository, notificationRepository, tracker, appDispatchers)

        safeSettingsViewModel.reload()
        safeSettingsViewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(null, ensName)
            assertEquals(null, safeInfo)
            assertEquals(null, safe)
            assertEquals(BaseStateViewModel.ViewAction.Loading(false), viewAction)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.getSafeInfo(any()) wasNot Called
            ensRepository wasNot Called
        }
    }

    @Test
    fun `reload - (activeSafe available, everything works) should emit everything`() = runBlockingTest {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "safe")
        val safeInfo = SafeInfo(safe.address, BigInteger.TEN, 2, emptyList(), Solidity.Address(BigInteger.ONE), emptyList(), null)
        val ensName = "ens.name"
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { safeRepository.getSafeInfo(any()) } returns safeInfo
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        coEvery { ensRepository.reverseResolve(any()) } returns ensName
        val testObserver = TestLiveDataObserver<SafeSettingsState>()
        safeSettingsViewModel = SafeSettingsViewModel(safeRepository, ensRepository, notificationRepository, tracker, appDispatchers)

        safeSettingsViewModel.reload()
        safeSettingsViewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(ensName, this.ensName)
            assertEquals(safeInfo, this.safeInfo)
            assertEquals(safe, this.safe)
            assertEquals(BaseStateViewModel.ViewAction.Loading(false), viewAction)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.getSafeInfo(safe.address)
            ensRepository.reverseResolve(safe.address)
        }
    }

    @Test
    fun `reload - (activeSafe available, ensFailure) should emit safe data with null name`() = runBlockingTest {
        val throwable = Throwable()
        val safe = Safe(Solidity.Address(BigInteger.ONE), "safe")
        val safeInfo = SafeInfo(safe.address, BigInteger.TEN, 2, emptyList(), Solidity.Address(BigInteger.ONE), emptyList(), null)
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { safeRepository.getSafeInfo(any()) } returns safeInfo
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        coEvery { ensRepository.reverseResolve(any()) } throws throwable
        mockkStatic(Timber::class)
        val testObserver = TestLiveDataObserver<SafeSettingsState>()
        safeSettingsViewModel = SafeSettingsViewModel(safeRepository, ensRepository, notificationRepository, tracker, appDispatchers)

        safeSettingsViewModel.reload()
        safeSettingsViewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(null, this.ensName)
            assertEquals(safeInfo, this.safeInfo)
            assertEquals(safe, this.safe)
            assertEquals(BaseStateViewModel.ViewAction.Loading(false), viewAction)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.getSafeInfo(safe.address)
            ensRepository.reverseResolve(safe.address)
            Timber.e(throwable)
        }
    }

    @Test
    fun `reload - (activeSafe available, safeInfo failure) should emit ShowError`() = runBlockingTest {
        val throwable = Throwable()
        val safe = Safe(Solidity.Address(BigInteger.ONE), "safe")
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { safeRepository.getSafeInfo(any()) } throws throwable
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        val testObserver = TestLiveDataObserver<SafeSettingsState>()
        safeSettingsViewModel = SafeSettingsViewModel(safeRepository, ensRepository, notificationRepository, tracker, appDispatchers)

        safeSettingsViewModel.reload()
        safeSettingsViewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(null, this.ensName)
            assertEquals(null, this.safeInfo)
            assertEquals(null, this.safe)
            assertEquals(BaseStateViewModel.ViewAction.ShowError(throwable), viewAction)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.getSafeInfo(safe.address)
            ensRepository wasNot Called
        }
    }

    @Test
    fun `removeSafe (one safe) - should remove safe and clear active safe`() = runBlockingTest {
        coEvery { safeRepository.getActiveSafe() } returnsMany listOf(SAFE_1, null)
        coEvery { safeRepository.activeSafeFlow() } returns flow {
            emit(SAFE_1)
            emit(null)
        }
            .conflate()
        coEvery { safeRepository.clearActiveSafe() } just Runs
        coEvery { safeRepository.getSafes() } returns listOf()
        coEvery { safeRepository.getSafeCount() } returns 0
        coEvery { safeRepository.removeSafe(SAFE_1) } just Runs
        coEvery { notificationRepository.unregisterSafe(any()) } just Runs
        coEvery { tracker.setNumSafes(any()) } just Runs

        safeSettingsViewModel = SafeSettingsViewModel(safeRepository, ensRepository, notificationRepository, tracker, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        safeSettingsViewModel.state.observeForever(stateObserver)

        safeSettingsViewModel.removeSafe()

        with(stateObserver.values()[0] as SafeSettingsState) {
            assert(safe == null && viewAction is BaseStateViewModel.ViewAction.Loading)
        }

        with(stateObserver.values()[1] as SafeSettingsState) {
            assert(safe == SAFE_1 && viewAction is SafeRemoved)
        }

        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.removeSafe(SAFE_1)
            notificationRepository.unregisterSafe(SAFE_1.address)
            safeRepository.getSafes()
            safeRepository.clearActiveSafe()
            safeRepository.getSafeCount()
            // verify SAFE_REMOVE event was tracked
            tracker.setNumSafes(0)
        }
    }

    @Test
    fun `removeSafe (two or more safes) - should remove safe and select next safe`() = runBlockingTest {
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(
            SAFE_1.address,
            BigInteger.ONE,
            2,
            emptyList(),
            Solidity.Address(BigInteger.ONE),
            emptyList(),
            Solidity.Address(BigInteger.ONE)
        )
        coEvery { safeRepository.getActiveSafe() } returnsMany listOf(SAFE_1, SAFE_2)
        coEvery { safeRepository.activeSafeFlow() } returns flow {
            emit(SAFE_1)
            emit(SAFE_2)
        }
            .conflate()
        coEvery { safeRepository.setActiveSafe(any()) } just Runs
        coEvery { safeRepository.getSafes() } returns listOf(SAFE_2)
        coEvery { safeRepository.getSafeCount() } returns 1
        coEvery { safeRepository.removeSafe(SAFE_1) } just Runs
        coEvery { notificationRepository.unregisterSafe(any()) } just Runs
        coEvery { tracker.setNumSafes(any()) } just Runs

        safeSettingsViewModel = SafeSettingsViewModel(safeRepository, ensRepository, notificationRepository, tracker, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        safeSettingsViewModel.state.observeForever(stateObserver)

        safeSettingsViewModel.removeSafe()

        with(stateObserver.values()[0] as SafeSettingsState) {
            assert(safe == SAFE_2 && viewAction is BaseStateViewModel.ViewAction.Loading)
        }

        with(stateObserver.values()[1] as SafeSettingsState) {
            assert(safe == SAFE_1 && viewAction is SafeRemoved)
        }

        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getSafeInfo(SAFE_2.address)
            safeRepository.getActiveSafe()
            safeRepository.removeSafe(SAFE_1)
            notificationRepository.unregisterSafe(SAFE_1.address)
            safeRepository.getSafes()
            safeRepository.setActiveSafe(SAFE_2)
            safeRepository.getSafeCount()
            // verify SAFE_REMOVE event was tracked
            tracker.setNumSafes(1)
        }
    }

    companion object {
        private val SAFE_1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        private val SAFE_2 = Safe(Solidity.Address(BigInteger.ONE), "safe2")
    }
}
