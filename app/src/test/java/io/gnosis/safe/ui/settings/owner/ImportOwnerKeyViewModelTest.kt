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

        viewModel.state.observeForever(stateObserver)

        stateObserver.assertValues(ImportOwnerKeyState.AwaitingInput)
    }

    @Test
    fun `validate - (valid seed phrase) should emit ValidSeedPhraseSubmitted`() {
        val seedPhrase = "some valid seed phrase"
        every { bip39Generator.validateMnemonic(seedPhrase) } returns seedPhrase
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state.observeForever(stateObserver)

        viewModel.validate(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.AwaitingInput,
            ImportOwnerKeyState.ValidSeedPhraseSubmitted
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(seedPhrase) }
    }

    @Test
    fun `validate - (invalid seed phrase) should emit Error`() {
        val seedPhrase = "some valid seed phrase"
        every { bip39Generator.validateMnemonic(seedPhrase) } returns seedPhrase.plus("invalid")
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state.observeForever(stateObserver)

        viewModel.validate(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.AwaitingInput,
            ImportOwnerKeyState.Error(BaseStateViewModel.ViewAction.ShowError(InvalidSeedPhrase))
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(seedPhrase) }
    }

    @Test
    fun `validate - (Bip39Generator exception) should emit Error`() {
        val seedPhrase = "some valid seed phrase"
        every { bip39Generator.validateMnemonic(seedPhrase) } throws InvalidChecksum(seedPhrase, "", "")
        viewModel = ImportOwnerKeyViewModel(bip39Generator, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel.state.observeForever(stateObserver)

        viewModel.validate(seedPhrase)

        stateObserver.assertValues(
            ImportOwnerKeyState.AwaitingInput,
            ImportOwnerKeyState.Error(BaseStateViewModel.ViewAction.ShowError(InvalidSeedPhrase))
        )
        verify(exactly = 1) { bip39Generator.validateMnemonic(seedPhrase) }
    }
}
