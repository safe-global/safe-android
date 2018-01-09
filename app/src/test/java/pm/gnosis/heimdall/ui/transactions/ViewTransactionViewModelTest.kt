package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.QrCodeGenerator
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.transactions.ViewTransactionContract.Info
import pm.gnosis.heimdall.utils.GnoSafeUrlParser
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import java.math.BigInteger


@RunWith(MockitoJUnitRunner::class)
class ViewTransactionViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var qrCodeGeneratorMock: QrCodeGenerator

    @Mock
    lateinit var signatureStoreMock: SignatureStore

    @Mock
    lateinit var transactionRepositoryMock: TransactionRepository

    @Mock
    lateinit var transactionDetailsRepositoryMock: TransactionDetailsRepository

    private lateinit var viewModel: ViewTransactionViewModel

    @Before
    fun setUp() {
        contextMock.mockGetString()
        viewModel = ViewTransactionViewModel(contextMock, qrCodeGeneratorMock, signatureStoreMock, transactionRepositoryMock, transactionDetailsRepositoryMock)
    }

    @Test
    fun loadExecuteInfo2OwnersWithSignatures() {
        val info = TransactionRepository.ExecuteInformation(TEST_TRANSACTION_HASH, TEST_TRANSACTION,
                true, 2, TEST_OWNERS)
        given(transactionRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any()))
                .willReturn(Single.just(info))

        given(transactionRepositoryMock.estimateFees(MockUtils.any(), MockUtils.any(), MockUtils.any()))
                .willReturn(Single.just(TEST_TRANSACTION_FEES))

        val signaturesSubject = PublishSubject.create<Map<BigInteger, Signature>>()
        given(signatureStoreMock.flatMapInfo(TEST_SAFE, info)).willReturn(signaturesSubject)

        val testObserver = TestObserver<Result<Info>>()
        viewModel.loadExecuteInfo(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)
        // No signatures emitted, therefore we should not have any information
        testObserver.assertEmpty()
        then(signatureStoreMock).should().flatMapInfo(TEST_SAFE, info)
        then(signatureStoreMock).shouldHaveNoMoreInteractions()
        then(transactionRepositoryMock).should().loadExecuteInformation(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit an empty signature map
        val noSignatures = emptyMap<BigInteger, Signature>()
        signaturesSubject.onNext(noSignatures)
        testObserver.assertValuesOnly(
                DataResult(Info(TEST_SAFE, info, noSignatures)))

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 1 signature
        val singleSignature = mapOf(TEST_OWNERS.first() to TEST_SIGNATURE)
        signaturesSubject.onNext(singleSignature)
        testObserver.assertValuesOnly(
                // Previous values
                DataResult(Info(TEST_SAFE, info, noSignatures)),
                // New values
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                DataResult(Info(TEST_SAFE, info, singleSignature, TEST_TRANSACTION_FEES)))

        then(transactionRepositoryMock).should().estimateFees(TEST_SAFE, TEST_TRANSACTION, singleSignature)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 1 signature again but estimation fails
        val error = IllegalStateException()
        given(transactionRepositoryMock.estimateFees(MockUtils.any(), MockUtils.any(), MockUtils.any()))
                .willReturn(Single.error<GasEstimate>(error))
        signaturesSubject.onNext(singleSignature)
        testObserver.assertValuesOnly(
                // Previous values
                DataResult(Info(TEST_SAFE, info, noSignatures)),
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                DataResult(Info(TEST_SAFE, info, singleSignature, TEST_TRANSACTION_FEES)),
                // New values
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                ErrorResult(error))

        then(transactionRepositoryMock).should(times(2))
                .estimateFees(TEST_SAFE, TEST_TRANSACTION, singleSignature)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // Signature store fails => no signatures
        signaturesSubject.onError(Exception())
        testObserver.assertResult(
                // Previous values
                DataResult(Info(TEST_SAFE, info, noSignatures)),
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                DataResult(Info(TEST_SAFE, info, singleSignature, TEST_TRANSACTION_FEES)),
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                ErrorResult(error),
                // New values
                DataResult(Info(TEST_SAFE, info, noSignatures)))

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadExecuteInfoSingleOwner() {
        val info = TransactionRepository.ExecuteInformation(TEST_TRANSACTION_HASH, TEST_TRANSACTION,
                true, 1, TEST_OWNERS)
        given(transactionRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any()))
                .willReturn(Single.just(info))

        given(transactionRepositoryMock.estimateFees(MockUtils.any(), MockUtils.any(), MockUtils.any()))
                .willReturn(Single.just(TEST_TRANSACTION_FEES))

        val signaturesSubject = PublishSubject.create<Map<BigInteger, Signature>>()
        given(signatureStoreMock.flatMapInfo(TEST_SAFE, info)).willReturn(signaturesSubject)

        val testObserver = TestObserver<Result<Info>>()
        viewModel.loadExecuteInfo(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)
        // No signatures emitted, therefore we should not have any information
        testObserver.assertEmpty()
        then(signatureStoreMock).should().flatMapInfo(TEST_SAFE, info)
        then(signatureStoreMock).shouldHaveNoMoreInteractions()
        then(transactionRepositoryMock).should().loadExecuteInformation(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit an empty signature map, current device is owner and can confirm without signatures
        val noSignatures = emptyMap<BigInteger, Signature>()
        signaturesSubject.onNext(noSignatures)
        testObserver.assertValuesOnly(
                DataResult(Info(TEST_SAFE, info, noSignatures)),
                DataResult(Info(TEST_SAFE, info, noSignatures, TEST_TRANSACTION_FEES)))

        then(transactionRepositoryMock).should().estimateFees(TEST_SAFE, TEST_TRANSACTION, noSignatures)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 1 signature, to many signatures should not be a problem
        val singleSignature = mapOf(TEST_OWNERS.first() to TEST_SIGNATURE)
        signaturesSubject.onNext(singleSignature)
        testObserver.assertValuesOnly(
                // Previous values
                DataResult(Info(TEST_SAFE, info, noSignatures)),
                DataResult(Info(TEST_SAFE, info, noSignatures, TEST_TRANSACTION_FEES)),
                // New values
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                DataResult(Info(TEST_SAFE, info, singleSignature, TEST_TRANSACTION_FEES)))

        then(transactionRepositoryMock).should().estimateFees(TEST_SAFE, TEST_TRANSACTION, singleSignature)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 1 signature again but estimation fails
        val error = IllegalStateException()
        given(transactionRepositoryMock.estimateFees(MockUtils.any(), MockUtils.any(), MockUtils.any()))
                .willReturn(Single.error<GasEstimate>(error))
        signaturesSubject.onNext(singleSignature)
        testObserver.assertValuesOnly(
                // Previous values
                DataResult(Info(TEST_SAFE, info, noSignatures)),
                DataResult(Info(TEST_SAFE, info, noSignatures, TEST_TRANSACTION_FEES)),
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                DataResult(Info(TEST_SAFE, info, singleSignature, TEST_TRANSACTION_FEES)),
                // New values
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                ErrorResult(error))

        then(transactionRepositoryMock).should(times(2))
                .estimateFees(TEST_SAFE, TEST_TRANSACTION, singleSignature)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // Signature store fails => no signatures
        given(transactionRepositoryMock.estimateFees(MockUtils.any(), MockUtils.any(), MockUtils.any()))
                .willReturn(Single.just(TEST_TRANSACTION_FEES))
        signaturesSubject.onError(Exception())
        testObserver.assertResult(
                // Previous values
                DataResult(Info(TEST_SAFE, info, noSignatures)),
                DataResult(Info(TEST_SAFE, info, noSignatures, TEST_TRANSACTION_FEES)),
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                DataResult(Info(TEST_SAFE, info, singleSignature, TEST_TRANSACTION_FEES)),
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                ErrorResult(error),
                // New values
                DataResult(Info(TEST_SAFE, info, noSignatures)),
                DataResult(Info(TEST_SAFE, info, noSignatures, TEST_TRANSACTION_FEES)))

        then(transactionRepositoryMock).should(times(2))
                .estimateFees(TEST_SAFE, TEST_TRANSACTION, noSignatures)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadExecuteInfoSingleOwnerSignatures() {
        val info = TransactionRepository.ExecuteInformation(TEST_TRANSACTION_HASH, TEST_TRANSACTION,
                false, 2, TEST_OWNERS)
        given(transactionRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any()))
                .willReturn(Single.just(info))

        given(transactionRepositoryMock.estimateFees(MockUtils.any(), MockUtils.any(), MockUtils.any()))
                .willReturn(Single.just(TEST_TRANSACTION_FEES))

        val signaturesSubject = PublishSubject.create<Map<BigInteger, Signature>>()
        given(signatureStoreMock.flatMapInfo(TEST_SAFE, info)).willReturn(signaturesSubject)

        val testObserver = TestObserver<Result<Info>>()
        viewModel.loadExecuteInfo(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)
        // No signatures emitted, therefore we should not have any information
        testObserver.assertEmpty()
        then(signatureStoreMock).should().flatMapInfo(TEST_SAFE, info)
        then(signatureStoreMock).shouldHaveNoMoreInteractions()
        then(transactionRepositoryMock).should().loadExecuteInformation(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit an empty signature map
        val noSignatures = emptyMap<BigInteger, Signature>()
        signaturesSubject.onNext(noSignatures)
        testObserver.assertValuesOnly(
                DataResult(Info(TEST_SAFE, info, noSignatures)))

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 1 signature
        val singleSignature = mapOf(TEST_OWNERS.first() to TEST_SIGNATURE)
        signaturesSubject.onNext(singleSignature)
        testObserver.assertValuesOnly(
                // Previous values
                DataResult(Info(TEST_SAFE, info, noSignatures)),
                // New values
                DataResult(Info(TEST_SAFE, info, singleSignature)))
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 2 signature
        val twoSignatures = mapOf(TEST_OWNERS.first() to TEST_SIGNATURE, TEST_OWNERS.last() to TEST_SIGNATURE)
        signaturesSubject.onNext(twoSignatures)
        testObserver.assertValuesOnly(
                // Previous values
                DataResult(Info(TEST_SAFE, info, noSignatures)),
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                // New values
                DataResult(Info(TEST_SAFE, info, twoSignatures)),
                DataResult(Info(TEST_SAFE, info, twoSignatures, TEST_TRANSACTION_FEES)))

        then(transactionRepositoryMock).should().estimateFees(TEST_SAFE, TEST_TRANSACTION, twoSignatures)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 2 signature again but estimation fails
        val error = IllegalStateException()
        given(transactionRepositoryMock.estimateFees(MockUtils.any(), MockUtils.any(), MockUtils.any()))
                .willReturn(Single.error<GasEstimate>(error))
        signaturesSubject.onNext(twoSignatures)
        testObserver.assertValuesOnly(
                // Previous values
                DataResult(Info(TEST_SAFE, info, noSignatures)),
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                DataResult(Info(TEST_SAFE, info, twoSignatures)),
                DataResult(Info(TEST_SAFE, info, twoSignatures, TEST_TRANSACTION_FEES)),
                // New values
                DataResult(Info(TEST_SAFE, info, twoSignatures)),
                ErrorResult(error))

        then(transactionRepositoryMock).should(times(2))
                .estimateFees(TEST_SAFE, TEST_TRANSACTION, twoSignatures)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // Signature store fails => no signatures
        signaturesSubject.onError(Exception())
        testObserver.assertResult(
                // Previous values
                DataResult(Info(TEST_SAFE, info, noSignatures)),
                DataResult(Info(TEST_SAFE, info, singleSignature)),
                DataResult(Info(TEST_SAFE, info, twoSignatures)),
                DataResult(Info(TEST_SAFE, info, twoSignatures, TEST_TRANSACTION_FEES)),
                DataResult(Info(TEST_SAFE, info, twoSignatures)),
                ErrorResult(error),
                // New values
                DataResult(Info(TEST_SAFE, info, noSignatures)))

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadExecuteInfoError() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.loadExecuteInformation(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.error(error))

        val testObserver = TestObserver<Result<Info>>()
        viewModel.loadExecuteInfo(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        testObserver.assertNoErrors()
                .assertValue(ErrorResult(error))
                .assertComplete()

        then(transactionRepositoryMock).should().loadExecuteInformation(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()
    }

    private fun testSubmitTransactionWithGas(info: TransactionRepository.ExecuteInformation, submitError: Throwable?,
                                             expectingSubmit: Boolean, gasOverride: Wei?,
                                             signatures: Map<BigInteger, Signature>?,
                                             expectedResult: Result<BigInteger>) {

        given(transactionRepositoryMock.loadExecuteInformation(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.just(info))

        val signatureSingle = signatures?.let { Single.just(it) } ?: Single.error(TestException())
        given(signatureStoreMock.loadSignatures()).willReturn(signatureSingle)

        if (expectingSubmit) {
            val submitReturn = submitError?.let { Completable.error(it) } ?: Completable.complete()
            given(transactionRepositoryMock.submit(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(submitReturn)
        }

        val testObserverDefaultGas = TestObserver<Result<BigInteger>>()
        viewModel.submitTransaction(TEST_SAFE, TEST_TRANSACTION, gasOverride).subscribe(testObserverDefaultGas)

        testObserverDefaultGas.assertResult(expectedResult)

        then(transactionRepositoryMock).should().loadExecuteInformation(TEST_SAFE, TEST_TRANSACTION)
        if (expectingSubmit) {
            then(transactionRepositoryMock).should().submit(TEST_SAFE, TEST_TRANSACTION, signatures!!, gasOverride)
        }
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        BDDMockito.reset(transactionRepositoryMock)
    }

    private fun testSubmitTransaction(info: TransactionRepository.ExecuteInformation, submitError: Throwable?,
                                      expectingSubmit: Boolean, signatures: Map<BigInteger, Signature>?,
                                      expectedResult: Result<BigInteger>) {
        testSubmitTransactionWithGas(info, submitError, expectingSubmit, null, signatures, expectedResult)
        testSubmitTransactionWithGas(info, submitError, expectingSubmit, TEST_GAS_OVERRIDE, signatures, expectedResult)
    }

    @Test
    fun submitTransaction() {

        // Test execute
        val execute = TransactionRepository.ExecuteInformation(TEST_TRANSACTION_HASH, TEST_TRANSACTION,
                true, 1, TEST_OWNERS)
        testSubmitTransaction(execute, null, true, emptyMap(), DataResult(TEST_SAFE))

        // Test not owner and no signatures
        val notOwner = TransactionRepository.ExecuteInformation(TEST_TRANSACTION_HASH, TEST_TRANSACTION,
                false, 1, TEST_OWNERS)
        testSubmitTransaction(notOwner, null,
                false, emptyMap(), ErrorResult(SimpleLocalizedException(R.string.error_not_enough_confirmations.toString())))

        // Test not owner but has signatures
        val twoSignatures = mapOf(TEST_OWNERS.first() to TEST_SIGNATURE, TEST_OWNERS.last() to TEST_SIGNATURE)
        val notOwnerSignatures = TransactionRepository.ExecuteInformation(TEST_TRANSACTION_HASH, TEST_TRANSACTION,
                false, 2, TEST_OWNERS)
        testSubmitTransaction(notOwnerSignatures, null,
                true, twoSignatures, DataResult(TEST_SAFE))

        // Test owner but missing signatures
        val ownerMultipleConfirms = TransactionRepository.ExecuteInformation(TEST_TRANSACTION_HASH, TEST_TRANSACTION,
                true, 2, TEST_OWNERS)
        testSubmitTransaction(ownerMultipleConfirms, null,
                false, emptyMap(), ErrorResult(SimpleLocalizedException(R.string.error_not_enough_confirmations.toString())))

        // Test owner but missing signatures
        testSubmitTransaction(execute, null,
                false, null, ErrorResult(TestException()))

        // Test error submitting transaction
        val error = IllegalStateException()
        testSubmitTransaction(execute, error, true, emptyMap(), ErrorResult(error))
    }

    @Test
    fun submitTransactionLoadInfoError() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.loadExecuteInformation(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.error(error))

        val testObserver = TestObserver<Result<BigInteger>>()
        viewModel.submitTransaction(TEST_SAFE, TEST_TRANSACTION, null).subscribe(testObserver)

        testObserver.assertNoErrors()
                .assertValue(ErrorResult(error))
                .assertComplete()

        then(transactionRepositoryMock).should().loadExecuteInformation(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkTransactionType() {
        given(transactionDetailsRepositoryMock.loadTransactionType(MockUtils.any()))
                .willReturn(Single.just(TransactionType.GENERIC))

        val testObserver = TestObserver<TransactionType>()
        val transaction = Transaction(BigInteger.ZERO)
        viewModel.checkTransactionType(transaction).subscribe(testObserver)
        testObserver.assertNoErrors().assertValue(TransactionType.GENERIC).assertComplete()
        then(transactionDetailsRepositoryMock).should().loadTransactionType(transaction)
        then(transactionDetailsRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkTransactionTypeError() {
        val error = IllegalStateException()
        given(transactionDetailsRepositoryMock.loadTransactionType(MockUtils.any()))
                .willReturn(Single.error(error))

        val testObserver = TestObserver<TransactionType>()
        val transaction = Transaction(BigInteger.ZERO)
        viewModel.checkTransactionType(transaction).subscribe(testObserver)
        testObserver.assertError(error).assertNoValues().assertTerminated()
        then(transactionDetailsRepositoryMock).should().loadTransactionType(transaction)
        then(transactionDetailsRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun addSignature() {
        val sigInfo = TEST_OWNER to TEST_SIGNATURE
        given(signatureStoreMock.loadSingingInfo()).willReturn(Single.just(TEST_SAFE to TEST_TRANSACTION))
        given(transactionRepositoryMock.checkSignature(MockUtils.any(), MockUtils.any(), MockUtils.any()))
                .willReturn(Single.just(sigInfo))

        val testObserver = TestObserver<Unit>()
        viewModel.addSignature(GnoSafeUrlParser.signResponse(TEST_SIGNATURE)).subscribe(testObserver)

        then(signatureStoreMock).should().addSignature(sigInfo)
        then(signatureStoreMock).should().loadSingingInfo()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().checkSignature(TEST_SAFE, TEST_TRANSACTION, TEST_SIGNATURE)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertComplete()
    }

    @Test
    fun addSignatureInvalid() {
        given(signatureStoreMock.loadSingingInfo()).willReturn(Single.just(TEST_SAFE to TEST_TRANSACTION))

        val testObserver = TestObserver<Unit>()
        viewModel.addSignature("").subscribe(testObserver)

        then(signatureStoreMock).should().loadSingingInfo()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(SimpleLocalizedException(R.string.invalid_signature_uri.toString()))
    }

    @Test
    fun singTransaction() {
        given(transactionRepositoryMock.sign(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.just(TEST_SIGNATURE))
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt(), anyInt())).willReturn(Single.just(TEST_BITMAP))

        val testObserver = TestObserver<Result<Pair<String, Bitmap>>>()
        viewModel.signTransaction(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().sign(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(qrCodeGeneratorMock).should().generateQrCode(GnoSafeUrlParser.signResponse(TEST_SIGNATURE))
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(DataResult(TEST_SIGNATURE.toString() to TEST_BITMAP))
    }

    @Test
    fun singTransactionFailSigning() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.sign(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.error(error))

        val testObserver = TestObserver<Result<Pair<String, Bitmap>>>()
        viewModel.signTransaction(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().sign(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(ErrorResult(error))
    }

    @Test
    fun singTransactionFailQRCodeGeneration() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.sign(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.just(TEST_SIGNATURE))
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt(), anyInt())).willReturn(Single.error(error))

        val testObserver = TestObserver<Result<Pair<String, Bitmap>>>()
        viewModel.signTransaction(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().sign(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(qrCodeGeneratorMock).should().generateQrCode(GnoSafeUrlParser.signResponse(TEST_SIGNATURE))
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(ErrorResult(error))
    }

    private data class TestException(val name: String = "test_exception"): Exception()

    companion object {
        private val TEST_BITMAP = mock(Bitmap::class.java)
        private val TEST_SIGNATURE = Signature(BigInteger.valueOf(987), BigInteger.valueOf(678), 27)
        private val TEST_SAFE = BigInteger.ZERO
        private val TEST_TRANSACTION_HASH = "SomeHash"
        private val TEST_TRANSACTION = Transaction(BigInteger.ZERO, nonce = BigInteger.TEN)
        private val TEST_OWNER = BigInteger.valueOf(5674)
        private val TEST_OWNERS = listOf(TEST_OWNER, BigInteger.valueOf(13))
        private val TEST_TRANSACTION_FEES = GasEstimate(BigInteger.valueOf(1337), Wei(BigInteger.valueOf(23)))
        private val TEST_GAS_OVERRIDE = Wei(BigInteger.valueOf(7331))
    }

}
