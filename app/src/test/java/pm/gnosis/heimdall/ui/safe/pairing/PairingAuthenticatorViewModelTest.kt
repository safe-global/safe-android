package pm.gnosis.heimdall.ui.safe.pairing

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import junit.framework.Assert.assertEquals
import org.hamcrest.core.AnyOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.adapters.DecimalNumberAdapter
import pm.gnosis.heimdall.data.adapters.DefaultNumberAdapter
import pm.gnosis.heimdall.data.remote.models.push.PushServiceTemporaryAuthorization
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.ui.two_factor.authenticator.PairingAuthenticatorContract
import pm.gnosis.heimdall.ui.two_factor.authenticator.PairingAuthenticatorViewModel
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.tests.utils.*
import pm.gnosis.utils.asEthereumAddress
import java.lang.Exception

@RunWith(MockitoJUnitRunner::class)
class PairingAuthenticatorViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @JvmField
    @Rule
    val lifecycleRule = TestLifecycleRule()

    @Mock
    private lateinit var pushServiceRepositoryMock: PushServiceRepository

    private val encryptedByteArrayConverter = EncryptedByteArray.Converter()

    private lateinit var viewModel: PairingAuthenticatorViewModel

    private val moshi = Moshi.Builder()
        .add(DecimalNumberAdapter())
        .add(DefaultNumberAdapter())
        .build()

    @Before
    fun setup() {
        val safe = "0xdeadbeef".asEthereumAddress()!!
        viewModel = PairingAuthenticatorViewModel(pushServiceRepositoryMock, moshi, testAppDispatchers)
        viewModel.setup(safe)
    }

    @Test
    fun pair() {
        val safe = "0xdeadbeef".asEthereumAddress()!!
        val address = Solidity.Address(1.toBigInteger())

        val safeOwner = AccountsRepository.SafeOwner("0xbaddad".asEthereumAddress()!!, encryptedByteArrayConverter.fromStorage("pk"))
        given(pushServiceRepositoryMock.pair(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(safeOwner to address))

        val testObserver = TestLiveDataObserver<PairingAuthenticatorContract.ViewUpdate>()
        viewModel.observableState.observe(lifecycleRule, testObserver)

        viewModel.pair(PAYLOAD)

        then(pushServiceRepositoryMock).should().pair(TEMPORARY_AUTHORIZATION, safe)
        then(pushServiceRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertValues(
            PairingAuthenticatorContract.ViewUpdate(
                true,
                null
            ),
            PairingAuthenticatorContract.ViewUpdate(
                false,
                PairingAuthenticatorContract.PairingResult.PairingSuccess(
                    AuthenticatorSetupInfo(safeOwner, AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, address))
                )
            )
        )
    }

    @Test
    fun pairPushServiceError() {
        val safe = "0xdeadbeef".asEthereumAddress()!!
        val exception = Exception("exception")
        exception.stackTrace = arrayOf()

        given(pushServiceRepositoryMock.pair(MockUtils.any(), MockUtils.any())).willReturn(Single.error(exception))

        val testObserver = TestLiveDataObserver<PairingAuthenticatorContract.ViewUpdate>()
        viewModel.observableState.observe(lifecycleRule, testObserver)

        viewModel.pair(PAYLOAD)

        then(pushServiceRepositoryMock).should().pair(TEMPORARY_AUTHORIZATION, safe)
        then(pushServiceRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertValueCount(2)
        assertEquals(testObserver.values()[0], PairingAuthenticatorContract.ViewUpdate(true, null))
        assert(testObserver.values()[1].pairingResult is PairingAuthenticatorContract.PairingResult.PairingError)
    }

    @Test
    fun pairInvalidPayload() {
        val invalidPayload = """ {
    "signature": {
        "r": "308286038459945541092142858495441563861731132686688575999943100082681274395",
        "s": "5480478211022603008401571187091564117322602492197866229991416173934635675148"
    },
    "expirationDate": "2018-04-18T14:46:09+00:00"
}
            """

        val testObserver = TestLiveDataObserver<PairingAuthenticatorContract.ViewUpdate>()
        viewModel.observableState.observe(lifecycleRule, testObserver)

        viewModel.pair(invalidPayload)

        testObserver.assertValueCount(2)
        assertEquals(testObserver.values()[0], PairingAuthenticatorContract.ViewUpdate(true, null))
        assert(testObserver.values()[1].pairingResult is PairingAuthenticatorContract.PairingResult.PairingError)

        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()
    }

    companion object {
        private const val PAYLOAD = """
            {
    "signature": {
        "v": "28",
        "r": "308286038459945541092142858495441563861731132686688575999943100082681274395",
        "s": "5480478211022603008401571187091564117322602492197866229991416173934635675148"
    },
    "expirationDate": "2018-04-18T14:46:09+00:00"
}
            """
        private val TEMPORARY_AUTHORIZATION = PushServiceTemporaryAuthorization(
            ServiceSignature(
                r = "308286038459945541092142858495441563861731132686688575999943100082681274395".toBigInteger(),
                s = "5480478211022603008401571187091564117322602492197866229991416173934635675148".toBigInteger(),
                v = 28
            ),
            expirationDate = "2018-04-18T14:46:09+00:00"
        )
    }
}
