package io.gnosis.safe.ui.transactions.execution

import io.gnosis.data.backend.rpc.RpcClient
import io.gnosis.data.models.Chain
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionLocalRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.Tracker
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.test
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.owner.list.OwnerViewData
import io.gnosis.safe.utils.BalanceFormatter
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import java.math.BigInteger

class TxReviewViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()
    private val credentialsRepository = mockk<CredentialsRepository>()
    private val localTxRepository = mockk<TransactionLocalRepository>()
    private val settingsHandler = mockk<SettingsHandler>()
    private val rpcClient = mockk<RpcClient>()
    private val balanceFormatter = BalanceFormatter()
    private val tracker = mockk<Tracker>()

    private lateinit var viewModel: TxReviewViewModel

    @Test
    fun `loadDefaultKey() (success) should emit executionKey`() {
        coEvery { safeRepository.getActiveSafe() } returns TEST_SAFE.apply {
            signingOwners = listOf(Solidity.Address(BigInteger.ONE), Solidity.Address(BigInteger.TEN))
        }
        coEvery { credentialsRepository.owners() } returns listOf(TEST_SAFE_OWNER1)
        coEvery { rpcClient.getBalances(any()) } returns listOf(Wei(BigInteger.TEN.pow(Chain.DEFAULT_CHAIN.currency.decimals)))

        viewModel = TxReviewViewModel(
            safeRepository,
            credentialsRepository,
            localTxRepository,
            settingsHandler,
            rpcClient,
            balanceFormatter,
            tracker,
            appDispatchers
        )

        viewModel.loadDefaultKey()

        with(viewModel.state.test().values()) {
            Assert.assertEquals(
                DefaultKey(
                    OwnerViewData(
                        TEST_SAFE_OWNER1.address,
                        TEST_SAFE_OWNER1.name,
                        Owner.Type.IMPORTED,
                        "1 ${Chain.DEFAULT_CHAIN.currency.symbol}",
                        false
                    )
                ), this[0].viewAction
            )
        }
    }

    @Test
    fun `loadDefaultKey() (getBalances failure) should emit LoadBalancesFailed`() {
        coEvery { safeRepository.getActiveSafe() } returns TEST_SAFE.apply {
            signingOwners = listOf(Solidity.Address(BigInteger.ONE), Solidity.Address(BigInteger.TEN))
        }
        coEvery { credentialsRepository.owners() } returns listOf(TEST_SAFE_OWNER1)
        coEvery { rpcClient.getBalances(any()) } throws Throwable()

        viewModel = TxReviewViewModel(
            safeRepository,
            credentialsRepository,
            localTxRepository,
            settingsHandler,
            rpcClient,
            balanceFormatter,
            tracker,
            appDispatchers
        )

        viewModel.loadDefaultKey()

        with(viewModel.state.test().values()) {
            Assert.assertEquals(
                BaseStateViewModel.ViewAction.ShowError(
                    LoadBalancesFailed
                ), this[0].viewAction
            )
        }
    }

    @Test
    fun `updateDefaultKey(different address) emit DefaultKey and track key changed`() {
        coEvery { safeRepository.getActiveSafe() } returns TEST_SAFE.apply {
            signingOwners = listOf(Solidity.Address(BigInteger.ONE), Solidity.Address(BigInteger.TEN))
        }
        coEvery { credentialsRepository.owner(any()) } returns TEST_SAFE_OWNER1
        coEvery { tracker.logTxExecKeyChanged() } just Runs

        viewModel = TxReviewViewModel(
            safeRepository,
            credentialsRepository,
            localTxRepository,
            settingsHandler,
            rpcClient,
            balanceFormatter,
            tracker,
            appDispatchers
        )

        viewModel.updateDefaultKey(TEST_SAFE_OWNER2.address)

        with(viewModel.state.test().values()) {
            Assert.assertEquals(
                DefaultKey(
                    OwnerViewData(
                        TEST_SAFE_OWNER1.address,
                        TEST_SAFE_OWNER1.name,
                        Owner.Type.IMPORTED,
                        null,
                        false
                    )
                ), this[0].viewAction
            )
        }

        coVerify(exactly = 1) {
           tracker.logTxExecKeyChanged()
        }
    }

    @Test
    fun `updateDefaultKey(same address) emit DefaultKey and not track key changed`() {
        coEvery { safeRepository.getActiveSafe() } returns TEST_SAFE.apply {
            signingOwners = listOf(Solidity.Address(BigInteger.ONE), Solidity.Address(BigInteger.TEN))
        }
        coEvery { credentialsRepository.owners() } returns listOf(TEST_SAFE_OWNER1)
        coEvery { credentialsRepository.owner(any()) } returns TEST_SAFE_OWNER1
        coEvery { rpcClient.getBalances(any()) } returns listOf(Wei(BigInteger.TEN.pow(Chain.DEFAULT_CHAIN.currency.decimals)))
        coEvery { tracker.logTxExecKeyChanged() } just Runs

        viewModel = TxReviewViewModel(
            safeRepository,
            credentialsRepository,
            localTxRepository,
            settingsHandler,
            rpcClient,
            balanceFormatter,
            tracker,
            appDispatchers
        )

        viewModel.loadDefaultKey()
        viewModel.updateDefaultKey(TEST_SAFE_OWNER1.address)

        with(viewModel.state.test().values()) {
            Assert.assertEquals(
                DefaultKey(
                    OwnerViewData(
                        TEST_SAFE_OWNER1.address,
                        TEST_SAFE_OWNER1.name,
                        Owner.Type.IMPORTED,
                        null,
                        false
                    )
                ), this[0].viewAction
            )
        }

        coVerify(exactly = 0) {
            tracker.logTxExecKeyChanged()
        }
    }

    companion object {
        val TEST_SAFE = Safe(
            Solidity.Address(BigInteger.ZERO),
            "safe_name",
            Chain.DEFAULT_CHAIN.chainId
        )

        val TEST_SAFE_OWNER1 = Owner(
            Solidity.Address(BigInteger.ONE),
            "owner1",
            Owner.Type.IMPORTED
        )

        val TEST_SAFE_OWNER2 = Owner(
            Solidity.Address(BigInteger.TEN),
            "owner2",
            Owner.Type.IMPORTED
        )
    }
}
