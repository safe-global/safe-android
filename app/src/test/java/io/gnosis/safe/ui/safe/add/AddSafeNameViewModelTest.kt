package io.gnosis.safe.ui.safe.add

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.Tracker
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.test
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
import java.lang.IllegalStateException

class AddSafeNameViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val tracker: Tracker = mockk<Tracker>()
    private val safeRepository = mockk<SafeRepository>()
    private val repositories = mockk<Repositories>().apply {
        every { safeRepository() } returns safeRepository
    }

    private lateinit var viewModel: AddSafeNameViewModel

    @Before
    fun setup() {
        viewModel = AddSafeNameViewModel(repositories, appDispatchers, tracker)
        Dispatchers.setMain(TestCoroutineDispatcher())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitAddressAndName (safeRepository failure) should fail`() {
        val throwable = IllegalStateException()
        coEvery { safeRepository.addSafe(any()) } throws throwable

        viewModel.submitAddressAndName(VALID_SAFE_ADDRESS, "Name")

        viewModel.state.test()
            .assertValueAt(0) {
                it.viewAction is BaseStateViewModel.ViewAction.ShowError &&
                        (it.viewAction as BaseStateViewModel.ViewAction.ShowError).error == throwable
            }
        coVerify(exactly = 1) { safeRepository.addSafe(Safe(VALID_SAFE_ADDRESS, "Name")) }
    }

    @Test
    fun `submitAddressAndName (empty Name) should fail`() {
        val throwable = IllegalStateException()
        coEvery { safeRepository.addSafe(any()) } throws throwable

        viewModel.submitAddressAndName(VALID_SAFE_ADDRESS, "")

        viewModel.state.test()
            .assertValueAt(0) {
                it.viewAction is BaseStateViewModel.ViewAction.ShowError &&
                        (it.viewAction as BaseStateViewModel.ViewAction.ShowError).error is InvalidName
            }
        coVerify { safeRepository wasNot Called }
    }

    @Test
    fun `submitAddressAndName (blank name) should fail`() {
        val throwable = IllegalStateException()
        coEvery { safeRepository.addSafe(any()) } throws throwable

        viewModel.submitAddressAndName(VALID_SAFE_ADDRESS, "    ")

        viewModel.state.test()
            .assertValueAt(0) {
                it.viewAction is BaseStateViewModel.ViewAction.ShowError &&
                        (it.viewAction as BaseStateViewModel.ViewAction.ShowError).error is InvalidName
            }
        coVerify { safeRepository wasNot Called }
    }

    @Test
    fun `submitAddressAndName (name) should succeed`() {
        coEvery { safeRepository.addSafe(any()) } just Runs

        viewModel.submitAddressAndName(VALID_SAFE_ADDRESS, "Name")

        viewModel.state.test()
            .assertValueAt(0) {
                it.viewAction is BaseStateViewModel.ViewAction.CloseScreen
            }
        coVerify(exactly = 1) { safeRepository.addSafe(Safe(VALID_SAFE_ADDRESS, "Name")) }
    }

    @Test
    fun `submitAddressAndName (name with additional whitespace) should trim and succeed`() {
        coEvery { safeRepository.addSafe(any()) } just Runs

        viewModel.submitAddressAndName(VALID_SAFE_ADDRESS, "          Name          ")

        viewModel.state.test()
            .assertValueAt(0) {
                it.viewAction is BaseStateViewModel.ViewAction.CloseScreen
            }
        coVerify(exactly = 1) { safeRepository.addSafe(Safe(VALID_SAFE_ADDRESS, "Name")) }
    }

    companion object {
        private val VALID_SAFE_ADDRESS = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
    }
}
