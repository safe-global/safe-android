package pm.gnosis.heimdall.utils

import org.junit.Test

import org.junit.Assert.*
import pm.gnosis.utils.asEthereumAddress

class SafeContractUtilsTest {

    @Test
    fun checkForUpdate() {
        assertNull(SafeContractUtils.checkForUpdate(TEST_SAFE))
        assertNull(SafeContractUtils.checkForUpdate(SAFE_MASTER_COPY_1_0_0))

        assertEquals(SAFE_MASTER_COPY_1_0_0, SafeContractUtils.checkForUpdate(SAFE_MASTER_COPY_0_0_2))
        assertEquals(SAFE_MASTER_COPY_1_0_0, SafeContractUtils.checkForUpdate(SAFE_MASTER_COPY_0_1_0))
    }

    @Test
    fun isSupported() {
        assertTrue(SafeContractUtils.isSupported(SAFE_MASTER_COPY_0_0_2))
        assertTrue(SafeContractUtils.isSupported(SAFE_MASTER_COPY_0_1_0))
        assertTrue(SafeContractUtils.isSupported(SAFE_MASTER_COPY_1_0_0))

        assertFalse(SafeContractUtils.isSupported(TEST_SAFE))
    }

    @Test
    fun currentMasterCopy() {
        assertEquals(SAFE_MASTER_COPY_1_0_0, SafeContractUtils.currentMasterCopy())
    }

    companion object {
        private val TEST_SAFE = "0xdeadfeedbeaf".asEthereumAddress()!!
        private val SAFE_MASTER_COPY_0_0_2 = "0xAC6072986E985aaBE7804695EC2d8970Cf7541A2".asEthereumAddress()!!
        private val SAFE_MASTER_COPY_0_1_0 = "0x8942595A2dC5181Df0465AF0D7be08c8f23C93af".asEthereumAddress()!!
        private val SAFE_MASTER_COPY_1_0_0 = "0xb6029EA3B2c51D09a50B53CA8012FeEB05bDa35A".asEthereumAddress()!!
    }
}