package io.gnosis.safe.ui.safe.add

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.*
import com.jraska.livedata.test
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.Description
import pm.gnosis.utils.asEthereumAddress
import java.lang.IllegalStateException
import java.util.concurrent.CopyOnWriteArrayList

class AddSafeViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()
    private val repositories = mockk<Repositories>().apply {
        every { safeRepository() } returns safeRepository
    }
    private lateinit var viewModel: AddSafeViewModel

    @Before
    fun setup() {
        viewModel = AddSafeViewModel(repositories)
        Dispatchers.setMain(TestCoroutineDispatcher())
    }

    @After
    fun cleanUp() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitAddress - (empty address string) should ShowError with InvalidSafeAddress`() {
        val address = ""

        viewModel.submitAddress(address)

        viewModel.state.test()
            .awaitValue()
            .assertValue {
                it.viewAction is BaseStateViewModel.ViewAction.ShowError &&
                        (it.viewAction as BaseStateViewModel.ViewAction.ShowError).error is InvalidSafeAddress
            }

        coVerify { safeRepository wasNot Called }
    }

    @Test
    fun `submitAddress - (address safeRepository failure) should ShowError`() = runBlocking {
        val address = VALID_SAFE_ADDRESS
        val exception = IllegalStateException()
        coEvery { safeRepository.isValidSafe(address.asEthereumAddress()!!) } throws exception
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.submitAddress(address)

        viewModel.state.observeForever(stateObserver)
        delay(50)
        stateObserver
//            .assertValueCount(2)
            .assertValues(
                CaptureSafe(BaseStateViewModel.ViewAction.Loading(true)),
                CaptureSafe(BaseStateViewModel.ViewAction.ShowError(exception))
            )
        coVerify(exactly = 1) { safeRepository.isValidSafe(VALID_SAFE_ADDRESS.asEthereumAddress()!!) }
    }

    @Test
    fun `submitAddress - (invalid address safeRepository works) should ShowError InvalidSafeAddress`() {
        val address = "0x0"
        coEvery { safeRepository.isValidSafe(address.asEthereumAddress()!!) } returns false

        viewModel.submitAddress(address)

        viewModel.state.test()
            .awaitValue()
            .assertValue {
                it.viewAction is BaseStateViewModel.ViewAction.ShowError &&
                        (it.viewAction as BaseStateViewModel.ViewAction.ShowError).error is InvalidSafeAddress
            }
        coVerify(exactly = 1) { safeRepository.isValidSafe("0x0".asEthereumAddress()!!) }
    }

    @Test
    fun `submitAddress - (valid address) should NavigateTo`() = runBlocking {
        val address = VALID_SAFE_ADDRESS
        coEvery { safeRepository.isValidSafe(address.asEthereumAddress()!!) } returns true
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.submitAddress(address)

        viewModel.state.observeForever(stateObserver)
        delay(50)
        stateObserver
//            .assertValueCount(2)
            .assertValues(
                CaptureSafe(BaseStateViewModel.ViewAction.Loading(true)),
                CaptureSafe(
                    BaseStateViewModel.ViewAction.NavigateTo(
                        AddSafeFragmentDirections.actionAddSafeFragmentToAddSafeNameFragment(address)
                    )
                )
            )

        coVerify(exactly = 1) { safeRepository.isValidSafe(VALID_SAFE_ADDRESS.asEthereumAddress()!!) }
    }

    companion object {
        private const val VALID_SAFE_ADDRESS = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC"
    }
}


open class TestLiveDataObserver<E> : Observer<E> {

    private val values = CopyOnWriteArrayList<E>()

    fun values(): List<E> = values

    fun assertValueCount(expected: Int): TestLiveDataObserver<E> {
        assertEquals("Value count differs;", expected, values.size)
        return this
    }

    fun assertValues(vararg expectedValues: E): TestLiveDataObserver<E> {
//        assertValueCount(expectedValues.size)
        values.forEachIndexed { index, e ->
            assertEquals("Value at $index differs;", expectedValues[index], e)
        }
        return this
    }


    fun assertValueAt(index: Int, predicate: (E) -> Unit): TestLiveDataObserver<E> {
        predicate(values[index])
        return this
    }

    fun assertEmpty(): TestLiveDataObserver<E> {
        assertValueCount(0)
        return this
    }

    fun clear(): TestLiveDataObserver<E> {
        values.clear()
        return this
    }

    override fun onChanged(t: E) {
        values.add(t)
    }
}

class TestLifecycleRule : InstantTaskExecutorRule(), LifecycleOwner {

    private val lifecycle = LifecycleRegistry(this)

    override fun getLifecycle() = lifecycle

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(TestCoroutineDispatcher())
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun finished(description: Description?) {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        Dispatchers.resetMain()
        super.finished(description)
    }

}
