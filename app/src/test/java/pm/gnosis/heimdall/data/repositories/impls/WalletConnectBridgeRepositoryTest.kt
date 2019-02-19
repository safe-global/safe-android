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
        val uri =
            "wc:645bf32b-8697-476f-a85f-7125a03bd012@1?bridge=https%3A%2F%2Fbridge.walletconnect.org&key=98650b0362374ef67e5d434900fdf294d861480ee180ea50ea080476c926ef5a"

        val sessionId = repo.createSession(uri)
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
                            rejectRequest(repo, sessionId, it, "0x52275f87fc078ff8381f636776cb649dda7a8882e7ba7a2ba9aa1cf24ce4b849")
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

    private fun rejectRequest(repo: WalletConnectBridgeRepository, sessionId: String, requestId: Long, errorMessage: String) {
        System.out.println("Reject request")
        repo.rejectRequest(sessionId, requestId, 42, errorMessage).subscribeBy(
            onError = {
                System.out.println("Reject request error $it")
            },
            onComplete = {
                System.out.println("Reject request complete")
            }
        )
    }
}
