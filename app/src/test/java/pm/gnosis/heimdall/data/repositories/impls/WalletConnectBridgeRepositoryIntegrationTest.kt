package pm.gnosis.heimdall.data.repositories.impls

import android.app.Application
import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.picasso.Picasso
import io.reactivex.rxkotlin.subscribeBy
import okhttp3.OkHttpClient
import org.junit.Rule
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.walletconnect.impls.FileWCSessionStore
import org.walletconnect.impls.MoshiPayloadAdapter
import org.walletconnect.impls.OkHttpTransport
import pm.gnosis.heimdall.data.preferences.PreferencesWalletConnect
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.utils.asEthereumAddress
import java.io.File
import java.util.concurrent.TimeUnit

class WalletConnectBridgeRepositoryIntegrationTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var application: Application


    //@Test
    fun integration() {

        BDDMockito.given(application.getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())).willReturn(TestPreferences())
        val prefs = PreferencesWalletConnect(application)

        val client = OkHttpClient.Builder().pingInterval(1000, TimeUnit.MILLISECONDS).build()
        val moshi = Moshi.Builder().build()
        val sessionStore = FileWCSessionStore(File("build/tmp/test_store.json").apply { createNewFile() }, moshi)
        val sessionPayloadAdapter = MoshiPayloadAdapter(moshi)
        val sessionTransportBuilder = OkHttpTransport.Builder(client, moshi)
        val sessionBuilder = WCSessionBuilder(sessionStore, sessionPayloadAdapter, sessionTransportBuilder)
        val rpcProxyApiMock = mock(RpcProxyApi::class.java)

        val repo =
            WalletConnectBridgeRepository(
                mock(Context::class.java),
                rpcProxyApiMock,
                mock(Picasso::class.java),
                mock(TransactionInfoRepository::class.java),
                mock(LocalNotificationManager::class.java),
                sessionStore,
                sessionBuilder,
                prefs,
                mock(TransactionExecutionRepository::class.java)
            )
        val uri =
            "wc:11ed3d6d-5611-4b29-b7b3-5cf6de484f05@1?bridge=https%3A%2F%2Fbridge.walletconnect.org&key=5ddc6551e18349068f7ba3dbd909b3182be28475879d24e929c60e3cbb2d36ee"

        repo.sessions(null)
            .subscribe { list -> System.out.println("Sessions: $list") }
        val sessionId = repo.createSession(uri, "0xdeadbeef".asEthereumAddress()!!)
        repo.observeSession(sessionId)
            .subscribeBy(
                onError = {
                    System.out.println("Observe error $it")
                },
                onNext = {
                    System.out.println("Observe data $it")
                    when (it) {
                        is BridgeRepository.SessionEvent.Transaction -> {
                            rejectRequest(repo, it.id, "0x52275f87fc078ff8381f636776cb649dda7a8882e7ba7a2ba9aa1cf24ce4b849")
                        }
                    }
                }
            )
        repo.initSession(sessionId)
            .subscribeBy(
                onError = {
                    System.out.println("Init error $it")
                },
                onComplete = {
                    System.out.println("Init complete")
                }
            )
        repo.sessions(null)
            .subscribe { list -> System.out.println("Sessions: $list") }
        Thread.sleep(100000)
    }

    private fun approveRequest(repo: WalletConnectBridgeRepository, requestId: Long, response: Any) {
        System.out.println("Approve request")
        repo.approveRequest(requestId, response).subscribeBy(
            onError = {
                System.out.println("Approve request error $it")
            },
            onComplete = {
                System.out.println("Approve request complete")
            }
        )
    }

    private fun rejectRequest(repo: WalletConnectBridgeRepository, requestId: Long, errorMessage: String) {
        System.out.println("Reject request")
        repo.rejectRequest(requestId, BridgeRepository.RejectionReason.RPCError(42, errorMessage)).subscribeBy(
            onError = {
                System.out.println("Reject request error $it")
            },
            onComplete = {
                System.out.println("Reject request complete")
            }
        )
    }
}

