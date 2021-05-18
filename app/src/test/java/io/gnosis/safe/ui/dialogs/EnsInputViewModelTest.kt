package io.gnosis.safe.ui.dialogs

import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.EnsResolutionError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Test
import pm.gnosis.model.Solidity
import java.math.BigInteger

class EnsInputViewModelTest {

    private val ensRepository = mockk<EnsRepository>(relaxed = true, relaxUnitFun = true)

    private val viewModel = EnsInputViewModel(ensRepository)

    @Test
    fun `processEnsInput (valid input ensRepository failure) should throw`() = runBlockingTest {
        val throwable = IllegalStateException()
        coEvery { ensRepository.resolve(any()) } throws throwable

        val actual = runCatching { viewModel.processEnsInput("") }

        with(actual) {
            assertTrue(isFailure)
        }
        coVerify(exactly = 1) { ensRepository.resolve("") }
    }

    @Test
    fun `processEnsInput (invalid input) should throw`() = runBlockingTest {
        coEvery { ensRepository.resolve(any()) } throws EnsResolutionError()

        val actual = runCatching { viewModel.processEnsInput("") }

        with(actual) {
            assertTrue(isFailure)
            assertTrue(exceptionOrNull() is EnsResolutionError)
        }
        coVerify(exactly = 1) { ensRepository.resolve("") }
    }

    @Test
    fun `processEnsInput (valid input) should return Address`() = runBlockingTest {
        val address = Solidity.Address(BigInteger.ONE)
        coEvery { ensRepository.resolve(any()) } returns address

        val actual = runCatching { viewModel.processEnsInput("safe.eth") }

        with(actual) {
            assertTrue(isSuccess)
            assertTrue(getOrNull() == address)
        }
        coVerify(exactly = 1) { ensRepository.resolve("safe.eth") }
    }

    @Test
    fun `processEnsInput (invalid characters in input) should throw`() = runBlockingTest {
        coEvery { ensRepository.resolve(any()) } throws IllegalArgumentException()

        val actual = runCatching { viewModel.processEnsInput("") }

        with(actual) {
            assertTrue(isFailure)
        }
        coVerify(exactly = 1) { ensRepository.resolve("") }
    }
}
