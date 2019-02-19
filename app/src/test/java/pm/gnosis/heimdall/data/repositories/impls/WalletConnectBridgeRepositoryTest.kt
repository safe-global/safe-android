package pm.gnosis.heimdall.data.repositories.impls

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.reactivex.rxkotlin.subscribeBy
import okhttp3.OkHttpClient
import org.junit.Rule
import org.junit.Test
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.repositories.BridgeReposity
import pm.gnosis.heimdall.data.repositories.impls.wc.WCSessionStore
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.nullOnThrow
import java.io.File
import java.lang.IllegalStateException
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class WalletConnectBridgeRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()


    @Test
    fun integration() {
        val client = OkHttpClient.Builder().pingInterval(1000, TimeUnit.MILLISECONDS).build()
        val moshi = Moshi.Builder().build()
        val repo = WalletConnectBridgeRepository(client, moshi, FileWCSessionStore(moshi))
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
                        is BridgeReposity.SessionEvent.SessionRequest -> {
                            approveSession(repo, sessionId)
                        }
                        is BridgeReposity.SessionEvent.Transaction -> {
                            rejectRequest(repo, sessionId, it.id, "0x52275f87fc078ff8381f636776cb649dda7a8882e7ba7a2ba9aa1cf24ce4b849")
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

class FileWCSessionStore(moshi: Moshi) : WCSessionStore {
    private val adapter = moshi.adapter<Map<String, WCSessionStore.State>>(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            WCSessionStore.State::class.java
        )
    )

    private val storageFile: File = File("build/tmp/test_store.json").apply { createNewFile() }
    private val currentStates: MutableMap<String, WCSessionStore.State> = ConcurrentHashMap()

    init {
        val storeContent = storageFile.readText()
        nullOnThrow { adapter.fromJson(storeContent) }?.let {
            currentStates.putAll(it)
        }
    }

    override fun load(id: String): WCSessionStore.State? = currentStates[id]

    override fun store(id: String, state: WCSessionStore.State) {
        currentStates[id] = state
        writeToFile()
    }

    override fun remove(id: String) {
        currentStates.remove(id)
        writeToFile()
    }

    override fun list(): List<WCSessionStore.State> = currentStates.values.toList()

    private fun writeToFile() {
        storageFile.writeText(adapter.toJson(currentStates))
    }

}
