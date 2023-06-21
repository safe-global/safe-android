package io.gnosis.safe.ui.transactions.execution

import android.content.Context
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.math.BigInteger

class TxEditFeeViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val context = mockk<Context>()

    private lateinit var viewModel: TxEditFeeViewModel

    @Test
    fun `validate1559Inputs (valid inputs) should emit no errors`() {

        val testObserver = TestLiveDataObserver<TxEditFeeState>()

        val minNonce = BigInteger.ZERO
        val nonceValue = "1"
        val gasLimitValue = "1"
        val maxPriorityFeeValue = "1"
        val maxFeeValue = "1"

        coEvery { context.getString(any()) } returns ""

        viewModel = TxEditFeeViewModel(appDispatchers)
        viewModel.state.observeForever(testObserver)

        viewModel.validate1559Inputs(
            context,
            minNonce,
            nonceValue,
            gasLimitValue,
            maxPriorityFeeValue,
            maxFeeValue
        )

        testObserver.assertValueCount(2)
        with(testObserver.values()[1]) {
            assert(saveEnabled)
            assertEquals(
                Validate1559FeeData(
                    null,
                    null,
                    null,
                    null
                ),
                viewAction
            )
        }
    }

    @Test
    fun `validate1559Inputs (nonce lt minNonce) should emit nonceError`() {

        val testObserver = TestLiveDataObserver<TxEditFeeState>()

        val minNonce = BigInteger.ONE
        val nonceValue = "0"
        val gasLimitValue = "1"
        val maxPriorityFeeValue = "1"
        val maxFeeValue = "1"

        coEvery { context.getString(any()) } returns ""

        viewModel = TxEditFeeViewModel(appDispatchers)
        viewModel.state.observeForever(testObserver)

        viewModel.validate1559Inputs(
            context,
            minNonce,
            nonceValue,
            gasLimitValue,
            maxPriorityFeeValue,
            maxFeeValue
        )

        testObserver.assertValueCount(2)
        with(testObserver.values()[1]) {
            assertFalse(saveEnabled)
            assertEquals(
                Validate1559FeeData(
                    "",
                    null,
                    null,
                    null
                ),
                viewAction
            )
        }
    }

    @Test
    fun `validate1559Inputs (maxPriorityFeeValue gt maxFee) should emit maxPriorityFeeError and maxFeeError`() {

        val testObserver = TestLiveDataObserver<TxEditFeeState>()

        val minNonce = BigInteger.ZERO
        val nonceValue = "1"
        val gasLimitValue = "1"
        val maxPriorityFeeValue = "2"
        val maxFeeValue = "1"

        coEvery { context.getString(any()) } returns ""

        viewModel = TxEditFeeViewModel(appDispatchers)
        viewModel.state.observeForever(testObserver)

        viewModel.validate1559Inputs(
            context,
            minNonce,
            nonceValue,
            gasLimitValue,
            maxPriorityFeeValue,
            maxFeeValue
        )

        testObserver.assertValueCount(2)
        with(testObserver.values()[1]) {
            assertFalse(saveEnabled)
            assertEquals(
                Validate1559FeeData(
                    null,
                    null,
                    "",
                    ""
                ),
                viewAction
            )
        }
    }

    @Test
    fun `validateLegacyInputs (valid inputs) should emit no errors`() {

        val testObserver = TestLiveDataObserver<TxEditFeeState>()

        val minNonce = BigInteger.ZERO
        val nonceValue = "1"
        val gasLimitValue = "1"
        val gasPrice = "1"

        coEvery { context.getString(any()) } returns ""

        viewModel = TxEditFeeViewModel(appDispatchers)
        viewModel.state.observeForever(testObserver)

        viewModel.validateLegacyInputs(
            context,
            minNonce,
            nonceValue,
            gasLimitValue,
            gasPrice,
        )

        testObserver.assertValueCount(2)
        with(testObserver.values()[1]) {
            assert(saveEnabled)
            assertEquals(
                ValidateLegacyFeeData(
                    null,
                    null,
                    null
                ),
                viewAction
            )
        }
    }

    @Test
    fun `validateLegacyInputs (gasPrice nan) should emit gasPriceError`() {

        val testObserver = TestLiveDataObserver<TxEditFeeState>()

        val minNonce = BigInteger.ZERO
        val nonceValue = "1"
        val gasLimitValue = "1"
        val gasPrice = null

        coEvery { context.getString(any()) } returns ""

        viewModel = TxEditFeeViewModel(appDispatchers)
        viewModel.state.observeForever(testObserver)

        viewModel.validateLegacyInputs(
            context,
            minNonce,
            nonceValue,
            gasLimitValue,
            gasPrice,
        )

        testObserver.assertValueCount(2)
        with(testObserver.values()[1]) {
            assertFalse(saveEnabled)
            assertEquals(
                ValidateLegacyFeeData(
                    null,
                    null,
                    ""
                ),
                viewAction
            )
        }
    }
}
