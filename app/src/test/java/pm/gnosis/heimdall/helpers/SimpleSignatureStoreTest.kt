package pm.gnosis.heimdall.helpers

import android.content.Context
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
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.getTestString
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
        val signaturesObserver = TestObserver<Map<Solidity.Address, Signature>>()
        store.observe().subscribe(signaturesObserver)
        // No signatures exist we should get an empty map
        signaturesObserver.assertValuesOnly(emptyMap())

        // It should not be possible to add a signature if we have no information about the safe
        assertError(SimpleLocalizedException(contextMock.getTestString(R.string.error_signature_not_owner)), {
            store.add(TEST_OWNERS[0] to TEST_SIGNATURE)
        })

        val mappedObserver = TestObserver<Map<Solidity.Address, Signature>>()
        val info = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION, TEST_OWNERS[2], TEST_OWNERS.size, TEST_OWNERS, BigInteger.ZERO, BigInteger.TEN, Wei.ZERO
        )
        // Set store info an observe it
        store.flatMapInfo(TEST_SAFE, info).subscribe(mappedObserver)
        mappedObserver.assertValuesOnly(emptyMap())

        // Values updated still no signatures
        signaturesObserver.assertValuesOnly(
            emptyMap(), // Previous value
            emptyMap()
        )

        // It should not be possible to add a signature if he is not an owner
        assertError(SimpleLocalizedException(contextMock.getTestString(R.string.error_signature_not_owner)), {
            store.add(Solidity.Address(BigInteger.valueOf(8754)) to TEST_SIGNATURE)
        })

        // It should not be possible to add the signature of the sender
        assertError(SimpleLocalizedException(contextMock.getTestString(R.string.error_signature_already_exists)), {
            store.add(TEST_OWNERS[2] to TEST_SIGNATURE)
        })

        store.add(TEST_OWNERS[0] to TEST_SIGNATURE)
        // Signature added, changes should be propagated
        mappedObserver.assertValuesOnly(
            emptyMap(), // Previous value
            mapOf(TEST_OWNERS[0] to TEST_SIGNATURE)
        )
        signaturesObserver.assertValuesOnly(
            emptyMap(), emptyMap(), // Previous values
            mapOf(TEST_OWNERS[0] to TEST_SIGNATURE)
        )

        // It should not be possible to add the same signature again
        assertError(SimpleLocalizedException(contextMock.getTestString(R.string.error_signature_already_exists)), {
            store.add(TEST_OWNERS[0] to TEST_SIGNATURE)
        })

        store.add(TEST_OWNERS[1] to TEST_SIGNATURE)
        // Signature added, changes should be propagated
        mappedObserver.assertValuesOnly(
            // Previous value
            emptyMap(),
            mapOf(TEST_OWNERS[0] to TEST_SIGNATURE),
            // New value
            TEST_SIGNERS.associate { it to TEST_SIGNATURE })
        signaturesObserver.assertValuesOnly(
            // Previous value
            emptyMap(), emptyMap(),
            mapOf(TEST_OWNERS[0] to TEST_SIGNATURE),
            // New value
            TEST_SIGNERS.associate { it to TEST_SIGNATURE })

        /*
         * Checks for load methods
         */
        val loadSignaturesObserver = TestObserver<Map<Solidity.Address, Signature>>()
        store.load().subscribe(loadSignaturesObserver)
        loadSignaturesObserver.assertResult(TEST_SIGNERS.associate { it to TEST_SIGNATURE })

        val loadSigningInfoObserver = TestObserver<Pair<Solidity.Address, SafeTransaction>>()
        store.loadSingingInfo().subscribe(loadSigningInfoObserver)
        loadSigningInfoObserver.assertResult(TEST_SAFE to TEST_TRANSACTION)

        /*
         * Checks for changes in information
         */
        val updateOwnersObserver = TestObserver<Map<Solidity.Address, Signature>>()
        val updateOwnersInfo = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION, TEST_OWNERS[2], TEST_OWNERS_2.size,
            TEST_OWNERS_2, BigInteger.ZERO, BigInteger.TEN, Wei.ZERO
        )
        // Set store info an observe it
        store.flatMapInfo(TEST_SAFE, updateOwnersInfo).subscribe(updateOwnersObserver)
        updateOwnersObserver.dispose()
        // Owners changes, signatures should update (remove old owners signatures)
        mappedObserver.assertValuesOnly(
            // Previous value
            emptyMap(),
            mapOf(TEST_OWNERS[0] to TEST_SIGNATURE),
            TEST_SIGNERS.associate { it to TEST_SIGNATURE },
            // New value
            mapOf(TEST_OWNERS_2[0] to TEST_SIGNATURE)
        )
        signaturesObserver.assertValuesOnly(
            // Previous value
            emptyMap(), emptyMap(),
            mapOf(TEST_OWNERS[0] to TEST_SIGNATURE),
            TEST_SIGNERS.associate { it to TEST_SIGNATURE },
            // New value
            mapOf(TEST_OWNERS_2[0] to TEST_SIGNATURE)
        )
        updateOwnersObserver.assertValuesOnly(mapOf(TEST_OWNERS_2[0] to TEST_SIGNATURE))

        val updateHashObserver = TestObserver<Map<Solidity.Address, Signature>>()
        val updateHashInfo = TransactionExecutionRepository.ExecuteInformation(
            "some_new_hash", TEST_TRANSACTION, TEST_OWNERS[2], TEST_OWNERS_2.size,
            TEST_OWNERS_2, BigInteger.ZERO, BigInteger.TEN, Wei.ZERO
        )
        // Set store info an observe it
        store.flatMapInfo(TEST_SAFE, updateHashInfo).subscribe(updateHashObserver)
        updateHashObserver.dispose()
        // Owners changes, signatures should update (remove old owners signatures)
        mappedObserver.assertValuesOnly(
            // Previous value
            emptyMap(),
            mapOf(TEST_OWNERS[0] to TEST_SIGNATURE),
            TEST_SIGNERS.associate { it to TEST_SIGNATURE },
            mapOf(TEST_OWNERS_2[0] to TEST_SIGNATURE),
            // New value
            emptyMap()
        )
        signaturesObserver.assertValuesOnly(
            // Previous value
            emptyMap(), emptyMap(),
            mapOf(TEST_OWNERS[0] to TEST_SIGNATURE),
            TEST_SIGNERS.associate { it to TEST_SIGNATURE },
            mapOf(TEST_OWNERS_2[0] to TEST_SIGNATURE),
            // New value
            emptyMap()
        )
        updateHashObserver.assertValuesOnly(emptyMap())
        // Was disposed before, should not get any updates
        updateOwnersObserver.assertValuesOnly(mapOf(TEST_OWNERS_2[0] to TEST_SIGNATURE))

        mappedObserver.dispose()
        signaturesObserver.dispose()
    }

    private fun assertError(expected: Throwable, action: (() -> Unit)) {
        try {
            action()
            assertFalse("No error occurred!", false)
        } catch (actual: Throwable) {
            assertEquals(expected, actual)
        }
    }

    companion object {
        private const val TEST_TRANSACTION_HASH = "SomeHash"
        private val TEST_SIGNATURE = Signature(BigInteger.valueOf(987), BigInteger.valueOf(678), 27)
        private val TEST_SAFE = Solidity.Address(BigInteger.ZERO)
        private val TEST_TRANSACTION = SafeTransaction(Transaction(Solidity.Address(BigInteger.ZERO), nonce = BigInteger.TEN), TransactionExecutionRepository.Operation.CALL)
        private val TEST_SIGNERS = listOf(BigInteger.valueOf(7), BigInteger.valueOf(13)).map { Solidity.Address(it) }
        private val TEST_OWNERS = TEST_SIGNERS + Solidity.Address(BigInteger.valueOf(5))
        private val TEST_OWNERS_2 = listOf(BigInteger.valueOf(13), BigInteger.valueOf(23)).map { Solidity.Address(it) }
    }
}
