package pm.gnosis.heimdall.helpers

import android.content.Context
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.models.Transaction
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.mockGetStringWithArgs
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SimpleSignatureStoreTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    private lateinit var store: SimpleSignatureStore

    @Before
    fun setUp() {
        contextMock.mockGetStringWithArgs()
        store = SimpleSignatureStore(contextMock)
    }

    @Test
    fun completeFlow() {
        val signaturesObserver = TestObserver<Map<BigInteger, Signature>>()
        Observable.create(store).subscribe(signaturesObserver)
        // No signatures exist we should get an empty map
        signaturesObserver.assertValuesOnly(emptyMap())

        // It should not be possible to add a signature if we have no information about the safe
        assertError(SimpleLocalizedException(R.string.error_signature_not_owner.asString()), {
            store.addSignature(TEST_OWNERS[0] to TEST_SIGNATURE)
        })

        val mappedObserver = TestObserver<Map<BigInteger, Signature>>()
        val info = TransactionRepository.ExecuteInformation(
                TEST_TRANSACTION_HASH, TEST_TRANSACTION, false, 2, TEST_OWNERS
        )
        // Set store info an observe it
        store.flatMapInfo(TEST_SAFE, info).subscribe(mappedObserver)
        mappedObserver.assertValuesOnly(emptyMap())

        // Values updated still no signatures
        signaturesObserver.assertValuesOnly(
                emptyMap(), // Previous value
                emptyMap())

        // It should not be possible to add a signature if he is not an owner
        assertError(SimpleLocalizedException(R.string.error_signature_not_owner.asString()), {
            store.addSignature(BigInteger.valueOf(8754) to TEST_SIGNATURE)
        })

        store.addSignature(TEST_OWNERS[0] to TEST_SIGNATURE)
        // Signature added, changes should be propagated
        mappedObserver.assertValuesOnly(
                emptyMap(), // Previous value
                mapOf(TEST_OWNERS[0] to TEST_SIGNATURE))
        signaturesObserver.assertValuesOnly(
                emptyMap(), emptyMap(), // Previous values
                mapOf(TEST_OWNERS[0] to TEST_SIGNATURE))

        // It should not be possible to add the same signature again
        assertError(SimpleLocalizedException(R.string.error_signature_already_exists.asString()), {
            store.addSignature(TEST_OWNERS[0] to TEST_SIGNATURE)
        })

        store.addSignature(TEST_OWNERS[1] to TEST_SIGNATURE)
        // Signature added, changes should be propagated
        mappedObserver.assertValuesOnly(
                // Previous value
                emptyMap(),
                mapOf(TEST_OWNERS[0] to TEST_SIGNATURE),
                // New value
                TEST_OWNERS.associate { it to TEST_SIGNATURE })
        signaturesObserver.assertValuesOnly(
                // Previous value
                emptyMap(), emptyMap(),
                mapOf(TEST_OWNERS[0] to TEST_SIGNATURE),
                // New value
                TEST_OWNERS.associate { it to TEST_SIGNATURE })

        /*
         * Checks for load methods
         */
        val loadSignaturesObserver = TestObserver<Map<BigInteger, Signature>>()
        store.loadSignatures().subscribe(loadSignaturesObserver)
        loadSignaturesObserver.assertResult(TEST_OWNERS.associate { it to TEST_SIGNATURE })

        val loadSigningInfoObserver = TestObserver<Pair<BigInteger, Transaction>>()
        store.loadSingingInfo().subscribe(loadSigningInfoObserver)
        loadSigningInfoObserver.assertResult(TEST_SAFE to TEST_TRANSACTION)

        /*
         * Checks for changes in information
         */
        val updateOwnersObserver = TestObserver<Map<BigInteger, Signature>>()
        val updateOwnersInfo = TransactionRepository.ExecuteInformation(
                TEST_TRANSACTION_HASH, TEST_TRANSACTION, false, 2,
                TEST_OWNERS_2
        )
        // Set store info an observe it
        store.flatMapInfo(TEST_SAFE, updateOwnersInfo).subscribe(updateOwnersObserver)
        updateOwnersObserver.dispose()
        // Owners changes, signatures should update (remove old owners signatures)
        mappedObserver.assertValuesOnly(
                // Previous value
                emptyMap(),
                mapOf(TEST_OWNERS[0] to TEST_SIGNATURE),
                TEST_OWNERS.associate { it to TEST_SIGNATURE },
                // New value
                mapOf(TEST_OWNERS_2[0] to TEST_SIGNATURE))
        signaturesObserver.assertValuesOnly(
                // Previous value
                emptyMap(), emptyMap(),
                mapOf(TEST_OWNERS[0] to TEST_SIGNATURE),
                TEST_OWNERS.associate { it to TEST_SIGNATURE },
                // New value
                mapOf(TEST_OWNERS_2[0] to TEST_SIGNATURE))
        updateOwnersObserver.assertValuesOnly(mapOf(TEST_OWNERS_2[0] to TEST_SIGNATURE))

        val updateHashObserver = TestObserver<Map<BigInteger, Signature>>()
        val updateHashInfo = TransactionRepository.ExecuteInformation(
                "some_new_hash", TEST_TRANSACTION, false, 2,
                TEST_OWNERS_2
        )
        // Set store info an observe it
        store.flatMapInfo(TEST_SAFE, updateHashInfo).subscribe(updateHashObserver)
        updateHashObserver.dispose()
        // Owners changes, signatures should update (remove old owners signatures)
        mappedObserver.assertValuesOnly(
                // Previous value
                emptyMap(),
                mapOf(TEST_OWNERS[0] to TEST_SIGNATURE),
                TEST_OWNERS.associate { it to TEST_SIGNATURE },
                mapOf(TEST_OWNERS_2[0] to TEST_SIGNATURE),
                // New value
                emptyMap())
        signaturesObserver.assertValuesOnly(
                // Previous value
                emptyMap(), emptyMap(),
                mapOf(TEST_OWNERS[0] to TEST_SIGNATURE),
                TEST_OWNERS.associate { it to TEST_SIGNATURE },
                mapOf(TEST_OWNERS_2[0] to TEST_SIGNATURE),
                // New value
                emptyMap())
        updateHashObserver.assertValuesOnly(emptyMap())
        // Was disposed before, should not get any updates
        updateOwnersObserver.assertValuesOnly(mapOf(TEST_OWNERS_2[0] to TEST_SIGNATURE))

        mappedObserver.dispose()
        signaturesObserver.dispose()
    }

    private fun Int.asString(vararg params: Any) =
            contextMock.getString(this, params)

    private fun assertError(expected: Throwable, action: (() -> Unit)) {
        try {
            action()
            assertFalse("No error occurred!", false)
        } catch (actual: Throwable) {
            assertEquals(expected, actual)
        }
    }

    companion object {
        private val TEST_SIGNATURE = Signature(BigInteger.valueOf(987), BigInteger.valueOf(678), 27)
        private val TEST_SAFE = BigInteger.ZERO
        private val TEST_TRANSACTION_HASH = "SomeHash"
        private val TEST_TRANSACTION = Transaction(BigInteger.ZERO, nonce = BigInteger.TEN)
        private val TEST_OWNERS = listOf(BigInteger.valueOf(7), BigInteger.valueOf(13))
        private val TEST_OWNERS_2 = listOf(BigInteger.valueOf(13), BigInteger.valueOf(23))
    }
}