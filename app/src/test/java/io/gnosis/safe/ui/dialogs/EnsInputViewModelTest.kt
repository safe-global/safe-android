package io.gnosis.safe.ui.dialogs

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.safe.di.Repositories
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import java.lang.IllegalStateException
import java.math.BigInteger

class EnsInputViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val ensRepository = mockk<EnsRepository>()
    private val repositories = mockk<Repositories>().apply {
        every { ensRepository() } returns ensRepository
    }

    private val viewModel = EnsInputViewModel(repositories)

    @Test
    fun `processEnsInput (valid input safeRepository failure) should throw`() = runBlocking {
        val throwable = IllegalStateException()
        coEvery { ensRepository.resolve(any()) } throws throwable

        val actual = runCatching { viewModel.processEnsInput("") }

        with(actual) {
            assert(isFailure)
            assertEquals(exceptionOrNull(), throwable)
        }
        coVerify(exactly = 1) { ensRepository.resolve("") }
    }

    @Test
    fun `processEnsInput (invalid input) should throw`() = runBlocking {
        coEvery { ensRepository.resolve(any()) } returns null

        val actual = runCatching { viewModel.processEnsInput("") }

        with(actual) {
            assert(isFailure)
            assert(exceptionOrNull() is AddressNotFound)
        }
        coVerify(exactly = 1) { ensRepository.resolve("") }
    }

    @Test
    fun `processEnsInput (valid input) should return Address`() = runBlocking {
        val address = Solidity.Address(BigInteger.ONE)
        coEvery { ensRepository.resolve(any()) } returns address

        val actual = runCatching { viewModel.processEnsInput("safe.eth") }

        with(actual) {
            assert(isSuccess)
            assert(getOrNull() == address)
        }
        coVerify(exactly = 1) { ensRepository.resolve("safe.eth") }
    }

}
