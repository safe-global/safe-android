package io.gnosis.safe.ui.settings

import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.models.VersionState
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.Tracker
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.notifications.NotificationManager
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import timber.log.Timber
import java.math.BigInteger

class SafeSettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()
    private val credentialsRepository = mockk<CredentialsRepository>()
    private val notificationRepository = mockk<NotificationRepository>()
    private val notificationManager = mockk<NotificationManager>().apply {
        coEvery { deleteNotificationChannelGroup(any()) } just Runs
    }
    private val tracker = mockk<Tracker>()
    private val ensRepository = mockk<EnsRepository>().apply {
        coEvery { reverseResolve(any(), any()) } returns null
    }

    private lateinit var safeSettingsViewModel: SafeSettingsViewModel

    @Test
    fun `init - (no active safe) should emit loading`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        val testObserver = TestLiveDataObserver<SafeSettingsState>()

        safeSettingsViewModel =
            SafeSettingsViewModel(
                safeRepository,
                ensRepository,
                credentialsRepository,
                notificationRepository,
                notificationManager,
                tracker,
                appDispatchers
            )
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
    fun `init - (activeSafe change) should load new data`() = runTest(UnconfinedTestDispatcher()) {
        val safe1 = Safe(Solidity.Address(BigInteger.ONE), "safe")
        val safe2 = Safe(Solidity.Address(BigInteger.TEN), "safe")
        val safeInfo1 = SafeInfo(
            AddressInfo(safe1.address),
            BigInteger.TEN,
            2,
            emptyList(),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            emptyList(),
            null,
            null,
            "1.1.1",
            VersionState.OUTDATED
        )
        val safeInfo2 = SafeInfo(
            AddressInfo(safe2.address),
            BigInteger.TEN,
            2,
            emptyList(),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            emptyList(),
            null,
            null,
            "1.1.1",
            VersionState.OUTDATED
        )
        val ensName1 = "ens.name"
        val ensName2 = "ens.name"
        coEvery { safeRepository.getSafeInfo(any()) } returnsMany listOf(safeInfo1, safeInfo2)
        coEvery { safeRepository.activeSafeFlow() } returns flowOf(safe1, safe2)
        coEvery { credentialsRepository.owners() } returns emptyList()
        coEvery { ensRepository.reverseResolve(any(), any()) } returnsMany listOf(
            ensName1,
            ensName2
        )
        val testObserver = TestLiveDataObserver<SafeSettingsState>()

        safeSettingsViewModel =
            SafeSettingsViewModel(
                safeRepository,
                ensRepository,
                credentialsRepository,
                notificationRepository,
                notificationManager,
                tracker,
                appDispatchers
            )
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
            safeRepository.getSafeInfo(safe1)
            ensRepository.reverseResolve(safe1.address, safe1.chain)
            safeRepository.getSafeInfo(safe2)
            ensRepository.reverseResolve(safe2.address, safe2.chain)
        }
    }

    @Test
    fun `reload - (activeSafe null) should emit not loading`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
            coEvery { safeRepository.getActiveSafe() } returns null
            coEvery { credentialsRepository.owners() } returns emptyList()
            val testObserver = TestLiveDataObserver<SafeSettingsState>()
            safeSettingsViewModel =
                SafeSettingsViewModel(
                    safeRepository,
                    ensRepository,
                    credentialsRepository,
                    notificationRepository,
                    notificationManager,
                    tracker,
                    appDispatchers
                )

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
    fun `reload - (activeSafe available, everything works) should emit everything`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "safe")
        val safeInfo = SafeInfo(
            AddressInfo(safe.address),
            BigInteger.TEN,
            2,
            emptyList(),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            emptyList(),
            null,
            null,
            "1.1.1",
            VersionState.OUTDATED
        )
        val ensName = "ens.name"
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { safeRepository.getSafeInfo(any()) } returns safeInfo
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        coEvery { credentialsRepository.owners() } returns emptyList()
        coEvery { ensRepository.reverseResolve(any(), any()) } returns ensName
        val testObserver = TestLiveDataObserver<SafeSettingsState>()
        safeSettingsViewModel =
            SafeSettingsViewModel(
                safeRepository,
                ensRepository,
                credentialsRepository,
                notificationRepository,
                notificationManager,
                tracker,
                appDispatchers
            )

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
            safeRepository.getSafeInfo(safe)
            ensRepository.reverseResolve(safe.address, safe.chain)
        }
    }

    @Test
    fun `reload - (activeSafe available, ensFailure) should emit safe data with null name`() =
        runTest(
            UnconfinedTestDispatcher()
        ) {
            val throwable = Throwable()
            val safe = Safe(Solidity.Address(BigInteger.ONE), "safe")
            val safeInfo = SafeInfo(
                AddressInfo(safe.address),
                BigInteger.TEN,
                2,
                emptyList(),
                AddressInfo(Solidity.Address(BigInteger.ONE)),
                emptyList(),
                null,
                null,
                "1.1.1",
                VersionState.OUTDATED
            )
            coEvery { safeRepository.getActiveSafe() } returns safe
            coEvery { credentialsRepository.owners() } returns emptyList()
            coEvery { safeRepository.getSafeInfo(any()) } returns safeInfo
            coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
            coEvery { ensRepository.reverseResolve(any(), any()) } throws throwable
            mockkStatic(Timber::class)
            val testObserver = TestLiveDataObserver<SafeSettingsState>()
            safeSettingsViewModel =
                SafeSettingsViewModel(
                    safeRepository,
                    ensRepository,
                    credentialsRepository,
                    notificationRepository,
                    notificationManager,
                    tracker,
                    appDispatchers
                )

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
                safeRepository.getSafeInfo(safe)
                ensRepository.reverseResolve(safe.address, safe.chain)
                Timber.e(throwable)
            }
        }

    @Test
    fun `reload - (activeSafe available, safeInfo failure) should emit ShowError`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val throwable = Throwable()
        val safe = Safe(Solidity.Address(BigInteger.ONE), "safe")
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { safeRepository.getSafeInfo(any()) } throws throwable
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        val testObserver = TestLiveDataObserver<SafeSettingsState>()
        safeSettingsViewModel =
            SafeSettingsViewModel(
                safeRepository,
                ensRepository,
                credentialsRepository,
                notificationRepository,
                notificationManager,
                tracker,
                appDispatchers
            )

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
            safeRepository.getSafeInfo(safe)
            ensRepository wasNot Called
        }
    }

    @Test
    fun `removeSafe (one safe) - should remove safe and clear active safe`() = runTest(
        UnconfinedTestDispatcher()
    ) {
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
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(
            AddressInfo(SAFE_1.address),
            BigInteger.ONE,
            2,
            emptyList(),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            emptyList(),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            null,
            "1.1.1",
            VersionState.OUTDATED
        )
        coEvery { credentialsRepository.owners() } returns emptyList()
        coEvery { notificationRepository.unregisterSafe(any(), any()) } just Runs
        coEvery { tracker.logSafeRemoved(any()) } just Runs
        coEvery { tracker.setNumSafes(any()) } just Runs

        safeSettingsViewModel =
            SafeSettingsViewModel(
                safeRepository,
                ensRepository,
                credentialsRepository,
                notificationRepository,
                notificationManager,
                tracker,
                appDispatchers
            )
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        safeSettingsViewModel.state.observeForever(stateObserver)

        safeSettingsViewModel.removeSafe()

        with(stateObserver.values()[0] as SafeSettingsState) {
            assertTrue(safe == null && viewAction is BaseStateViewModel.ViewAction.Loading)
        }

        with(stateObserver.values()[1] as SafeSettingsState) {
            assertEquals(safe, SAFE_1)
            println("viewAction: $viewAction")
            assertTrue(viewAction is SafeRemoved)
        }

        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getSafeInfo(SAFE_1)
            safeRepository.getActiveSafe()
            safeRepository.removeSafe(SAFE_1)
            safeRepository.getSafes()
            safeRepository.clearActiveSafe()
            tracker.logSafeRemoved(any())
            safeRepository.getSafeCount()
            tracker.setNumSafes(0)
            notificationRepository.unregisterSafe(SAFE_1.chainId, SAFE_1.address)
        }
    }

    @Test
    fun `removeSafe (two or more safes) - should remove safe and select next safe`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(
            AddressInfo(SAFE_1.address),
            BigInteger.ONE,
            2,
            emptyList(),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            emptyList(),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            null,
            "1.1.1",
            VersionState.OUTDATED
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
        coEvery { credentialsRepository.owners() } returns emptyList()
        coEvery { notificationRepository.unregisterSafe(any(), any()) } just Runs
        coEvery { tracker.logSafeRemoved(any()) } just Runs
        coEvery { tracker.setNumSafes(any()) } just Runs

        safeSettingsViewModel =
            SafeSettingsViewModel(
                safeRepository,
                ensRepository,
                credentialsRepository,
                notificationRepository,
                notificationManager,
                tracker,
                appDispatchers
            )
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        safeSettingsViewModel.state.observeForever(stateObserver)

        safeSettingsViewModel.removeSafe()

        with(stateObserver.values()[0] as SafeSettingsState) {
            assertTrue(safe == SAFE_2 && viewAction is BaseStateViewModel.ViewAction.Loading)
        }

        with(stateObserver.values()[1] as SafeSettingsState) {
            assertTrue(safe == SAFE_1 && viewAction is SafeRemoved)
        }

        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getSafeInfo(SAFE_1)
            safeRepository.getSafeInfo(SAFE_2)
            safeRepository.getActiveSafe()
            safeRepository.removeSafe(SAFE_1)
            safeRepository.getSafes()
            safeRepository.setActiveSafe(SAFE_2)
            tracker.logSafeRemoved(any())
            safeRepository.getSafeCount()
            tracker.setNumSafes(1)
            notificationRepository.unregisterSafe(SAFE_1.chainId, SAFE_1.address)
        }
    }

    companion object {
        private val SAFE_1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        private val SAFE_2 = Safe(Solidity.Address(BigInteger.ONE), "safe2")
    }
}
