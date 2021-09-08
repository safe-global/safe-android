package io.gnosis.safe.ui.settings.owner.details

import android.graphics.Bitmap
import io.gnosis.data.models.Owner
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.*
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.CloseScreen
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.None
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.mockk.*
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import java.math.BigInteger


class OwnerDetailsViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val credentialsRepository = mockk<CredentialsRepository>()
    private val notificationRepository = mockk<NotificationRepository>()
    private val settingsHandler = mockk<SettingsHandler>()
    private val tracker = mockk<Tracker>()
    private val qrCodeGenerator = mockk<QrCodeGenerator>()

    private lateinit var viewModel: OwnerDetailsViewModel

    @Test
    fun `loadOwnerDetails (ownerAddress) - should load owner name and generate qr code`() {

        val ownerAddress = Solidity.Address(BigInteger.ZERO)
        val ownerName = "owner1"
        val qrCode = mockk<Bitmap>()

        coEvery { credentialsRepository.owner(ownerAddress) } returns Owner(ownerAddress, "owner1", Owner.Type.IMPORTED)
        coEvery { qrCodeGenerator.generateQrCode(any(), any(), any(), any()) } returns qrCode

        viewModel = OwnerDetailsViewModel(credentialsRepository, notificationRepository, settingsHandler, tracker, qrCodeGenerator, appDispatchers)
        val testObserver = TestLiveDataObserver<OwnerDetailsState>()
        viewModel.state.observeForever(testObserver)

        viewModel.loadOwnerDetails(ownerAddress)

        testObserver.assertValues(
            OwnerDetailsState(None),
            OwnerDetailsState(ShowOwnerDetails(OwnerDetails(ownerName, qrCode, false, Owner.Type.IMPORTED)))
        )

        coVerifySequence {
            credentialsRepository.owner(ownerAddress)
            qrCodeGenerator.generateQrCode(any(), any(), any(), any())
        }
    }

    @Test
    fun `removeOwner (ownerAddress) - should remove and unregister owner`() {

        val ownerAddress = Solidity.Address(BigInteger.ZERO)
        val owner = Owner(ownerAddress, null, Owner.Type.IMPORTED, null)

        coEvery { credentialsRepository.owner(any()) } returns owner
        coEvery { credentialsRepository.removeOwner(owner) } just Runs
        coEvery { notificationRepository.unregisterOwners() } just Runs
        coEvery { tracker.logKeyDeleted() } just Runs
        coEvery { credentialsRepository.ownerCount(Owner.Type.IMPORTED) } returns 0
        coEvery { tracker.setNumKeysImported(any()) } just Runs

        viewModel = OwnerDetailsViewModel(credentialsRepository, notificationRepository, settingsHandler, tracker, qrCodeGenerator, appDispatchers)
        val testObserver = TestLiveDataObserver<OwnerDetailsState>()
        viewModel.state.observeForever(testObserver)

        viewModel.removeOwner(ownerAddress)

        testObserver.assertValues(
            OwnerDetailsState(None),
            OwnerDetailsState(CloseScreen)
        )

        coVerifySequence {
            credentialsRepository.owner(ownerAddress)
            credentialsRepository.removeOwner(owner)
            notificationRepository.unregisterOwners()
            tracker.logKeyDeleted()
            credentialsRepository.ownerCount(Owner.Type.IMPORTED)
            tracker.setNumKeysImported(any())
        }
    }
}
