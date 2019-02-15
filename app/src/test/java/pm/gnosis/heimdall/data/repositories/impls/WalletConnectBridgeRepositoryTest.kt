package pm.gnosis.heimdall.data.repositories.impls

import okhttp3.OkHttpClient
import org.junit.Test
import java.util.concurrent.TimeUnit

class WalletConnectBridgeRepositoryTest {


    @Test
    fun integration() {
        System.out.println("BLA")
        val repo = WalletConnectBridgeRepository(OkHttpClient.Builder().pingInterval(1000, TimeUnit.MILLISECONDS).build())
        //val id = repo.connect("wss://bridge.walletconnect.org")
        val session = repo.connect("ws://echo.websocket.org")
        Thread.sleep(2000)
        session.send("{\"test\": \"message\"}")
        Thread.sleep(2000)
        session.close()
        Thread.sleep(2000)
    }
}
