package io.gnosis.safe.ui.settings.owner

import io.gnosis.safe.MainCoroutineScopeRule
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import pm.gnosis.mnemonic.Bip39Generator
import pm.gnosis.mnemonic.InvalidChecksum

class ImportOwnerKeyViewModelTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val bip39Generator = mockk<Bip39Generator>()

    private lateinit var viewModel: ImportOwnerKeyViewModel

    @Test
    fun `init - AwaitingInput state should be emitted`() {
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

        viewModel.state().observeForever(stateObserver)

        stateObserver.assertEmpty()
    }

    @Test
    fun `validate - (valid seed phrase) should emit ValidSeedPhraseSubmitted`() {
        val seedPhrase = "some valid seed phrase"
        every { bip39Generator.validateMnemonic(seedPhrase) } returns seedPhrase
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validate(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.ValidSeedPhraseSubmitted(seedPhrase)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(seedPhrase) }
    }

    @Test
    fun `validate - (invalid seed phrase) should emit Error`() {
        val seedPhrase = "some valid seed phrase"
        every { bip39Generator.validateMnemonic(seedPhrase) } returns seedPhrase.plus("invalid")
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validate(seedPhrase)

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
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validate(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.Error(invalidChecksum)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(seedPhrase) }
    }

    @Test
    fun `validate - (contains linebreak) should emit ValidSeedPhraseSubmitted`() {
        val seedPhrase = "first\nsecond"
        val expected = "first second"
        every { bip39Generator.validateMnemonic(any()) } returns expected
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validate(seedPhrase)

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
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validate(seedPhrase)

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
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validate(seedPhrase)

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
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validate(seedPhrase)

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
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validate(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.ValidSeedPhraseSubmitted(expected)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(expected) }
    }

    @Test
    fun `validate - (casing remains intact) should emit ValidSeedPhraseSubmitted`() {
        val seedPhrase = "fIrSt\n\n\n  . . ?! !  \n\tsecOnD ???Third;;:;:?!\t\tFOURTH"
        val expected = "fIrSt secOnD Third FOURTH"
        every { bip39Generator.validateMnemonic(any()) } returns expected
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state().observeForever(stateObserver)

        viewModel.validate(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.ValidSeedPhraseSubmitted(expected)
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(expected) }
    }
}
