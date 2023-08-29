package io.gnosis.safe.ui.transactions.details

import io.gnosis.data.BuildConfig
import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Chain
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.models.TransactionLocal
import io.gnosis.data.models.VersionState
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.models.transaction.TransactionStatus
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionLocalRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.Tracker
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.readJsonFrom
import io.gnosis.safe.test
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.transactions.details.viewdata.toTransactionDetailsViewData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray
import java.math.BigInteger

class TransactionDetailsViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val transactionLocalRepository = mockk<TransactionLocalRepository>()
    private val safeRepository = mockk<SafeRepository>()
    private val credentialsRepository = mockk<CredentialsRepository>()
    private val settingsHandler = mockk<SettingsHandler>()
    private val tracker = mockk<Tracker>()

    private lateinit var viewModel: TransactionDetailsViewModel

    private val adapter = dataMoshi.adapter(TransactionDetails::class.java)

    @Test
    fun `loadDetails (transactionRepository failure) should emit error`() =
        runTest(UnconfinedTestDispatcher()) {
            val throwable = Throwable()
            val someAddress = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!

            coEvery { transactionRepository.getTransactionDetails(any(), any()) } throws throwable
            coEvery { safeRepository.getActiveSafe() } returns Safe(
                someAddress,
                "safe_name",
                CHAIN_ID
            )
            coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(
                AddressInfo(Solidity.Address(BigInteger.ONE)),
                BigInteger.ONE,
                1,
                listOf(
                    AddressInfo(Solidity.Address(BigInteger.ONE))
                ),
                AddressInfo(Solidity.Address(BigInteger.ONE)),
                listOf(AddressInfo(Solidity.Address(BigInteger.ONE))),
                AddressInfo(Solidity.Address(BigInteger.ONE)),
                null,
                "1.1.1",
                VersionState.OUTDATED
            )

            viewModel = TransactionDetailsViewModel(
                transactionRepository,
                transactionLocalRepository,
                safeRepository,
                credentialsRepository,
                settingsHandler,
                tracker,
                appDispatchers
            )

            viewModel.loadDetails("tx_details_id")

            with(viewModel.state.test().values()) {
                assertEquals(this[0].viewAction, BaseStateViewModel.ViewAction.ShowError(throwable))
            }
            coVerify(exactly = 1) {
                transactionRepository.getTransactionDetails(
                    CHAIN_ID,
                    "tx_details_id"
                )
            }
        }

    @Test
    fun `loadDetails (successful) should emit txDetails`() = runTest(UnconfinedTestDispatcher()) {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val someAddress = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!

        coEvery {
            transactionRepository.getTransactionDetails(
                any(),
                any()
            )
        } returns transactionDetails
        coEvery { transactionLocalRepository.updateLocalTx(any(), any<String>()) } returns null
        coEvery { safeRepository.getSafes() } returns emptyList()
        coEvery { credentialsRepository.owners() } returns listOf()
        coEvery { safeRepository.getActiveSafe() } returns Safe(someAddress, "safe_name", CHAIN_ID)
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            BigInteger.ONE,
            1,
            listOf(
                AddressInfo(Solidity.Address(BigInteger.ONE))
            ),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            listOf(AddressInfo(Solidity.Address(BigInteger.ONE))),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            null,
            "1.1.1",
            VersionState.OUTDATED
        )
        val expectedTransactionInfoViewData =
            transactionDetails.toTransactionDetailsViewData(
                emptyList(),
                canSign = false,
                canExecute = false,
                nextInLine = false,
                owners = emptyList(),
                hasOwnerKey = false
            )

        viewModel = TransactionDetailsViewModel(
            transactionRepository,
            transactionLocalRepository,
            safeRepository,
            credentialsRepository,
            settingsHandler,
            tracker,
            appDispatchers
        )

        viewModel.loadDetails("tx_details_id")

        with(viewModel.state.test().values()) {
            assertEquals(
                UpdateDetails(txDetails = expectedTransactionInfoViewData),
                this[0].viewAction
            )
        }
        coVerify(exactly = 1) {
            transactionRepository.getTransactionDetails(
                CHAIN_ID,
                "tx_details_id"
            )
        }
    }

    @Test
    fun `loadDetails (successful, awaiting execution with local pending tx) should emit txDetails with pending state`() = runTest(UnconfinedTestDispatcher()) {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto).copy(txStatus = TransactionStatus.AWAITING_EXECUTION)
        val someAddress = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!

        coEvery {
            transactionRepository.getTransactionDetails(
                any(),
                any()
            )
        } returns transactionDetails
        coEvery { transactionLocalRepository.updateLocalTx(any(), any<String>()) } returns TransactionLocal(
            CHAIN_ID,
            Solidity.Address(BigInteger.ZERO),
            BigInteger.ZERO,
            (transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails).safeTxHash,
            "",
            TransactionStatus.PENDING,
            0
        )
        coEvery { transactionLocalRepository.delete(any()) } just Runs
        coEvery { safeRepository.getSafes() } returns emptyList()
        coEvery { credentialsRepository.owners() } returns listOf()
        coEvery { safeRepository.getActiveSafe() } returns Safe(someAddress, "safe_name", CHAIN_ID)
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            BigInteger.ONE,
            1,
            listOf(
                AddressInfo(Solidity.Address(BigInteger.ONE))
            ),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            listOf(AddressInfo(Solidity.Address(BigInteger.ONE))),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            null,
            "1.1.1",
            VersionState.OUTDATED
        )
        val expectedTransactionInfoViewData =
            transactionDetails.toTransactionDetailsViewData(
                emptyList(),
                canSign = false,
                canExecute = false,
                nextInLine = false,
                owners = emptyList(),
                hasOwnerKey = false
            ).copy(txStatus = TransactionStatus.PENDING)

        viewModel = TransactionDetailsViewModel(
            transactionRepository,
            transactionLocalRepository,
            safeRepository,
            credentialsRepository,
            settingsHandler,
            tracker,
            appDispatchers
        )

        viewModel.loadDetails("tx_details_id")

        with(viewModel.state.test().values()) {
            assertEquals(
                UpdateDetails(txDetails = expectedTransactionInfoViewData),
                this[0].viewAction
            )
        }
        coVerify(exactly = 1) {
            transactionRepository.getTransactionDetails(
                CHAIN_ID,
                "tx_details_id"
            )
        }
        coVerify(exactly = 0) {
            transactionLocalRepository.delete(any())
        }
    }

    @Test
    fun `loadDetails (successful, success with local pending tx) should emit txDetails and delete local tx`() = runTest(UnconfinedTestDispatcher()) {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val someAddress = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!

        coEvery {
            transactionRepository.getTransactionDetails(
                any(),
                any()
            )
        } returns transactionDetails
        coEvery { transactionLocalRepository.updateLocalTx(any(), any<String>()) } returns TransactionLocal(
            CHAIN_ID,
            Solidity.Address(BigInteger.ZERO),
            BigInteger.ZERO,
            (transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails).safeTxHash,
            "",
            TransactionStatus.PENDING,
            0
        )
        coEvery { transactionLocalRepository.delete(any()) } just Runs
        coEvery { safeRepository.getSafes() } returns emptyList()
        coEvery { credentialsRepository.owners() } returns listOf()
        coEvery { safeRepository.getActiveSafe() } returns Safe(someAddress, "safe_name", CHAIN_ID)
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            BigInteger.ONE,
            1,
            listOf(
                AddressInfo(Solidity.Address(BigInteger.ONE))
            ),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            listOf(AddressInfo(Solidity.Address(BigInteger.ONE))),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            null,
            "1.1.1",
            VersionState.OUTDATED
        )
        val expectedTransactionInfoViewData =
            transactionDetails.toTransactionDetailsViewData(
                emptyList(),
                canSign = false,
                canExecute = false,
                nextInLine = false,
                owners = emptyList(),
                hasOwnerKey = false
            )

        viewModel = TransactionDetailsViewModel(
            transactionRepository,
            transactionLocalRepository,
            safeRepository,
            credentialsRepository,
            settingsHandler,
            tracker,
            appDispatchers
        )

        viewModel.loadDetails("tx_details_id")

        with(viewModel.state.test().values()) {
            assertEquals(
                UpdateDetails(txDetails = expectedTransactionInfoViewData),
                this[0].viewAction
            )
        }
        coVerify(exactly = 1) {
            transactionRepository.getTransactionDetails(
                CHAIN_ID,
                "tx_details_id"
            )
            transactionLocalRepository.delete(any())
        }
    }

    @Test
    fun `isAwaitingOwnerConfirmation (wrong status) should return false`() =
        runTest(UnconfinedTestDispatcher()) {
            val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
                .copy(txStatus = TransactionStatus.AWAITING_EXECUTION)
            val transactionDetails = toTransactionDetails(transactionDetailsDto)

            viewModel = TransactionDetailsViewModel(
                transactionRepository,
                transactionLocalRepository,
                safeRepository,
                credentialsRepository,
                settingsHandler,
                tracker,
                appDispatchers
            )

            val actual = viewModel.isAwaitingOwnerConfirmation(
                transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
                transactionDetails.txStatus,
                listOf()
            )

            assertEquals(false, actual)
        }

    @Test
    fun `isAwaitingOwnerConfirmation (no owner credential) should return false`() =
        runTest(UnconfinedTestDispatcher()) {
            val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
                .copy(txStatus = TransactionStatus.AWAITING_CONFIRMATIONS)
            val transactionDetails = toTransactionDetails(transactionDetailsDto)
            coEvery { credentialsRepository.ownerCount() } returns 0

            viewModel = TransactionDetailsViewModel(
                transactionRepository,
                transactionLocalRepository,
                safeRepository,
                credentialsRepository,
                settingsHandler,
                tracker,
                appDispatchers
            )

            val actual = viewModel.isAwaitingOwnerConfirmation(
                transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
                transactionDetails.txStatus,
                listOf()
            )

            assertEquals(false, actual)
        }

    @Test
    fun `isAwaitingOwnerConfirmation (owner is not signer) should return false`() =
        runTest(UnconfinedTestDispatcher()) {
            val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
                .copy(txStatus = TransactionStatus.AWAITING_CONFIRMATIONS)
            val transactionDetails = toTransactionDetails(transactionDetailsDto)
            val owners = listOf(Owner("0x1".asEthereumAddress()!!, null, Owner.Type.IMPORTED, null))
            coEvery { credentialsRepository.ownerCount() } returns 1
            coEvery { credentialsRepository.owners() } returns owners

            viewModel = TransactionDetailsViewModel(
                transactionRepository,
                transactionLocalRepository,
                safeRepository,
                credentialsRepository,
                settingsHandler,
                tracker,
                appDispatchers
            )

            val actual = viewModel.isAwaitingOwnerConfirmation(
                transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
                transactionDetails.txStatus,
                owners
            )

            assertEquals(false, actual)
        }

    @Test
    fun `isAwaitingOwnerConfirmation (owner is signer but has already signed) should return false`() =
        runTest(UnconfinedTestDispatcher()) {
            val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
                .copy(txStatus = TransactionStatus.AWAITING_CONFIRMATIONS)
            val transactionDetails = toTransactionDetails(transactionDetailsDto)
            val owners = listOf(
                Owner(
                    "0x65F8236309e5A99Ff0d129d04E486EBCE20DC7B0".asEthereumAddress()!!,
                    null,
                    Owner.Type.IMPORTED,
                    null
                )
            )
            coEvery { credentialsRepository.ownerCount() } returns 1
            coEvery { credentialsRepository.owners() } returns owners

            viewModel = TransactionDetailsViewModel(
                transactionRepository,
                transactionLocalRepository,
                safeRepository,
                credentialsRepository,
                settingsHandler,
                tracker,
                appDispatchers
            )

            val actual = viewModel.isAwaitingOwnerConfirmation(
                transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
                transactionDetails.txStatus,
                owners
            )

            assertEquals(false, actual)
        }

    @Test
    fun `isAwaitingOwnerConfirmation (successful) should return false`() =
        runTest(UnconfinedTestDispatcher()) {
            val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
                .copy(txStatus = TransactionStatus.AWAITING_CONFIRMATIONS)
            val transactionDetails = toTransactionDetails(transactionDetailsDto)
            val owners = listOf(
                Owner(
                    "0x8bc9Ab35a2A8b20ad8c23410C61db69F2e5d8164".asEthereumAddress()!!,
                    null,
                    Owner.Type.IMPORTED,
                    null
                )
            )
            coEvery { credentialsRepository.ownerCount() } returns 1
            coEvery { credentialsRepository.owners() } returns owners

            viewModel = TransactionDetailsViewModel(
                transactionRepository,
                transactionLocalRepository,
                safeRepository,
                credentialsRepository,
                settingsHandler,
                tracker,
                appDispatchers
            )

            val actual = viewModel.isAwaitingOwnerConfirmation(
                transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
                transactionDetails.txStatus,
                owners
            )

            assertEquals(true, actual)
        }

    @Test
    fun `submitConfirmation (invalid safeTxHash) emits error MismatchingSafeTxHash`() =
        runTest(UnconfinedTestDispatcher()) {
            val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
            val transactionDetails = toTransactionDetails(transactionDetailsDto)
            val corruptedTransactionDetails = transactionDetails.copy(
                detailedExecutionInfo = (transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails).copy(
                    nonce = BigInteger.valueOf(
                        -1
                    )
                )
            )
            coEvery { safeRepository.getActiveSafe() } returns Safe(
                "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!,
                "safe_name"
            )

            viewModel = TransactionDetailsViewModel(
                transactionRepository,
                transactionLocalRepository,
                safeRepository,
                credentialsRepository,
                settingsHandler,
                tracker,
                appDispatchers
            )

            viewModel.txDetails = corruptedTransactionDetails

            viewModel.submitConfirmation(corruptedTransactionDetails, "0x00".asEthereumAddress()!!)

            with(viewModel.state.test().values()) {
                assertEquals(
                    this[0].viewAction,
                    BaseStateViewModel.ViewAction.ShowError(MismatchingSafeTxHash)
                )
            }
            coVerify(exactly = 0) { transactionRepository.submitConfirmation(any(), any(), any()) }
        }

    @Test
    fun `submitConfirmation (no owner credentials) emits error MissingOwnerCredentials`() =
        runTest(UnconfinedTestDispatcher()) {
            val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
            val transactionDetails = toTransactionDetails(transactionDetailsDto)
            coEvery { safeRepository.getActiveSafe() } returns Safe(
                "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!,
                "safe_name"
            )
            coEvery { credentialsRepository.ownerCount() } returns 0
            coEvery { credentialsRepository.owners() } returns listOf()

            viewModel = TransactionDetailsViewModel(
                transactionRepository,
                transactionLocalRepository,
                safeRepository,
                credentialsRepository,
                settingsHandler,
                tracker,
                appDispatchers
            )

            viewModel.txDetails = transactionDetails

            viewModel.submitConfirmation(transactionDetails, "0x00".asEthereumAddress()!!)

            with(viewModel.state.test().values()) {
                assertEquals(
                    this[0].viewAction,
                    BaseStateViewModel.ViewAction.ShowError(MissingOwnerCredential)
                )
            }
            coVerify(exactly = 0) { transactionRepository.submitConfirmation(any(), any(), any()) }
            coVerify(exactly = 1) { credentialsRepository.ownerCount() }
            coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        }

    @Test
    fun `submitConfirmation (transactionRepository Failure, sign) emits error`() =
        runTest(UnconfinedTestDispatcher()) {
            val throwable = Throwable()
            val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
            val transactionDetails = toTransactionDetails(transactionDetailsDto)
            val someAddress = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!
            val owner = Owner(someAddress, null, Owner.Type.IMPORTED, null)
            coEvery { safeRepository.getActiveSafe() } returns Safe(someAddress, "safe_name")
            coEvery { credentialsRepository.signWithOwner(any(), any()) } throws throwable
            coEvery {
                transactionRepository.submitConfirmation(
                    any(),
                    any(),
                    any()
                )
            } throws throwable
            coEvery { credentialsRepository.ownerCount() } returns 1
            coEvery { credentialsRepository.owners() } returns listOf(owner)
            coEvery { credentialsRepository.owner(someAddress) } returns owner

            viewModel = TransactionDetailsViewModel(
                transactionRepository,
                transactionLocalRepository,
                safeRepository,
                credentialsRepository,
                settingsHandler,
                tracker,
                appDispatchers
            )

            viewModel.txDetails = transactionDetails

            viewModel.submitConfirmation(transactionDetails, someAddress)

            with(viewModel.state.test().values()) {
                assertTrue(this[0].viewAction is BaseStateViewModel.ViewAction.ShowError)
                assertTrue((this[0].viewAction as BaseStateViewModel.ViewAction.ShowError).error is TxConfirmationFailed)
            }
            coVerify(exactly = 0) { transactionRepository.submitConfirmation(any(), any(), any()) }
            coVerify(exactly = 1) {
                credentialsRepository.signWithOwner(
                    owner,
                    "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67".hexToByteArray()
                )
            }
            coVerify(exactly = 1) { credentialsRepository.owners() }
            coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        }


    @Test
    fun `submitConfirmation (transactionRepository Failure, gateway) emits error`() =
        runTest(UnconfinedTestDispatcher()) {
            val throwable = Throwable()
            val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
            val transactionDetails = toTransactionDetails(transactionDetailsDto)
            val ownerAddress = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!
            val owner = Owner(ownerAddress, null, Owner.Type.IMPORTED, null)
            coEvery { safeRepository.getActiveSafe() } returns Safe(ownerAddress, "safe_name")
            coEvery { credentialsRepository.signWithOwner(any(), any()) } returns ECDSASignature(BigInteger.ZERO, BigInteger.ZERO)
            coEvery {
                transactionRepository.submitConfirmation(
                    any(),
                    any(),
                    any()
                )
            } throws throwable
            coEvery { credentialsRepository.ownerCount() } returns 1
            coEvery { credentialsRepository.owners() } returns listOf(owner)
            coEvery { credentialsRepository.owner(ownerAddress) } returns owner

            viewModel = TransactionDetailsViewModel(
                transactionRepository,
                transactionLocalRepository,
                safeRepository,
                credentialsRepository,
                settingsHandler,
                tracker,
                appDispatchers
            )

            viewModel.txDetails = transactionDetails

            viewModel.submitConfirmation(transactionDetails, ownerAddress)

            with(viewModel.state.test().values()) {
                assertTrue(this[0].viewAction is BaseStateViewModel.ViewAction.ShowError)
                assertTrue((this[0].viewAction as BaseStateViewModel.ViewAction.ShowError).error is TxConfirmationFailed)
            }
            coVerify(exactly = 1) { transactionRepository.submitConfirmation(any(), any(), any()) }
            coVerify(exactly = 1) {
                credentialsRepository.signWithOwner(
                    owner,
                    "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67".hexToByteArray()
                )
            }
            coVerify(exactly = 1) { credentialsRepository.owners() }
            coVerify(exactly = 1) { credentialsRepository.owner(ownerAddress) }
            coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        }

    @Test
    fun `submitConfirmation (successful) emits ConfirmationSubmitted`() =
        runTest(UnconfinedTestDispatcher()) {
            val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
            val transactionDetails = toTransactionDetails(transactionDetailsDto)
            val someAddress = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!
            val owner = Owner(someAddress, null, Owner.Type.IMPORTED, null)
            coEvery { safeRepository.getActiveSafe() } returns Safe(someAddress, "safe_name")
            coEvery { safeRepository.getSafes() } returns emptyList()
            coEvery { credentialsRepository.signWithOwner(any(), any()) } returns ECDSASignature(BigInteger.ZERO, BigInteger.ZERO)
            coEvery {
                transactionRepository.submitConfirmation(
                    any(),
                    any(),
                    any()
                )
            } returns transactionDetails
            coEvery { credentialsRepository.ownerCount() } returns 1
            coEvery { credentialsRepository.owners() } returns listOf(owner)
            coEvery { credentialsRepository.owner(someAddress) } returns owner
            coEvery { tracker.logTransactionConfirmed(any()) } just Runs

            viewModel = TransactionDetailsViewModel(
                transactionRepository,
                transactionLocalRepository,
                safeRepository,
                credentialsRepository,
                settingsHandler,
                tracker,
                appDispatchers
            )

            viewModel.txDetails = transactionDetails

            viewModel.submitConfirmation(transactionDetails, someAddress)

            with(viewModel.state.test().values()) {
                assertEquals(
                    ConfirmationSubmitted(
                        txDetails = transactionDetails.toTransactionDetailsViewData(
                            safes = emptyList(),
                            canSign = false,
                            canExecute = true,
                            nextInLine = false,
                            hasOwnerKey = false,
                            owners = listOf(owner)
                        )
                    ), this[0].viewAction
                )
            }
            coVerify(exactly = 1) { transactionRepository.submitConfirmation(any(), any(), any()) }
            coVerify(exactly = 1) {
                credentialsRepository.signWithOwner(
                    owner,
                    "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67".hexToByteArray()
                )
            }
            coVerify(exactly = 1) { credentialsRepository.owners() }
            coVerify(exactly = 1) { safeRepository.getActiveSafe() }
            coVerify(exactly = 1) { tracker.logTransactionConfirmed(any()) }
        }

    @Test
    fun `startConfirmationFlow should emit NavigateTo with SigningOwnerSelectionFragment`() =
        runTest(UnconfinedTestDispatcher()) {
            val testObserver = TestLiveDataObserver<TransactionDetailsViewState>()
            val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
            val transactionDetails = toTransactionDetails(transactionDetailsDto)
            val someAddress = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!
            coEvery { safeRepository.getActiveSafe() } returns Safe(someAddress, "safe_name")

            viewModel = TransactionDetailsViewModel(
                transactionRepository,
                transactionLocalRepository,
                safeRepository,
                credentialsRepository,
                settingsHandler,
                tracker,
                appDispatchers
            )
            viewModel.txDetails = transactionDetails
            viewModel.state.observeForever(testObserver)

            viewModel.startConfirmationFlow()

            testObserver.assertValueCount(3)
            with(testObserver.values()[1]) {
                assertEquals(
                    BaseStateViewModel.ViewAction.NavigateTo(
                        TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToSigningOwnerSelectionFragment(
                            missingSigners = listOf(
                                "0x8bc9ab35a2a8b20ad8c23410c61db69f2e5d8164",
                                "0xf2cea96575d6b10f51d9af3b10e3e4e5738aa6bd"
                            ).toTypedArray(),
                            signingMode = SigningMode.CONFIRMATION,
                            chain = Chain.DEFAULT_CHAIN,
                            safeTxHash = "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67"
                        )
                    ).toString(), viewAction.toString()
                )
            }
        }

    private suspend fun toTransactionDetails(transactionDetailsDto: TransactionDetails): TransactionDetails {
        val mockGatewayApi = mockk<GatewayApi>().apply {
            coEvery {
                loadTransactionDetails(
                    transactionId = any(),
                    chainId = any()
                )
            } returns transactionDetailsDto
        }
        return TransactionRepository(mockGatewayApi).getTransactionDetails(Chain.ID_MAINNET, "txId")
    }

    companion object {
        private val CHAIN_ID = BuildConfig.CHAIN_ID.toBigInteger()
    }
}
