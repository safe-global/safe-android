package pm.gnosis.heimdall.data.repositories.impls

import com.squareup.moshi.Moshi
import io.reactivex.rxkotlin.subscribeBy
import okhttp3.OkHttpClient
import org.junit.Rule
import org.junit.Test
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.utils.asEthereumAddress
import java.lang.IllegalStateException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

class WalletConnectBridgeRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()


    @Test
    fun integration() {
        val repo = WalletConnectBridgeRepository(OkHttpClient.Builder().pingInterval(1000, TimeUnit.MILLISECONDS).build(), Moshi.Builder().build())
        val id = UUID.randomUUID().toString()
        val clientData = Session.PayloadAdapter.PeerData(id, null)
        val uri =
            "wc:bb33a5cc-9e98-45b4-ad73-737938a3bc7f@1?bridge=https%3A%2F%2Fbridge.walletconnect.org&key=4c9929cf102d1cfa9f9b7015fd5a5e24f7654294f8c8e8a63e2231aab3b86871"

        val sessionId = repo.createSession(uri, clientData)
        repo.observeSession(sessionId)
            .subscribeBy(
                onError = {
                    System.out.println("Observe error $it")
                },
                onNext = {
                    System.out.println("Observe data $it")
                    when (it) {
                        is Session.PayloadAdapter.PeerData -> {
                            approveSession(repo, sessionId)
                        }
                        is Long -> {
                            approveRequest(repo, sessionId, it, "0x52275f87fc078ff8381f636776cb649dda7a8882e7ba7a2ba9aa1cf24ce4b849")
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
        Thread.sleep(100000)
    }

    private fun approveSession(repo: WalletConnectBridgeRepository, sessionId: String) {
        System.out.println("Approve session")
        repo.approveSession(sessionId, "0xdeadbeef".asEthereumAddress()!!).subscribeBy(
            onError = {
                System.out.println("Approve error $it")
            },
            onComplete = {
                System.out.println("Approve complete")
            }
        )
    }

    private fun approveRequest(repo: WalletConnectBridgeRepository, sessionId: String, requestId: Long, response: Any) {
        System.out.println("Approve request")
        repo.approveRequest(sessionId, requestId, response).subscribeBy(
            onError = {
                System.out.println("Approve request error $it")
            },
            onComplete = {
                System.out.println("Approve request complete")
            }
        )
    }
}
