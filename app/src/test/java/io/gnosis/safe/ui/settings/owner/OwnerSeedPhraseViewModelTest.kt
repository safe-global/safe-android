package io.gnosis.safe.ui.settings.owner

import io.gnosis.data.models.Owner
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.MainCoroutineScopeRule
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import pm.gnosis.crypto.KeyPair
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.mnemonic.InvalidChecksum
import pm.gnosis.model.Solidity
import pm.gnosis.utils.*

class OwnerSeedPhraseViewModelTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val bip39Generator = mockk<Bip39>()
    private val credentialsRepository = mockk<CredentialsRepository>()

    private lateinit var viewModel: OwnerSeedPhraseViewModel

    @Before
    fun setUp() {
        viewModel = OwnerSeedPhraseViewModel(bip39Generator, credentialsRepository, appDispatchers)
    }

    @Test
    fun `init - AwaitingInput state should be emitted`() {
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.state().observeForever(stateObserver)

        stateObserver.assertEmpty()
    }

    @Test
    fun `validate - (valid seed phrase) should emit ValidSeedPhraseSubmitted`() {
        val seedPhrase = "some valid seed phrase"
        every { bip39Generator.validateMnemonic(seedPhrase) } returns seedPhrase
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validateSeedPhrase(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.ValidSeedPhraseSubmitted(seedPhrase)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(seedPhrase) }
    }

    @Test
    fun `validate - (invalid seed phrase) should emit Error`() {
        val seedPhrase = "some valid seed phrase"
        every { bip39Generator.validateMnemonic(seedPhrase) } returns seedPhrase.plus("invalid")
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validateSeedPhrase(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.Error(InvalidSeedPhrase)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(seedPhrase) }
    }

    @Test
    fun `validate - (Bip39Generator exception) should emit Error`() {
        val seedPhrase = "some valid seed phrase"
        val invalidChecksum = InvalidChecksum(seedPhrase, "", "")
        every { bip39Generator.validateMnemonic(seedPhrase) } throws invalidChecksum
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validateSeedPhrase(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.Error(InvalidSeedPhrase)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(seedPhrase) }
    }

    @Test
    fun `validate - (contains linebreak) should emit ValidSeedPhraseSubmitted`() {
        val seedPhrase = "first\nsecond"
        val expected = "first second"
        every { bip39Generator.validateMnemonic(any()) } returns expected
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validateSeedPhrase(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.ValidSeedPhraseSubmitted(expected)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(expected) }
    }

    @Test
    fun `validate - (punctuation mark) should emit ValidSeedPhraseSubmitted`() {
        val seedPhrase = "first.second"
        val expected = "first second"
        every { bip39Generator.validateMnemonic(any()) } returns expected
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validateSeedPhrase(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.ValidSeedPhraseSubmitted(expected)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(expected) }
    }

    @Test
    fun `validate - (contains multiple punctuation marks) should emit ValidSeedPhraseSubmitted`() {
        val seedPhrase = "first........second"
        val expected = "first second"
        every { bip39Generator.validateMnemonic(any()) } returns expected
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validateSeedPhrase(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.ValidSeedPhraseSubmitted(expected)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(expected) }
    }

    @Test
    fun `validate - (contains multiple line breaks) should emit ValidSeedPhraseSubmitted`() {
        val seedPhrase = "first\n\n\nsecond"
        val expected = "first second"
        every { bip39Generator.validateMnemonic(any()) } returns expected
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validateSeedPhrase(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.ValidSeedPhraseSubmitted(expected)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(expected) }
    }

    @Test
    fun `validate - (contains mixed whitespace and punctuation) should emit ValidSeedPhraseSubmitted`() {
        val seedPhrase = "first\n\n\n  . . ?! !  \n\tsecond"
        val expected = "first second"
        every { bip39Generator.validateMnemonic(any()) } returns expected
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validateSeedPhrase(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.ValidSeedPhraseSubmitted(expected)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(expected) }
    }

    @Test
    fun `validate - (casing remains intact) should emit ValidSeedPhraseSubmitted`() {
        val seedPhrase = "fIrSt\n\n\n  . . ?! !  \n\tsecOnD ???Third;;:;:?!\t\tFOURTH"
        val expected = "first second third fourth"
        every { bip39Generator.validateMnemonic(any()) } returns expected
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validateSeedPhrase(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.ValidSeedPhraseSubmitted(expected)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(expected) }
    }

    @Test
    fun `isPrivateKey (64 character long) should succeed`() {

        val result = viewModel.isPrivateKey("0000000000000000000000000000000000000000000000000000000000000001")

        assertTrue(result)
    }

    @Test
    fun `isPrivateKey (64 character long with 0x prefix) should succeed`() {

        val result = viewModel.isPrivateKey("0x0000000000000000000000000000000000000000000000000000000000000001")

        assertTrue(result)
    }

    @Test
    fun `isPrivateKey (key with additional white space) should fail`() {

        val result = viewModel.isPrivateKey("0x0000000000000000000000000000000000000000000000000000000000000001 ")

        assertFalse(result)
    }

    @Test
    fun `isPrivateKey (not 64 character long) should fail`() {

        val result = viewModel.isPrivateKey("00")

        assertFalse(result)
    }

    @Test
    fun `isPrivateKey (random key) should succeed`() {

        val result = viewModel.isPrivateKey("203dA5d2babe41e03b85496a8aDeaDe0472b3ec443edebeed3277501d227DAda")

        assertTrue(result)
    }

    @Test
    fun `isPrivateKey (containing white space) should fail`() {

        val result = viewModel.isPrivateKey("203dA5d2babe41e03b85496a8aDe De0472b3ec443edebeed3277501d227DAda")

        assertFalse(result)
    }

    @Test
    fun `validatePrivateKey (containing zero key) should fail`() {

        val seedPhrase = "fIrSt\n\n\n  . . ?! !  \n\tsecOnD ???Third;;:;:?!\t\tFOURTH"
        val expected = "first second third fourth"
        every { bip39Generator.validateMnemonic(any()) } returns expected
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validatePrivateKey("0x0000000000000000000000000000000000000000000000000000000000000000")

        stateObserver.assertValues(
            ImportOwnerKeyState.Error(InvalidPrivateKey)
        )
    }

    @Test
    fun `validatePrivateKey (good key) should succeed`() {

        val privateKeyString = "0x0000000000000000000000000000000000000000000000000000000000000001".removeHexPrefix()
        val ownerKeyPair = KeyPair.fromPrivate(privateKeyString.hexAsBigInteger())
        val ownerAddressString = Solidity.Address(ownerKeyPair.address.asBigInteger()).asEthereumAddressString()

        coEvery { credentialsRepository.owner(any()) } returns null

        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validatePrivateKey(privateKeyString)

        stateObserver.assertValues(
            ImportOwnerKeyState.ValidKeySubmitted(privateKeyString, ownerAddressString)
        )
    }

    @Test
    fun `validatePrivateKey (good key already imported ) should fail`() {
        coEvery { credentialsRepository.owner(any()) } returns Owner(address = "0x00".asEthereumAddress()!!, type = Owner.Type.IMPORTED)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validatePrivateKey("0x0000000000000000000000000000000000000000000000000000000000000001")

        stateObserver.assertValues(
            ImportOwnerKeyState.Error(KeyAlreadyImported)
        )
    }
}
