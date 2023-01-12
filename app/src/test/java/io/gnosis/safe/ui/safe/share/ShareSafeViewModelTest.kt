package io.gnosis.safe.ui.safe.share

import android.graphics.Bitmap
import android.graphics.Color
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import timber.log.Timber
import java.math.BigInteger

class ShareSafeViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()
    private val ensRepository = mockk<EnsRepository>()
    private val qrCodeGenerator = mockk<QrCodeGenerator>()

    private lateinit var viewModel: ShareSafeViewModel

    @Test
    fun `init - (no preconditions) emits loading`() = runBlocking {
        val testObserver = TestLiveDataObserver<ShareSafeState>()
        viewModel = ShareSafeViewModel(safeRepository, ensRepository, qrCodeGenerator, appDispatchers)

        viewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(BaseStateViewModel.ViewAction.Loading(true), viewAction)
        }

        coVerify(exactly = 1) {
            safeRepository wasNot Called
            ensRepository wasNot Called
            qrCodeGenerator wasNot Called
        }
    }

    @Test
    fun `load - (no active safe) emits ShowError`() = runBlocking {
        coEvery { safeRepository.getActiveSafe() } returns null
        val testObserver = TestLiveDataObserver<ShareSafeState>()
        viewModel = ShareSafeViewModel(safeRepository, ensRepository, qrCodeGenerator, appDispatchers)

        viewModel.state.observeForever(testObserver)
        viewModel.load()

        testObserver.assertValueCount(2)
        with(testObserver.values()[1]) {
            assertTrue(viewAction is BaseStateViewModel.ViewAction.ShowError)
            assertTrue((viewAction as BaseStateViewModel.ViewAction.ShowError).error is IllegalStateException)
        }
        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        coVerify {
            ensRepository wasNot Called
            qrCodeGenerator wasNot Called
        }
    }

    @Test
    fun `load - (safeRepository failure) emits ShowError`() = runBlocking {
        val throwable = Throwable()
        coEvery { safeRepository.getActiveSafe() } throws throwable
        val testObserver = TestLiveDataObserver<ShareSafeState>()
        viewModel = ShareSafeViewModel(safeRepository, ensRepository, qrCodeGenerator, appDispatchers)

        viewModel.state.observeForever(testObserver)
        viewModel.load()

        testObserver.assertValueCount(2)
        with(testObserver.values()[1]) {
            assertTrue(viewAction is BaseStateViewModel.ViewAction.ShowError)
            assertEquals(throwable, (viewAction as BaseStateViewModel.ViewAction.ShowError).error)
        }
        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        coVerify {
            ensRepository wasNot Called
            qrCodeGenerator wasNot Called
        }
    }

    @Test
    fun `load - (ensRepository and qrCodeGenerator failure) emits SafeDetails with ensName and bitmap null`() = runBlocking {
        val throwable = Throwable()
        val safe = Safe(Solidity.Address(BigInteger.ONE), "Safe name")
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { ensRepository.reverseResolve(any(), any()) } throws throwable
        coEvery { qrCodeGenerator.generateQrCode(any(), any(), any(), any()) } throws throwable
        mockkStatic(Timber::class)
        val testObserver = TestLiveDataObserver<ShareSafeState>()
        viewModel = ShareSafeViewModel(safeRepository, ensRepository, qrCodeGenerator, appDispatchers)

        viewModel.state.observeForever(testObserver)
        viewModel.load()

        testObserver.assertValueCount(2)
        with(testObserver.values()[1].viewAction) {
            assertTrue(this is ShowSafeDetails)
            val safeDetails = (this as ShowSafeDetails).safeDetails
            assertEquals(safe, safeDetails.safe)
            assertEquals(null, safeDetails.ensName)
            assertEquals(null, safeDetails.qrCode)
        }
        coVerifySequence {
            safeRepository.getActiveSafe()
            ensRepository.reverseResolve(safe.address, safe.chain)
            Timber.e(throwable)
            qrCodeGenerator.generateQrCode(safe.address.asEthereumAddressChecksumString(), any(), any(), Color.WHITE)
            Timber.e(throwable)
        }
    }

    @Test
    fun `load - (valid safe, everything works) emits full SafeDetails`() = runBlocking {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "Safe name")
        val ensName = "ens.name"
        val bitmap = mockk<Bitmap>()
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { ensRepository.reverseResolve(any(), any()) } returns ensName
        coEvery { qrCodeGenerator.generateQrCode(any(), any(), any(), any()) } returns bitmap
        val testObserver = TestLiveDataObserver<ShareSafeState>()
        viewModel = ShareSafeViewModel(safeRepository, ensRepository, qrCodeGenerator, appDispatchers)

        viewModel.state.observeForever(testObserver)
        viewModel.load()

        testObserver.assertValueCount(2)
        with(testObserver.values()[1].viewAction) {
            assertTrue(this is ShowSafeDetails)
            val safeDetails = (this as ShowSafeDetails).safeDetails
            assertEquals(safe, safeDetails.safe)
            assertEquals(ensName, safeDetails.ensName)
            assertEquals(bitmap, safeDetails.qrCode)
        }
        coVerifySequence {
            safeRepository.getActiveSafe()
            ensRepository.reverseResolve(safe.address, safe.chain)
            qrCodeGenerator.generateQrCode(safe.address.asEthereumAddressChecksumString(), any(), any(), Color.WHITE)
        }
    }
}
