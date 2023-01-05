package io.gnosis.safe.ui.transactions.details

import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import java.math.BigInteger

class TransactionDetailsActionViewModelTest {
    
    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()
    private val credentialsRepository = mockk<CredentialsRepository>()

    private lateinit var viewModel: TransactionDetailsActionViewModel


    @Test
    fun `extendAddressInfoIndexWithLocalData - should add AddressInfo from local safes and owners to empty addressInfoIndex`() {

        val safe1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        val safe2 = Safe(Solidity.Address(BigInteger.ONE), "safe2")
        val owner1 = Owner(Solidity.Address(BigInteger.valueOf(2)), "owner1", Owner.Type.IMPORTED)

        val addressInfoIndex = mapOf<String, AddressInfo>()
        val extendexAddressInfoIndex = mapOf(
            safe1.address.asEthereumAddressChecksumString() to AddressInfo(
                safe1.address,
                safe1.localName,
                null
            ),
            safe2.address.asEthereumAddressChecksumString() to AddressInfo(
                safe2.address,
                safe2.localName,
                null
            ),
            owner1.address.asEthereumAddressChecksumString() to AddressInfo(
                owner1.address,
                owner1.name!!,
                null
            )
        )

        coEvery { safeRepository.getSafes() } returns listOf(safe1, safe2)
        coEvery { credentialsRepository.owners() } returns listOf(owner1)

        val testObserver = TestLiveDataObserver<ActionDetailsState>()
        viewModel =
            TransactionDetailsActionViewModel(safeRepository, credentialsRepository, appDispatchers)
        viewModel.state.observeForever(testObserver)

        viewModel.extendAddressInfoIndexWithLocalData(addressInfoIndex)

        testObserver.assertValues(
            ActionDetailsState(null, BaseStateViewModel.ViewAction.Loading(true)),
            ActionDetailsState(
                extendexAddressInfoIndex,
                BaseStateViewModel.ViewAction.Loading(false)
            )
        )

        coVerifySequence {
            safeRepository.getSafes()
            credentialsRepository.owners()
        }
    }

    @Test
    fun `extendAddressInfoIndexWithLocalData - should add AddressInfo from local safes and owners to addressInfoIndex`() {

        val knownAddress = Solidity.Address(BigInteger.TEN)
        val knownAddressInfo = AddressInfo(knownAddress, "name", null)

        val safe1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        val safe2 = Safe(Solidity.Address(BigInteger.ONE), "safe2")
        val owner1 = Owner(Solidity.Address(BigInteger.valueOf(2)), "owner1", Owner.Type.IMPORTED)

        val addressInfoIndex = mapOf(
            knownAddress.asEthereumAddressChecksumString() to knownAddressInfo
        )
        val extendexAddressInfoIndex = mapOf(
            knownAddress.asEthereumAddressChecksumString() to knownAddressInfo,
            safe1.address.asEthereumAddressChecksumString() to AddressInfo(
                safe1.address,
                safe1.localName,
                null
            ),
            safe2.address.asEthereumAddressChecksumString() to AddressInfo(
                safe2.address,
                safe2.localName,
                null
            ),
            owner1.address.asEthereumAddressChecksumString() to AddressInfo(
                owner1.address,
                owner1.name!!,
                null
            )
        )

        coEvery { safeRepository.getSafes() } returns listOf(safe1, safe2)
        coEvery { credentialsRepository.owners() } returns listOf(owner1)

        val testObserver = TestLiveDataObserver<ActionDetailsState>()
        viewModel =
            TransactionDetailsActionViewModel(safeRepository, credentialsRepository, appDispatchers)
        viewModel.state.observeForever(testObserver)

        viewModel.extendAddressInfoIndexWithLocalData(addressInfoIndex)

        testObserver.assertValues(
            ActionDetailsState(null, BaseStateViewModel.ViewAction.Loading(true)),
            ActionDetailsState(
                extendexAddressInfoIndex,
                BaseStateViewModel.ViewAction.Loading(false)
            )
        )

        coVerifySequence {
            safeRepository.getSafes()
            credentialsRepository.owners()
        }
    }

    @Test
    fun `extendAddressInfoIndexWithLocalData - should replace AddressInfo in addressInfoIndex for addresses of local safes or owners`() {

        val knownAddress1 = Solidity.Address(BigInteger.ONE)
        val knownAddressInfo1 = AddressInfo(knownAddress1, "known address 1", null)

        val knownAddress2 = Solidity.Address(BigInteger.TEN)
        val knownAddressInfo2 = AddressInfo(knownAddress2, "known address 2", null)

        val safe1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        val safe2 = Safe(Solidity.Address(BigInteger.ONE), "safe2")
        val owner1 = Owner(Solidity.Address(BigInteger.TEN), "owner1", Owner.Type.IMPORTED)

        val addressInfoIndex = mapOf(
            knownAddress1.asEthereumAddressChecksumString() to knownAddressInfo1,
            knownAddress2.asEthereumAddressChecksumString() to knownAddressInfo2
        )
        val extendexAddressInfoIndex = mapOf(
            safe1.address.asEthereumAddressChecksumString() to AddressInfo(
                safe1.address,
                safe1.localName,
                null
            ),
            safe2.address.asEthereumAddressChecksumString() to AddressInfo(
                safe2.address,
                safe2.localName,
                null
            ),
            owner1.address.asEthereumAddressChecksumString() to AddressInfo(
                owner1.address,
                owner1.name!!,
                null
            )
        )

        coEvery { safeRepository.getSafes() } returns listOf(safe1, safe2)
        coEvery { credentialsRepository.owners() } returns listOf(owner1)

        val testObserver = TestLiveDataObserver<ActionDetailsState>()
        viewModel =
            TransactionDetailsActionViewModel(safeRepository, credentialsRepository, appDispatchers)
        viewModel.state.observeForever(testObserver)

        viewModel.extendAddressInfoIndexWithLocalData(addressInfoIndex)

        testObserver.assertValues(
            ActionDetailsState(null, BaseStateViewModel.ViewAction.Loading(true)),
            ActionDetailsState(
                extendexAddressInfoIndex,
                BaseStateViewModel.ViewAction.Loading(false)
            )
        )

        coVerifySequence {
            safeRepository.getSafes()
            credentialsRepository.owners()
        }
    }
}
