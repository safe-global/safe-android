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
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.transactions.ViewTransactionContract.Info
import pm.gnosis.heimdall.utils.GnoSafeUrlParser
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.Result
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
    lateinit var signaturePushRepositoryMock: SignaturePushRepository

    @Mock
    lateinit var signatureStoreMock: SignatureStore

    @Mock
    lateinit var transactionRepositoryMock: TransactionRepository

    @Mock
    lateinit var transactionDetailsRepositoryMock: TransactionDetailsRepository

    @Mock
    lateinit var txExecutorRepositoryMock: TxExecutorRepository

    private lateinit var viewModel: ViewTransactionViewModel

    @Before
    fun setUp() {
        contextMock.mockGetString()
        viewModel = ViewTransactionViewModel(
            contextMock,
            qrCodeGeneratorMock,
            signaturePushRepositoryMock,
            signatureStoreMock,
            transactionRepositoryMock,
            transactionDetailsRepositoryMock,
            txExecutorRepositoryMock
        )
    }

    @Test
    fun loadExecuteInfo2OwnersWithSignatures() {
        val info = TransactionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION,
            TEST_OWNER, 2, TEST_OWNERS
        )
        given(transactionRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(info))

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
            DataResult(Info(TEST_SAFE, info, noSignatures))
        )

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 1 signature
        val singleSignature = mapOf(TEST_OWNERS.first() to TEST_SIGNATURE)
        signaturesSubject.onNext(singleSignature)
        testObserver.assertValuesOnly(
            // Previous values
            DataResult(Info(TEST_SAFE, info, noSignatures)),
            // New values
            DataResult(Info(TEST_SAFE, info, singleSignature))
        )

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 1 signature again but estimation fails
        signaturesSubject.onNext(singleSignature)
        testObserver.assertValuesOnly(
            // Previous values
            DataResult(Info(TEST_SAFE, info, noSignatures)),
            DataResult(Info(TEST_SAFE, info, singleSignature)),
            // New values
            DataResult(Info(TEST_SAFE, info, singleSignature))
        )

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // Signature store fails => no signatures
        signaturesSubject.onError(Exception())
        testObserver.assertResult(
            // Previous values
            DataResult(Info(TEST_SAFE, info, noSignatures)),
            DataResult(Info(TEST_SAFE, info, singleSignature)),
            DataResult(Info(TEST_SAFE, info, singleSignature)),
            // New values
            DataResult(Info(TEST_SAFE, info, noSignatures))
        )

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadExecuteInfoSingleOwner() {
        val info = TransactionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION,
            TEST_OWNER, 1, TEST_OWNERS
        )
        given(transactionRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(info))

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
            DataResult(Info(TEST_SAFE, info, noSignatures))
        )

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 1 signature, to many signatures should not be a problem
        val singleSignature = mapOf(TEST_OWNERS.first() to TEST_SIGNATURE)
        signaturesSubject.onNext(singleSignature)
        testObserver.assertValuesOnly(
            // Previous values
            DataResult(Info(TEST_SAFE, info, noSignatures)),
            // New values
            DataResult(Info(TEST_SAFE, info, singleSignature))
        )

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 1 signature again
        signaturesSubject.onNext(singleSignature)
        testObserver.assertValuesOnly(
            // Previous values
            DataResult(Info(TEST_SAFE, info, noSignatures)),
            DataResult(Info(TEST_SAFE, info, singleSignature)),
            // New values
            DataResult(Info(TEST_SAFE, info, singleSignature))
        )

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // Signature store fails => no signatures
        signaturesSubject.onError(Exception())
        testObserver.assertResult(
            // Previous values
            DataResult(Info(TEST_SAFE, info, noSignatures)),
            DataResult(Info(TEST_SAFE, info, singleSignature)),
            DataResult(Info(TEST_SAFE, info, singleSignature)),
            // New values
            DataResult(Info(TEST_SAFE, info, noSignatures))
        )

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadExecuteInfoSingleOwnerSignatures() {
        val info = TransactionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION,
            TEST_NOT_OWNER, 2, TEST_OWNERS
        )
        given(transactionRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(info))

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
            DataResult(Info(TEST_SAFE, info, noSignatures))
        )

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 1 signature
        val singleSignature = mapOf(TEST_OWNERS.first() to TEST_SIGNATURE)
        signaturesSubject.onNext(singleSignature)
        testObserver.assertValuesOnly(
            // Previous values
            DataResult(Info(TEST_SAFE, info, noSignatures)),
            // New values
            DataResult(Info(TEST_SAFE, info, singleSignature))
        )
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 2 signatures
        val twoSignatures = mapOf(TEST_OWNERS.first() to TEST_SIGNATURE, TEST_OWNERS.last() to TEST_SIGNATURE)
        signaturesSubject.onNext(twoSignatures)
        testObserver.assertValuesOnly(
            // Previous values
            DataResult(Info(TEST_SAFE, info, noSignatures)),
            DataResult(Info(TEST_SAFE, info, singleSignature)),
            // New values
            DataResult(Info(TEST_SAFE, info, twoSignatures))
        )

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // We emit 2 signatures again
        signaturesSubject.onNext(twoSignatures)
        testObserver.assertValuesOnly(
            // Previous values
            DataResult(Info(TEST_SAFE, info, noSignatures)),
            DataResult(Info(TEST_SAFE, info, singleSignature)),
            DataResult(Info(TEST_SAFE, info, twoSignatures)),
            // New values
            DataResult(Info(TEST_SAFE, info, twoSignatures))
        )

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // Signature store fails => no signatures
        signaturesSubject.onError(Exception())
        testObserver.assertResult(
            // Previous values
            DataResult(Info(TEST_SAFE, info, noSignatures)),
            DataResult(Info(TEST_SAFE, info, singleSignature)),
            DataResult(Info(TEST_SAFE, info, twoSignatures)),
            DataResult(Info(TEST_SAFE, info, twoSignatures)),
            // New values
            DataResult(Info(TEST_SAFE, info, noSignatures))
        )

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

    private fun testSubmitTransaction(
        info: TransactionRepository.ExecuteInformation, submitError: Throwable?,
        expectingSubmit: Boolean,
        signatures: Map<BigInteger, Signature>?,
        expectedResult: Result<BigInteger>
    ) {

        given(transactionRepositoryMock.loadExecuteInformation(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.just(info))

        val signatureSingle = signatures?.let { Single.just(it) } ?: Single.error(TestException())
        given(signatureStoreMock.load()).willReturn(signatureSingle)

        if (expectingSubmit) {
            val submitReturn = submitError?.let { Completable.error(it) } ?: Completable.complete()
            given(transactionRepositoryMock.submit(MockUtils.any(), MockUtils.any(), MockUtils.any(), anyBoolean())).willReturn(
                submitReturn
            )
        }

        val testObserverDefaultGas = TestObserver<Result<BigInteger>>()
        viewModel.submitTransaction(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserverDefaultGas)

        testObserverDefaultGas.assertResult(expectedResult)

        then(transactionRepositoryMock).should().loadExecuteInformation(TEST_SAFE, TEST_TRANSACTION)
        if (expectingSubmit) {
            then(transactionRepositoryMock).should().submit(TEST_SAFE, TEST_TRANSACTION, signatures!!, info.isOwner)
        }
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        BDDMockito.reset(transactionRepositoryMock)
    }

    @Test
    fun submitTransaction() {
        // Test execute
        val execute = TransactionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION,
            TEST_OWNER, 1, TEST_OWNERS
        )
        testSubmitTransaction(execute, null, true, emptyMap(), DataResult(TEST_SAFE))

        // Test not owner and no signatures
        val notOwner = TransactionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION,
            TEST_NOT_OWNER, 1, TEST_OWNERS
        )
        testSubmitTransaction(
            notOwner, null,
            false, emptyMap(), ErrorResult(SimpleLocalizedException(R.string.error_not_enough_confirmations.toString()))
        )

        // Test not owner but has signatures
        val twoSignatures = mapOf(TEST_OWNERS.first() to TEST_SIGNATURE, TEST_OWNERS.last() to TEST_SIGNATURE)
        val notOwnerSignatures = TransactionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION,
            TEST_NOT_OWNER, 2, TEST_OWNERS
        )
        testSubmitTransaction(
            notOwnerSignatures, null,
            true, twoSignatures, DataResult(TEST_SAFE)
        )

        // Test owner but missing signatures
        val ownerMultipleConfirms = TransactionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION,
            TEST_OWNER, 2, TEST_OWNERS
        )
        testSubmitTransaction(
            ownerMultipleConfirms, null,
            false, emptyMap(), ErrorResult(SimpleLocalizedException(R.string.error_not_enough_confirmations.toString()))
        )

        // Test owner but missing signatures
        testSubmitTransaction(
            execute, null,
            false, null, ErrorResult(TestException())
        )

        // Test error submitting transaction
        val error = IllegalStateException()
        testSubmitTransaction(execute, error, true, emptyMap(), ErrorResult(error))
    }

    @Test
    fun submitTransactionLoadInfoError() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.loadExecuteInformation(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.error(error))

        val testObserver = TestObserver<Result<BigInteger>>()
        viewModel.submitTransaction(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

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
    fun observeSignature() {
        val signatureSubject = PublishSubject.create<Signature>()
        given(signaturePushRepositoryMock.observe(TEST_SAFE)).willReturn(signatureSubject)
        val sigInfo = TEST_OWNER to TEST_SIGNATURE
        given(transactionRepositoryMock.checkSignature(MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(sigInfo))

        val testObserver = TestObserver<Result<Unit>>()
        viewModel.observeSignaturePushes(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        then(signaturePushRepositoryMock).should().observe(TEST_SAFE)
        then(signaturePushRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertEmpty()

        // Added new signature successfully
        signatureSubject.onNext(TEST_SIGNATURE)

        testObserver.assertValuesOnly(DataResult(Unit))

        then(signatureStoreMock).should().add(sigInfo)
        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().checkSignature(TEST_SAFE, TEST_TRANSACTION, TEST_SIGNATURE)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // Failure adding to storage
        given(signatureStoreMock.add(MockUtils.any()))
            .will { throw SimpleLocalizedException(R.string.error_owner_already_added.toString()) }
        signatureSubject.onNext(TEST_SIGNATURE)

        testObserver.assertValuesOnly(
            DataResult(Unit),
            ErrorResult(SimpleLocalizedException(R.string.error_owner_already_added.toString()))
        ) // New value

        then(signatureStoreMock).should(times(2)).add(sigInfo)
        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should(times(2)).checkSignature(TEST_SAFE, TEST_TRANSACTION, TEST_SIGNATURE)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // Failure verifying signature
        given(transactionRepositoryMock.checkSignature(MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.error(IllegalArgumentException()))
        signatureSubject.onNext(TEST_SIGNATURE)

        testObserver.assertValuesOnly(
            DataResult(Unit),
            ErrorResult(SimpleLocalizedException(R.string.error_owner_already_added.toString())),
            ErrorResult(SimpleLocalizedException(R.string.invalid_signature.toString()))
        ) // New value

        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should(times(3)).checkSignature(TEST_SAFE, TEST_TRANSACTION, TEST_SIGNATURE)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        // Failure from upstream
        signatureSubject.onError(IllegalStateException())

        testObserver.assertFailure(
            IllegalStateException::class.java,
            DataResult(Unit),
            ErrorResult(SimpleLocalizedException(R.string.error_owner_already_added.toString())),
            ErrorResult(SimpleLocalizedException(R.string.invalid_signature.toString()))
        )

        then(signatureStoreMock).shouldHaveNoMoreInteractions()
        then(signaturePushRepositoryMock).shouldHaveNoMoreInteractions()
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun addSignature() {
        val sigInfo = TEST_OWNER to TEST_SIGNATURE
        given(signatureStoreMock.loadSingingInfo()).willReturn(Single.just(TEST_SAFE to TEST_TRANSACTION))
        given(transactionRepositoryMock.checkSignature(MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(sigInfo))

        val testObserver = TestObserver<Unit>()
        viewModel.addSignature(GnoSafeUrlParser.signResponse(TEST_SIGNATURE)).subscribe(testObserver)

        then(signatureStoreMock).should().add(sigInfo)
        then(signatureStoreMock).should().loadSingingInfo()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().checkSignature(TEST_SAFE, TEST_TRANSACTION, TEST_SIGNATURE)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertComplete()
    }

    @Test
    fun addSignatureInvalidUrl() {
        given(signatureStoreMock.loadSingingInfo()).willReturn(Single.just(TEST_SAFE to TEST_TRANSACTION))

        val testObserver = TestObserver<Unit>()
        viewModel.addSignature("").subscribe(testObserver)

        then(signatureStoreMock).should().loadSingingInfo()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(SimpleLocalizedException(R.string.invalid_signature_uri.toString()))
    }

    @Test
    fun addSignatureInvalidSignature() {
        given(signatureStoreMock.loadSingingInfo()).willReturn(Single.just(TEST_SAFE to TEST_TRANSACTION))
        given(transactionRepositoryMock.checkSignature(MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.error(Exception()))

        val testObserver = TestObserver<Unit>()
        viewModel.addSignature(GnoSafeUrlParser.signResponse(TEST_SIGNATURE)).subscribe(testObserver)

        then(signatureStoreMock).should().loadSingingInfo()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().checkSignature(TEST_SAFE, TEST_TRANSACTION, TEST_SIGNATURE)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(SimpleLocalizedException(R.string.invalid_signature.toString()))
    }

    @Test
    fun addSignatureNoSigningInfo() {
        val error = IllegalStateException()
        given(signatureStoreMock.loadSingingInfo()).willReturn(Single.error(error))

        val testObserver = TestObserver<Unit>()
        viewModel.addSignature(GnoSafeUrlParser.signResponse(TEST_SIGNATURE)).subscribe(testObserver)

        then(signatureStoreMock).should().loadSingingInfo()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(error)
    }

    @Test
    fun addSignatureStoreFailure() {
        given(signatureStoreMock.add(MockUtils.any()))
            .will { throw SimpleLocalizedException(R.string.error_owner_already_added.toString()) }
        val sigInfo = TEST_OWNER to TEST_SIGNATURE
        given(signatureStoreMock.loadSingingInfo()).willReturn(Single.just(TEST_SAFE to TEST_TRANSACTION))
        given(transactionRepositoryMock.checkSignature(MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(sigInfo))

        val testObserver = TestObserver<Unit>()
        viewModel.addSignature(GnoSafeUrlParser.signResponse(TEST_SIGNATURE)).subscribe(testObserver)

        then(signatureStoreMock).should().add(sigInfo)
        then(signatureStoreMock).should().loadSingingInfo()
        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().checkSignature(TEST_SAFE, TEST_TRANSACTION, TEST_SIGNATURE)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(SimpleLocalizedException(R.string.error_owner_already_added.toString()))
    }

    @Test
    fun signTransactionFailSigning() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.sign(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.error(error))

        val testObserver = TestObserver<Result<Pair<String, Bitmap?>>>()
        viewModel.signTransaction(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().sign(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()

        then(signaturePushRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(ErrorResult(error))
    }

    @Test
    fun signTransactionBitmap() {
        given(transactionRepositoryMock.sign(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.just(TEST_SIGNATURE))
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt(), anyInt())).willReturn(Single.just(TEST_BITMAP))

        val testObserver = TestObserver<Result<Pair<String, Bitmap?>>>()
        viewModel.signTransaction(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().sign(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(qrCodeGeneratorMock).should().generateQrCode(GnoSafeUrlParser.signResponse(TEST_SIGNATURE))
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()

        then(signaturePushRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(DataResult(TEST_SIGNATURE.toString() to TEST_BITMAP))
    }

    @Test
    fun signTransactionBitmapFailQRCodeGeneration() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.sign(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.just(TEST_SIGNATURE))
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt(), anyInt())).willReturn(Single.error(error))

        val testObserver = TestObserver<Result<Pair<String, Bitmap?>>>()
        viewModel.signTransaction(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().sign(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(qrCodeGeneratorMock).should().generateQrCode(GnoSafeUrlParser.signResponse(TEST_SIGNATURE))
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()

        then(signaturePushRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(ErrorResult(error))
    }

    @Test
    fun signTransactionPush() {
        given(transactionRepositoryMock.sign(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.just(TEST_SIGNATURE))
        given(signaturePushRepositoryMock.send(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())

        val testObserver = TestObserver<Result<Pair<String, Bitmap?>>>()
        viewModel.signTransaction(TEST_SAFE, TEST_TRANSACTION, true).subscribe(testObserver)

        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().sign(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()

        then(signaturePushRepositoryMock).should().send(TEST_SAFE, TEST_TRANSACTION, TEST_SIGNATURE)
        then(signaturePushRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(DataResult(TEST_SIGNATURE.toString() to null))
    }

    @Test
    fun signTransactionPushFailQRCodeGeneration() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.sign(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.just(TEST_SIGNATURE))
        given(signaturePushRepositoryMock.send(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(Completable.error(error))

        val testObserver = TestObserver<Result<Pair<String, Bitmap?>>>()
        viewModel.signTransaction(TEST_SAFE, TEST_TRANSACTION, true).subscribe(testObserver)

        then(signatureStoreMock).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().sign(TEST_SAFE, TEST_TRANSACTION)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()

        then(signaturePushRepositoryMock).should().send(TEST_SAFE, TEST_TRANSACTION, TEST_SIGNATURE)
        then(signaturePushRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(ErrorResult(error))
    }

    @Test
    fun sendSignature() {
        val executeInformation = TransactionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION,
            TEST_OWNER, 2, TEST_OWNERS
        )
        val info = Info(TEST_SAFE, executeInformation, emptyMap())
        given(signaturePushRepositoryMock.request(MockUtils.any(), anyString())).willReturn(Completable.complete())

        val testObserver = TestObserver<Result<Unit>>()
        viewModel.sendSignaturePush(info).subscribe(testObserver)
        testObserver.assertResult(DataResult(Unit))

        val data = GnoSafeUrlParser.signRequest(
            TEST_TRANSACTION_HASH,
            info.selectedSafe,
            TEST_TRANSACTION.address,
            TEST_TRANSACTION.value,
            TEST_TRANSACTION.data,
            TEST_TRANSACTION.nonce!!
        )
        then(signaturePushRepositoryMock).should().request(TEST_SAFE, data)
    }

    @Test
    fun sendSignatureError() {
        val executeInformation = TransactionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION,
            TEST_OWNER, 2, TEST_OWNERS
        )
        val info = Info(TEST_SAFE, executeInformation, emptyMap())
        val error = IllegalStateException()
        given(signaturePushRepositoryMock.request(MockUtils.any(), anyString())).willReturn(Completable.error(error))

        val testObserver = TestObserver<Result<Unit>>()
        viewModel.sendSignaturePush(info).subscribe(testObserver)
        testObserver.assertResult(ErrorResult(error))

        val data = GnoSafeUrlParser.signRequest(
            TEST_TRANSACTION_HASH,
            info.selectedSafe,
            TEST_TRANSACTION.address,
            TEST_TRANSACTION.value,
            TEST_TRANSACTION.data,
            TEST_TRANSACTION.nonce!!
        )
        then(signaturePushRepositoryMock).should().request(TEST_SAFE, data)
    }

    private data class TestException(val name: String = "test_exception") : Exception()

    companion object {
        private const val TEST_TRANSACTION_HASH = "SomeHash"
        private val TEST_BITMAP = mock(Bitmap::class.java)
        private val TEST_SIGNATURE = Signature(BigInteger.valueOf(987), BigInteger.valueOf(678), 27)
        private val TEST_SAFE = BigInteger.ZERO
        private val TEST_TRANSACTION = Transaction(BigInteger.ZERO, nonce = BigInteger.TEN)
        private val TEST_NOT_OWNER = BigInteger.valueOf(12345)
        private val TEST_OWNER = BigInteger.valueOf(5674)
        private val TEST_OWNERS = listOf(TEST_OWNER, BigInteger.valueOf(13))
    }
}
