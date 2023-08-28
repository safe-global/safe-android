package io.gnosis.safe.ui.safe.add

import io.gnosis.data.models.Chain
import io.gnosis.data.models.RpcAuthentication
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.ChainInfoRepository
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.Tracker
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.notifications.NotificationManager
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.test
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress

class AddSafeNameViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val tracker: Tracker = mockk()
    private val notificationRepository = mockk<NotificationRepository>()
    private val notificationManager = mockk<NotificationManager>().apply {
        coEvery { createNotificationChannelGroup(any()) } just Runs
    }
    private val safeRepository = mockk<SafeRepository>()
    private val chainInfoRepository = mockk<ChainInfoRepository>()
    private val credentialsRepository = mockk<CredentialsRepository>()
    private val settingsHandler = mockk<SettingsHandler>()

    private lateinit var viewModel: AddSafeNameViewModel
    private val mainnet = Chain(
        Chain.ID_MAINNET,
        false,
        "Mainnet",
        "",
        "",
        "",
        "",
        RpcAuthentication.API_KEY_PATH,
        "",
        "",
        null,
        listOf()
    )
    private val rinkeby = Chain(
        Chain.ID_GOERLI,
        true,
        "Goerli",
        "",
        "",
        "",
        "",
        RpcAuthentication.API_KEY_PATH,
        "",
        "",
        null,
        listOf()
    )

    @Before
    fun setup() {
        viewModel = AddSafeNameViewModel(
            safeRepository,
            chainInfoRepository,
            credentialsRepository,
            settingsHandler,
            notificationManager,
            appDispatchers,
            tracker,
            notificationRepository
        )
        Dispatchers.setMain(TestCoroutineDispatcher())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitAddressAndName (safeRepository failure) should fail`() {
        val throwable = IllegalStateException()
        coEvery { safeRepository.saveSafe(any()) } throws throwable

        viewModel.submitAddressAndName(VALID_SAFE_ADDRESS, "Name", rinkeby)

        val actual = viewModel.state.test().values()

        assertTrue(actual.size == 1)
        assertTrue(
            actual[0].viewAction is BaseStateViewModel.ViewAction.ShowError &&
                    (actual[0].viewAction as BaseStateViewModel.ViewAction.ShowError).error == throwable
        )
        coVerify(exactly = 1) { safeRepository.saveSafe(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId)) }
    }

    @Test
    fun `submitAddressAndName (empty Name) should fail`() {
        val throwable = IllegalStateException()
        coEvery { safeRepository.saveSafe(any()) } throws throwable

        viewModel.submitAddressAndName(VALID_SAFE_ADDRESS, "", mainnet)

        val actual = viewModel.state.test().values()

        assertTrue(actual.size == 1)
        assertTrue(
            actual[0].viewAction is BaseStateViewModel.ViewAction.ShowError &&
                    (actual[0].viewAction as BaseStateViewModel.ViewAction.ShowError).error is InvalidName
        )
        coVerify { safeRepository wasNot Called }
    }

    @Test
    fun `submitAddressAndName (blank name) should fail`() {
        val throwable = IllegalStateException()
        coEvery { safeRepository.saveSafe(any()) } throws throwable

        viewModel.submitAddressAndName(VALID_SAFE_ADDRESS, "    ", mainnet)

        val actual = viewModel.state.test().values()

        assertTrue(actual.size == 1)
        assertTrue(
            actual[0].viewAction is BaseStateViewModel.ViewAction.ShowError &&
                    (actual[0].viewAction as BaseStateViewModel.ViewAction.ShowError).error is InvalidName
        )
        coVerify { safeRepository wasNot Called }
    }

    @Test
    fun `submitAddressAndName (name with additional whitespace) should trim and succeed`() {
        coEvery { safeRepository.getSafeCount() } returns 0
        coEvery { safeRepository.saveSafe(any()) } just Runs
        coEvery { chainInfoRepository.save(any()) } just Runs
        coEvery { notificationRepository.registerSafes(any()) } just Runs
        coEvery { safeRepository.setActiveSafe(any()) } just Runs
        coEvery { tracker.logSafeAdded(any()) } just Runs
        coEvery { tracker.setNumSafes(any()) } just Runs
        coEvery { credentialsRepository.ownerCount() } returns 0
        coEvery { settingsHandler.showOwnerScreen } returns false

        viewModel.submitAddressAndName(VALID_SAFE_ADDRESS, "          Name          ", rinkeby)

        viewModel.state.test()
            .assertValues(
                AddSafeNameState(BaseStateViewModel.ViewAction.CloseScreen)
            )
        coVerifySequence {
            safeRepository.saveSafe(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId))
            chainInfoRepository.save(rinkeby)
            notificationRepository.registerSafes(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId))
            notificationManager.createNotificationChannelGroup(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId))
            safeRepository.setActiveSafe(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId))
            tracker.logSafeAdded(any())
            safeRepository.getSafeCount()
            tracker.setNumSafes(0)
            settingsHandler.showOwnerScreen
        }
    }

    @Test
    fun `submitAddressAndName (name) should succeed`() {
        coEvery { safeRepository.getSafeCount() } returns 0
        coEvery { safeRepository.saveSafe(any()) } just Runs
        coEvery { chainInfoRepository.save(any()) } just Runs
        coEvery { notificationRepository.registerSafes(any()) } just Runs
        coEvery { safeRepository.setActiveSafe(any()) } just Runs
        coEvery { tracker.logSafeAdded(any()) } just Runs
        coEvery { tracker.setNumSafes(any()) } just Runs
        coEvery { credentialsRepository.ownerCount() } returns 0
        coEvery { settingsHandler.showOwnerScreen } returns false

        viewModel.submitAddressAndName(VALID_SAFE_ADDRESS, "Name", rinkeby)

        viewModel.state.test()
            .assertValues(
                AddSafeNameState(BaseStateViewModel.ViewAction.CloseScreen)
            )
        coVerifySequence {
            safeRepository.saveSafe(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId))
            chainInfoRepository.save(rinkeby)
            notificationRepository.registerSafes(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId))
            notificationManager.createNotificationChannelGroup(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId))
            safeRepository.setActiveSafe(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId))
            tracker.logSafeAdded(any())
            safeRepository.getSafeCount()
            tracker.setNumSafes(0)
            settingsHandler.showOwnerScreen
        }
    }

    @Test
    fun `submitAddressAndName (name, should notify about importing owner) should succeed`() {
        coEvery { safeRepository.getSafeCount() } returns 0
        coEvery { safeRepository.saveSafe(any()) } just Runs
        coEvery { chainInfoRepository.save(any()) } just Runs
        coEvery { notificationRepository.registerSafes(any()) } just Runs
        coEvery { safeRepository.setActiveSafe(any()) } just Runs
        coEvery { tracker.logSafeAdded(any()) } just Runs
        coEvery { tracker.setNumSafes(any()) } just Runs
        coEvery { credentialsRepository.ownerCount() } returns 0
        coEvery { settingsHandler.showOwnerScreen } returns true

        viewModel.submitAddressAndName(VALID_SAFE_ADDRESS, "Name", rinkeby)

        viewModel.state.test()
            .assertValues(
                AddSafeNameState(ImportOwner)
            )
        coVerifySequence {
            safeRepository.saveSafe(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId))
            chainInfoRepository.save(rinkeby)
            notificationRepository.registerSafes(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId))
            notificationManager.createNotificationChannelGroup(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId))
            safeRepository.setActiveSafe(Safe(VALID_SAFE_ADDRESS, "Name", rinkeby.chainId))
            tracker.logSafeAdded(any())
            safeRepository.getSafeCount()
            tracker.setNumSafes(0)
            settingsHandler.showOwnerScreen
            credentialsRepository.ownerCount()
        }
    }

    companion object {
        private val VALID_SAFE_ADDRESS = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
    }
}
