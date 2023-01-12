package io.gnosis.safe.ui.settings.owner.intro

import io.gnosis.safe.R
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.utils.MnemonicKeyAndAddressDerivator
import io.mockk.*
import org.junit.Rule
import org.junit.Test
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.mnemonic.Bip39.Companion.MIN_ENTROPY_BITS
import pm.gnosis.model.Solidity
import java.math.BigInteger

class OwnerGenerateViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val bip39 = mockk<Bip39>()
    private val derivator = mockk<MnemonicKeyAndAddressDerivator>()

    private lateinit var viewModel: OwnerGenerateViewModel

    @Test
    fun `generateOwner () - should  generate mnemonic and derive key and address from it`() {

        val mnemonic = "bla bla bla"
        val key = BigInteger.ZERO
        val address = Solidity.Address(BigInteger.ZERO)

        coEvery { bip39.generateMnemonic(MIN_ENTROPY_BITS, R.id.english) } returns mnemonic
        coEvery { derivator.initialize(mnemonic) } just Runs
        coEvery { derivator.keyForIndex(0) } returns key
        coEvery { derivator.addressesForPage(0, 1) } returns listOf(address)

        viewModel = OwnerGenerateViewModel(bip39, derivator)

        coVerifySequence {
            bip39.generateMnemonic(languageId = R.id.english)
            derivator.initialize(any())
            derivator.keyForIndex(0)
            derivator.addressesForPage(0, 1)
        }
    }
}
