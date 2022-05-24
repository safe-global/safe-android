package io.gnosis.safe.ui.safe.add

import io.gnosis.data.models.Chain
import io.gnosis.data.models.RpcAuthentication
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SafeStatus
import io.gnosis.data.repositories.UnstoppableDomainsRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
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

class AddSafeViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val ensRepository = mockk<EnsRepository>()
    private val safeRepository = mockk<SafeRepository>()

    private val mainnet = Chain(
        Chain.ID_MAINNET,
        "Mainnet",
        "",
        "",
        "",
        "",
        RpcAuthentication.API_KEY_PATH,
        "",
        "",
        null
    )

    private lateinit var viewModel: AddSafeViewModel

    @Before
    fun setup() {
        viewModel = AddSafeViewModel(safeRepository, UnstoppableDomainsRepository(), ensRepository, appDispatchers)
        Dispatchers.setMain(TestCoroutineDispatcher())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitAddress (address safeRepository failure) should ShowError`() {
        val validSafe = Safe(VALID_SAFE_ADDRESS, "", mainnet.chainId)
        val exception = IllegalStateException()
        coEvery { safeRepository.isSafeAddressUsed(validSafe) } returns false
        coEvery { safeRepository.getSafeStatus(validSafe) } throws exception
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.validate(validSafe)

        viewModel.state.observeForever(stateObserver)
        stateObserver
            .assertValues(
                AddSafeState(BaseStateViewModel.ViewAction.ShowError(exception))
            )
        coVerifySequence {
            safeRepository.isSafeAddressUsed(validSafe)
            safeRepository.getSafeStatus(validSafe)
        }
    }

    @Test
    fun `submitAddress (invalid address safeRepository works) should ShowError InvalidSafeAddress`() {
        val address = "0x0".asEthereumAddress()!!
        val invalidSafe = Safe(address, "", mainnet.chainId)

        coEvery { safeRepository.getSafeStatus(invalidSafe) } returns SafeStatus.INVALID
        coEvery { safeRepository.isSafeAddressUsed(invalidSafe) } returns false
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.validate(invalidSafe)

        viewModel.state.observeForever(stateObserver)
        with(stateObserver.values()[0]) {
            assertTrue(
                viewAction is BaseStateViewModel.ViewAction.ShowError &&
                        (viewAction as BaseStateViewModel.ViewAction.ShowError).error is InvalidSafeAddress
            )
        }
        coVerify {
            safeRepository.isSafeAddressUsed(invalidSafe)
            safeRepository.getSafeStatus(invalidSafe)
        }
    }

    @Test
    fun `submitAddress (valid unused address) should NavigateTo`() {
        val validSafe = Safe(VALID_SAFE_ADDRESS, "", mainnet.chainId)

        coEvery { safeRepository.getSafeStatus(validSafe) } returns SafeStatus.VALID
        coEvery { safeRepository.isSafeAddressUsed(validSafe) } returns false
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.validate(validSafe)

        viewModel.state.observeForever(stateObserver)
        stateObserver.assertValues(AddSafeState(ShowValidSafe(validSafe)))

        coVerifySequence {
            safeRepository.isSafeAddressUsed(validSafe)
            safeRepository.getSafeStatus(validSafe)
        }
    }

    @Test
    fun `submitAddress (valid used address) should ShowError UsedSafeAddress `() {
        val validSafe = Safe(VALID_SAFE_ADDRESS, "", mainnet.chainId)

        coEvery { safeRepository.getSafeStatus(validSafe) } returns SafeStatus.VALID
        coEvery { safeRepository.isSafeAddressUsed(validSafe) } returns true
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.validate(validSafe)

        viewModel.state.observeForever(stateObserver)
        stateObserver.assertValues(AddSafeState(BaseStateViewModel.ViewAction.ShowError(UsedSafeAddress)))

        coVerifySequence {
            safeRepository.isSafeAddressUsed(validSafe)
            safeRepository.getSafeStatus(validSafe) wasNot Called
        }
    }

    @Test
    fun `validate (valid used address) should ShowError UsedSafeAddress `() {
        val validSafe = Safe(VALID_SAFE_ADDRESS, "", mainnet.chainId)
        coEvery { safeRepository.getSafeStatus(validSafe) } returns SafeStatus.VALID
        coEvery { safeRepository.isSafeAddressUsed(validSafe) } returns true
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.validate(validSafe)

        viewModel.state.observeForever(stateObserver)
        stateObserver.assertValues(AddSafeState(BaseStateViewModel.ViewAction.ShowError(UsedSafeAddress)))

        coVerify {
            safeRepository.isSafeAddressUsed(validSafe)
            safeRepository.getSafeStatus(validSafe) wasNot Called
        }
    }

    @Test
    fun `validate (invalid address) should ShowError UsedSafeAddress `() {
        val validSafe = Safe(VALID_SAFE_ADDRESS, "", mainnet.chainId)
        coEvery { safeRepository.getSafeStatus(validSafe) } returns SafeStatus.INVALID
        coEvery { safeRepository.isSafeAddressUsed(validSafe) } returns false
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.validate(validSafe)

        viewModel.state.observeForever(stateObserver)
        stateObserver.assertValues(AddSafeState(BaseStateViewModel.ViewAction.ShowError(InvalidSafeAddress)))

        coVerify {
            safeRepository.isSafeAddressUsed(validSafe)
            safeRepository.getSafeStatus(validSafe)
        }
    }

    companion object {
        private val VALID_SAFE_ADDRESS = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
    }
}
