package pm.gnosis.crypto

import okio.ByteString
import org.junit.Test

import org.junit.Assert.*

class KeyGeneratorTest {

    @Test
    fun masterNode() {
        TEST_CASES.forEach { (encodedSeed, encodedNode) ->
            val seed = ByteString.decodeHex(encodedSeed)
            val masterNode = KeyGenerator.masterNode(seed)
            Thread.sleep(100)
            val masterNodeCopy = KeyGenerator.masterNode(seed)
            assertEquals("Nodes generated with the same seed should have the same private key", masterNode.keyPair.privKey, masterNodeCopy.keyPair.privKey)
            assertEquals(encodedNode, masterNode.toBase58())
        }
    }

    companion object {
        private val TEST_CASES = listOf(
                "8ddfc9c8905eae8083b8b2f5c0e9818505fb58070fc812251708d8e8a0f533d309598f9d3abafa3489b0f555a6f2626a7e4c71fe4284673c8544db0c7d9fa74f" to
                        "xprv9s21ZrQH143K37Rktnz53tupRnjoX6ecHqJBmvbBKrrTrrvgwdXvcHT4KdT791EYrCLhZXUjKYwbDURbyHPMD6bBhDNmSYxK6baRU2uQoC9",

                "cbd57df0e668634fd43484eb0d85f3197c93a1a01d0e0c4375ca74c743d3925daf0c041fc4bb55ccf453628f48354cd6eddc77a6e878a2990b1b51788417568e" to
                        "xprv9s21ZrQH143K383fjdxFR2oMzfzyqL1vRs2aHotbLovKV13iVrERhjDtT6AV88zRZLCJAh8Kq2Bo2JqLmF3aUo6ZJKmGeHjirFxBy94TnVy",

                "cf5c843ae29507c38719d56d796dd33b76f90b4de72819ab1f7185df2ddf2658dc6209fa8fa056f1c07b0299857864a84caf0dca3001def6899348b499d62931" to
                        "xprv9s21ZrQH143K4QBuXhvun11mjYxREqJZaVJGciPMN8g3Jpbe57PHCSnY9fWnGhuvTthmGFFqWr4cAVe9hJR8gp4Yz6HyigQ81jDowRzHt1F",

                "ba1dec3c908d862cf0d3265b4087708e3cd5360026a34adfccaa31972f722eab9534dfe7cab81855287e2150aa99fad53abdd4820d6134219f9fc2685a3e8ab5" to
                        "xprv9s21ZrQH143K2Awv8WZhYFfn1hwB2p2pfv7VTRa8aTZKz1HhCgzhbi7Hd3K1bZyApEgH9aEtkoBVcLY8oHzS9Cysv2Jp6iyHpA46w1a7YTb",

                "d991930b92a9345fbf81fa0b91fb7410912bf5036884b5133ea878e7fc67be7ec5dc1dcfa183afe2caa35538c0213e7cd07c416fc99d2341a98650cb4502e904" to
                        "xprv9s21ZrQH143K4N25d2A1GceR9RouixtHPnGieKwUXpqQJAJi8Co3TouFSsmwNNEoRaMiRRYemt4A26LZbhUj2mYBHMRJfoBxJJ6i8LDksJ4"
        )
    }
}
