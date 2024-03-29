package io.gnosis.safe.ui.settings.owner.list

import io.gnosis.data.backend.rpc.RpcClient
import io.gnosis.data.models.Chain
import io.gnosis.data.models.Owner
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.transactions.details.ConfirmConfirmation
import io.gnosis.safe.ui.transactions.details.ConfirmRejection
import io.gnosis.safe.ui.transactions.details.SigningMode
import io.gnosis.safe.ui.transactions.details.SigningOwnerSelectionFragmentDirections
import io.gnosis.safe.utils.BalanceFormatter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

class OwnerListViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()
    private val credentialsRepository = mockk<CredentialsRepository>()
    private val settingsHandler = mockk<SettingsHandler>()
    private val rpcClient = mockk<RpcClient>()
    private val balanceFormatter = BalanceFormatter()

    private lateinit var viewModel: OwnerListViewModel

    @Test
    fun `loadOwners - should load locally saved owners`() {
        val owner1 = Owner(Solidity.Address(BigInteger.ZERO), null, Owner.Type.GENERATED)
        val owner2 = Owner(Solidity.Address(BigInteger.ONE), null, Owner.Type.IMPORTED)

        coEvery { credentialsRepository.owners() } returns listOf(owner1, owner2)

        viewModel = OwnerListViewModel(safeRepository, credentialsRepository, settingsHandler, rpcClient, balanceFormatter, appDispatchers)
        val testObserver = TestLiveDataObserver<OwnerListState>()
        viewModel.state.observeForever(testObserver)

        viewModel.loadOwners()

        val expectedOwnerViewData = listOf(owner1, owner2).map { OwnerViewData(it.address, it.name, it.type) }.sortedBy { it.name }

        testObserver.assertValues(
            OwnerListState(Loading(true)),
            OwnerListState(Loading(true)),
            OwnerListState(LocalOwners(expectedOwnerViewData))
        )

        coVerify(exactly = 1) { credentialsRepository.owners() }
    }

    @Test
    fun `selectKeyForSigning (passcode required) - should start enter passcode flow`() {

        val owner = Owner(Solidity.Address(BigInteger.ZERO), null, Owner.Type.GENERATED)

        coEvery { settingsHandler.usePasscode } returns true
        coEvery { settingsHandler.requirePasscodeForConfirmations } returns true

        viewModel = OwnerListViewModel(safeRepository, credentialsRepository, settingsHandler, rpcClient, balanceFormatter, appDispatchers)
        val testObserver = TestLiveDataObserver<OwnerListState>()
        viewModel.state.observeForever(testObserver)

        viewModel.selectKeyForSigning(owner.address, Owner.Type.IMPORTED, SigningMode.CONFIRMATION, Chain.DEFAULT_CHAIN)

        testObserver.assertValues(
            OwnerListState(Loading(true)),
            OwnerListState(
                NavigateTo(
                    SigningOwnerSelectionFragmentDirections.actionSigningOwnerSelectionFragmentToEnterPasscodeFragment(owner.address.asEthereumAddressString())
                )
            ),
            OwnerListState(None)
        )
    }

    @Test
    fun `selectKeyForSigning (passcode not required, confirmation) - should emit ConfirmConfirmation`() {

        val owner = Owner(Solidity.Address(BigInteger.ZERO), null, Owner.Type.GENERATED)

        coEvery { settingsHandler.usePasscode } returns false

        viewModel = OwnerListViewModel(safeRepository, credentialsRepository, settingsHandler, rpcClient, balanceFormatter, appDispatchers)
        val testObserver = TestLiveDataObserver<OwnerListState>()
        viewModel.state.observeForever(testObserver)

        viewModel.selectKeyForSigning(owner.address, Owner.Type.IMPORTED, SigningMode.CONFIRMATION, Chain.DEFAULT_CHAIN)

        testObserver.assertValues(
            OwnerListState(Loading(true)),
            OwnerListState(ConfirmConfirmation(owner.address)),
            OwnerListState(None)
        )
    }

    @Test
    fun `selectKeyForSigning (passcode not required, not confirmation) - should emit ConfirmRejection`() {

        val owner = Owner(Solidity.Address(BigInteger.ZERO), null, Owner.Type.GENERATED)

        coEvery { settingsHandler.usePasscode } returns false

        viewModel = OwnerListViewModel(safeRepository, credentialsRepository, settingsHandler, rpcClient, balanceFormatter, appDispatchers)
        val testObserver = TestLiveDataObserver<OwnerListState>()
        viewModel.state.observeForever(testObserver)

        viewModel.selectKeyForSigning(owner.address, Owner.Type.IMPORTED, SigningMode.REJECTION, Chain.DEFAULT_CHAIN)

        testObserver.assertValues(
            OwnerListState(Loading(true)),
            OwnerListState(ConfirmRejection(owner.address)),
            OwnerListState(None)
        )
    }
}
