package pm.gnosis.heimdall.data.repositories.impls

import android.content.Context
import com.squareup.moshi.Moshi
import io.reactivex.Flowable
import io.reactivex.rxkotlin.subscribeBy
import okhttp3.OkHttpClient
import org.junit.Rule
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.impls.wc.FileWCSessionStore
import pm.gnosis.heimdall.data.repositories.impls.wc.MoshiPayloadAdapter
import pm.gnosis.heimdall.data.repositories.impls.wc.OkHttpTransport
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.utils.asEthereumAddress
import java.io.File
import java.util.concurrent.TimeUnit

class WalletConnectBridgeRepositoryIntegrationTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    //@Test
    fun integration() {
        val client = OkHttpClient.Builder().pingInterval(1000, TimeUnit.MILLISECONDS).build()
        val moshi = Moshi.Builder().build()
        val sessionStore = FileWCSessionStore(File("build/tmp/test_store.json").apply { createNewFile() }, moshi)
        val sessionPayloadAdapter = MoshiPayloadAdapter(moshi)
        val sessionTransportBuilder = OkHttpTransport.Builder(client, moshi)
        val safeRepoMock = mock(GnosisSafeRepository::class.java)
        given(safeRepoMock.observeSafes()).willReturn(Flowable.just(listOf(Safe("0xdeadbeef".asEthereumAddress()!!))))
        val repo =
            WalletConnectBridgeRepository(
                mock(Context::class.java),
                mock(TransactionInfoRepository::class.java),
                mock(LocalNotificationManager::class.java),
                safeRepoMock,
                sessionStore,
                sessionPayloadAdapter,
                sessionTransportBuilder,
                mock(TransactionExecutionRepository::class.java)
            )
        val uri =
            "wc:11ed3d6d-5611-4b29-b7b3-5cf6de484f05@1?bridge=https%3A%2F%2Fbridge.walletconnect.org&key=5ddc6551e18349068f7ba3dbd909b3182be28475879d24e929c60e3cbb2d36ee"

        repo.sessions()
            .subscribe { list -> System.out.println("Sessions: $list") }
        val sessionId = repo.createSession(uri)
        repo.observeSession(sessionId)
            .subscribeBy(
                onError = {
                    System.out.println("Observe error $it")
                },
                onNext = {
                    System.out.println("Observe data $it")
                    when (it) {
                        is BridgeRepository.SessionEvent.SessionRequest -> {
                            approveSession(repo, sessionId)
                        }
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
        repo.sessions()
            .subscribe { list -> System.out.println("Sessions: $list") }
        Thread.sleep(100000)
    }

    private fun approveSession(repo: WalletConnectBridgeRepository, sessionId: String) {
        System.out.println("Approve session")
        repo.approveSession(sessionId).subscribeBy(
            onError = {
                System.out.println("Approve error $it")
            },
            onComplete = {
                System.out.println("Approve complete")
            }
        )
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
        repo.rejectRequest(requestId, 42, errorMessage).subscribeBy(
            onError = {
                System.out.println("Reject request error $it")
            },
            onComplete = {
                System.out.println("Reject request complete")
            }
        )
    }
}

