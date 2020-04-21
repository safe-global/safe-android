package io.gnosis.safe.ui.safe.add

import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.di.Repositories
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import pm.gnosis.ethereum.rpc.RpcEthereumRepository
import pm.gnosis.model.Solidity
import java.math.BigInteger

class AddSafeViewModelTest {

    val safeRepository = mockk<SafeRepository>()
    val repositories = mockk<Repositories>().apply {
        every { safeRepository() } returns safeRepository
    }
    private val viewModel = AddSafeViewModel(repositories)

    @Test
    fun `submitAddress - valid address should return true`() {
        val safeAddress = Solidity.Address(BigInteger.ONE)

        viewModel.submitAddress(safeAddress)
    }
}
