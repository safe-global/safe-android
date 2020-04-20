package io.gnosis.safe.ui.safe.add

import org.junit.Assert.*
import org.junit.Test
import pm.gnosis.model.Solidity
import java.math.BigInteger

class AddSafeViewModelTest {

    private val viewModel = AddSafeViewModel()

    @Test
    fun `submitAddress - valid address should return true`() {
        val safeAddress = Solidity.Address(BigInteger.ONE)

        viewModel.submitAddress(safeAddress)
    }
}
