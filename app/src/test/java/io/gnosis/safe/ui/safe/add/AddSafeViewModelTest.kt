package io.gnosis.safe.ui.safe.add

import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SafeStatus
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress

class AddSafeViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()

    private lateinit var viewModel: AddSafeViewModel

    @Before
    fun setup() {
        viewModel = AddSafeViewModel(safeRepository, appDispatchers)
        Dispatchers.setMain(TestCoroutineDispatcher())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitAddress (address safeRepository failure) should ShowError`() {
        val address = VALID_SAFE_ADDRESS
        val exception = IllegalStateException()
        coEvery { safeRepository.isSafeAddressUsed(address) } returns false
        coEvery { safeRepository.getSafeStatus(address) } throws exception
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.validate(address)

        viewModel.state.observeForever(stateObserver)
        stateObserver
            .assertValues(
                AddSafeState(BaseStateViewModel.ViewAction.ShowError(exception))
            )
        coVerifySequence {
            safeRepository.isSafeAddressUsed(VALID_SAFE_ADDRESS)
            safeRepository.getSafeStatus(VALID_SAFE_ADDRESS)
        }
    }

    @Test
    fun `submitAddress (invalid address safeRepository works) should ShowError InvalidSafeAddress`() {
        val address = "0x0".asEthereumAddress()!!
        coEvery { safeRepository.getSafeStatus(address) } returns SafeStatus.INVALID
        coEvery { safeRepository.isSafeAddressUsed(address) } returns false
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.validate(address)

        viewModel.state.observeForever(stateObserver)
        with(stateObserver.values()[0]) {
            assert(
                viewAction is BaseStateViewModel.ViewAction.ShowError &&
                        (viewAction as BaseStateViewModel.ViewAction.ShowError).error is InvalidSafeAddress
            )
        }
        coVerify {
            safeRepository.isSafeAddressUsed(address)
            safeRepository.getSafeStatus(address)
        }
    }

    @Test
    fun `submitAddress (valid unused address) should NavigateTo`() {
        val address = VALID_SAFE_ADDRESS
        coEvery { safeRepository.getSafeStatus(address) } returns SafeStatus.VALID
        coEvery { safeRepository.isSafeAddressUsed(address) } returns false
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.validate(address)

        viewModel.state.observeForever(stateObserver)
        stateObserver.assertValues(AddSafeState(ShowValidSafe(address)))

        coVerifySequence {
            safeRepository.isSafeAddressUsed(VALID_SAFE_ADDRESS)
            safeRepository.getSafeStatus(VALID_SAFE_ADDRESS)
        }
    }

    @Test
    fun `submitAddress (valid used address) should ShowError UsedSafeAddress `() {
        val address = VALID_SAFE_ADDRESS
        coEvery { safeRepository.getSafeStatus(address) } returns SafeStatus.VALID
        coEvery { safeRepository.isSafeAddressUsed(address) } returns true
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.validate(address)

        viewModel.state.observeForever(stateObserver)
        stateObserver.assertValues(AddSafeState(BaseStateViewModel.ViewAction.ShowError(UsedSafeAddress)))

        coVerifySequence {
            safeRepository.isSafeAddressUsed(VALID_SAFE_ADDRESS)
            safeRepository.getSafeStatus(VALID_SAFE_ADDRESS) wasNot Called
        }
    }

    @Test
    fun `validate (valid used address) should ShowError UsedSafeAddress `() {
        val address = VALID_SAFE_ADDRESS
        coEvery { safeRepository.getSafeStatus(address) } returns SafeStatus.VALID
        coEvery { safeRepository.isSafeAddressUsed(address) } returns true
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.validate(address)

        viewModel.state.observeForever(stateObserver)
        stateObserver.assertValues(AddSafeState(BaseStateViewModel.ViewAction.ShowError(UsedSafeAddress)))

        coVerify {
            safeRepository.isSafeAddressUsed(VALID_SAFE_ADDRESS)
            safeRepository.getSafeStatus(VALID_SAFE_ADDRESS) wasNot Called
        }
    }

    @Test
    fun `validate (invalid address) should ShowError UsedSafeAddress `() {
        val address = VALID_SAFE_ADDRESS
        coEvery { safeRepository.getSafeStatus(address) } returns SafeStatus.INVALID
        coEvery { safeRepository.isSafeAddressUsed(address) } returns false
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.validate(address)

        viewModel.state.observeForever(stateObserver)
        stateObserver.assertValues(AddSafeState(BaseStateViewModel.ViewAction.ShowError(InvalidSafeAddress)))

        coVerify {
            safeRepository.isSafeAddressUsed(VALID_SAFE_ADDRESS)
            safeRepository.getSafeStatus(VALID_SAFE_ADDRESS)
        }
    }

    companion object {
        private val VALID_SAFE_ADDRESS = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
    }
}
