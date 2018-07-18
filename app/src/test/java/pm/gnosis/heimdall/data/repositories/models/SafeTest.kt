package pm.gnosis.heimdall.data.repositories.models

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SafeTest {

    @Mock
    private lateinit var context: Context

    @Test
    fun displayName() {
        assertEquals(Safe(TEST_SAFE, "Deployed Safe").displayName(context), "Deployed Safe")
        assertEquals(
            PendingSafe(TEST_PENDING_SAFE, TEST_TX_HASH, "Pending Safe", TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT).displayName(context),
            "Pending Safe"
        )
        assertEquals(
            RecoveringSafe(
                TEST_RECOVERING_SAFE, TEST_TX_HASH, "Recovering Safe", TEST_SAFE, "", BigInteger.ZERO, BigInteger.ZERO,
                TEST_PAYMENT_TOKEN, BigInteger.ZERO, BigInteger.ZERO, TransactionExecutionRepository.Operation.CALL, emptyList()
            ).displayName(context),
            "Recovering Safe"
        )
        then(context).shouldHaveZeroInteractions()
    }

    @Test
    fun displayNameNoName() {
        context.mockGetString()
        assertEquals(Safe(TEST_SAFE).displayName(context), R.string.default_safe_name.toString())
        then(context).should().getString(R.string.default_safe_name)
        then(context).shouldHaveZeroInteractions()
        assertEquals(
            PendingSafe(TEST_PENDING_SAFE, TEST_TX_HASH, null, TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT).displayName(context),
            R.string.default_safe_name.toString()
        )
        assertEquals(
            RecoveringSafe(
                TEST_RECOVERING_SAFE, TEST_TX_HASH, null, TEST_SAFE, "", BigInteger.ZERO, BigInteger.ZERO,
                TEST_PAYMENT_TOKEN, BigInteger.ZERO, BigInteger.ZERO, TransactionExecutionRepository.Operation.CALL, emptyList()
            ).displayName(context),
            R.string.default_safe_name.toString()
        )
        then(context).should(times(3)).getString(R.string.default_safe_name)
        then(context).shouldHaveZeroInteractions()
    }

    companion object {
        private val TEST_SAFE = "0x1f81FFF89Bd57811983a35650296681f99C65C7E".asEthereumAddress()!!
        private val TEST_TX_HASH = "0xdae721569a948b87c269ebacaa5a4a67728095e32f9e7e4626f109f27a73b40f".hexAsBigInteger()
        private val TEST_PENDING_SAFE = "0xC2AC20b3Bb950C087f18a458DB68271325a48132".asEthereumAddress()!!
        private val TEST_RECOVERING_SAFE = "0xb36574155395D41b92664e7A215103262a14244A".asEthereumAddress()!!
        private val TEST_PAYMENT_TOKEN = ERC20Token.ETHER_TOKEN.address
        private val TEST_PAYMENT_AMOUNT = Wei.ether("0.1").value
    }
}
