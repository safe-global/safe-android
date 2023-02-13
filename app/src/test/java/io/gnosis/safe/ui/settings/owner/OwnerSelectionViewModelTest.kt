package io.gnosis.safe.ui.settings.owner

import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.settings.owner.selection.OwnerSelectionState
import io.gnosis.safe.ui.settings.owner.selection.OwnerSelectionViewModel
import io.gnosis.safe.utils.MnemonicKeyAndAddressDerivator
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.toHexString
import java.math.BigInteger

class OwnerSelectionViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val derivator = mockk<MnemonicKeyAndAddressDerivator>()

    private lateinit var viewModel: OwnerSelectionViewModel
    private val credentialsRepository = mockk<CredentialsRepository>()

    @Test
    fun `getOwnerData - should return owner address and key`() {

        coEvery { derivator.keyForIndex(any()) } returns BigInteger.ONE
        coEvery { derivator.addressesForPage(any(), any()) } returns listOf(Solidity.Address(BigInteger.ZERO))

        viewModel = OwnerSelectionViewModel(derivator, credentialsRepository, appDispatchers)
        val testObserver = TestLiveDataObserver<OwnerSelectionState>()
        viewModel.state.observeForever(testObserver)

        val (address, key) = viewModel.getOwnerData()

        assertEquals(Solidity.Address(BigInteger.ZERO).asEthereumAddressString(), address)
        assertEquals(BigInteger.ONE.toHexString(), key)

        testObserver.assertValues(
            OwnerSelectionState(Loading(true))
        )

        coVerifySequence {
            derivator.keyForIndex(any())
            derivator.addressesForPage(any(), any())
        }
    }
}
