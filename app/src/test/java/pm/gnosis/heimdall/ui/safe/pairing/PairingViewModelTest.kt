package pm.gnosis.heimdall.ui.safe.pairing

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.adapters.DecimalNumberAdapter
import pm.gnosis.heimdall.data.adapters.DefaultNumberAdapter
import pm.gnosis.heimdall.data.remote.models.push.PushServiceTemporaryAuthorization
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.model.Solidity
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils

@RunWith(MockitoJUnitRunner::class)
class PairingViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var pushServiceRepositoryMock: PushServiceRepository

    private lateinit var viewModel: PairingViewModel

    private val moshi = Moshi.Builder()
        .add(DecimalNumberAdapter())
        .add(DefaultNumberAdapter())
        .build()

    @Before
    fun setup() {
        viewModel = PairingViewModel(pushServiceRepositoryMock, moshi)
    }

    @Test
    fun pair() {
        val testObserver = TestObserver.create<Solidity.Address>()
        val address = Solidity.Address(1.toBigInteger())

        given(pushServiceRepositoryMock.pair(MockUtils.any())).willReturn(Single.just(address))

        viewModel.pair(PAYLOAD).subscribe(testObserver)

        testObserver.assertResult(address)

        then(pushServiceRepositoryMock).should().pair(TEMPORARY_AUTHORIZATION)
        then(pushServiceRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun pairPushServiceError() {
        val testObserver = TestObserver.create<Solidity.Address>()

        given(pushServiceRepositoryMock.pair(MockUtils.any())).willReturn(Single.error(Exception()))

        viewModel.pair(PAYLOAD).subscribe(testObserver)

        testObserver.assertFailure(Exception::class.java)

        then(pushServiceRepositoryMock).should().pair(TEMPORARY_AUTHORIZATION)
        then(pushServiceRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun pairInvalidPayload() {
        val testObserver = TestObserver.create<Solidity.Address>()
        val invalidPayload = """ {
    "signature": {
        "r": "308286038459945541092142858495441563861731132686688575999943100082681274395",
        "s": "5480478211022603008401571187091564117322602492197866229991416173934635675148"
    },
    "expirationDate": "2018-04-18T14:46:09+00:00"
}
            """

        viewModel.pair(invalidPayload).subscribe(testObserver)

        testObserver.assertFailure(JsonDataException::class.java)

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
