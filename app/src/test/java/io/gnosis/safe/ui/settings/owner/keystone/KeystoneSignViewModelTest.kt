package io.gnosis.safe.ui.settings.owner.keystone

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keystone.sdk.KeystoneEthereumSDK
import com.keystone.sdk.KeystoneSDK
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.transactions.details.SigningMode
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import pm.gnosis.svalinn.common.utils.QrCodeGenerator

@RunWith(AndroidJUnit4::class)
class KeystoneSignViewModelTest {

    private val credentialsRepository = mockk<CredentialsRepository>()
    private val qrCodeGenerator = mockk<QrCodeGenerator>()
    private val appDispatchers = mockk<AppDispatchers>(relaxed = true)
    private val keystoreSdk = mockk<KeystoneSDK>()
    private val viewModel =
        KeystoneSignViewModel(credentialsRepository, qrCodeGenerator, appDispatchers, keystoreSdk)

    @Test
    fun parse_signature_should_return_null_when_signature_size_less_than_65() {
        val result = viewModel.parseSignature(
            "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
            KeystoneEthereumSDK.DataType.PersonalMessage,
            5,
            SigningMode.CONFIRMATION
        )

        assertNull(result)
    }

    @Test
    fun parse_signature_should_return_correct_signature_when_signature_size_more_than_65() {
        val result = viewModel.parseSignature(
            "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002e",
            KeystoneEthereumSDK.DataType.PersonalMessage,
            5,
            SigningMode.CONFIRMATION
        )
        val expected =
            "0x0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005"

        assertEquals(expected, result)
    }

    @Test
    fun parse_signature_should_return_correct_signature_when_signature_size_equals_to_65_for_legacy_transaction() {
        val result = viewModel.parseSignature(
            "0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001c",
            KeystoneEthereumSDK.DataType.Transaction,
            5,
            SigningMode.CONFIRMATION
        )
        val expected =
            "0x0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005"

        assertEquals(expected, result)
    }

    @Test
    fun parse_signature_should_return_correct_signature_when_signature_size_equals_to_65_and_v_more_than_35_for_non_legacy_transaction() {
        val result = viewModel.parseSignature(
            "0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002e",
            KeystoneEthereumSDK.DataType.PersonalMessage,
            5,
            SigningMode.CONFIRMATION
        )
        val expected =
            "0x0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005"

        assertEquals(expected, result)
    }

    @Test
    fun parse_signature_should_return_correct_signature_when_signature_size_equals_to_65_and_v_less_than_35_for_non_legacy_transaction() {
        val result = viewModel.parseSignature(
            "0x3f109ebc816a7ac4d245922527246ff018c4272edc33c85d799389867fa2ae526a6a81efdf33f8efa96f9c455de5f2fded46410f0072ceb00d0ae9825d2354e01b",
            KeystoneEthereumSDK.DataType.PersonalMessage,
            5,
            SigningMode.CONFIRMATION
        )
        val expected =
            "0x3f109ebc816a7ac4d245922527246ff018c4272edc33c85d799389867fa2ae526a6a81efdf33f8efa96f9c455de5f2fded46410f0072ceb00d0ae9825d2354e01f"

        assertEquals(expected, result)
    }
}
