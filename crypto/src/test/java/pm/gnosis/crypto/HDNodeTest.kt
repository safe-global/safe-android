package pm.gnosis.crypto

import okio.ByteString
import org.junit.Assert.*
import org.junit.Test
import pm.gnosis.tests.utils.Asserts.assertThrow
import pm.gnosis.utils.hexStringToByteArray
import java.math.BigInteger

class HDNodeTest {
    @Test
    fun derive() {
        // Create node -> author wire harbor elbow library sphere nothing receive team
        val masterNode = KeyGenerator.masterNode(ByteString.decodeHex("358ace7a8cff8d5d42e2b33b5a8584b785e317df5f7b2717b4c5da23faf1f1442a644117005b37874b1be21b7a76cf4cd942dd93aabf5e33f392944f83e8ce33"))
        assertEquals("xprv9s21ZrQH143K4SVQXcP9UHe8tk5GhRpLJH97oc3v99LJbRt8xhdRUZgWpSN4nFuFWoMz7NW2HGUZVYyBP6n8Q9pR96MzwhPweAQ7nGhGZHF", masterNode.toBase58())
        val derivedAccount = masterNode.derive("m/44'/60'/0'")
        assertEquals("xprv9yw9Z66v5kozgS9sGqFd8kpVWeT5Zy3yYant2HfaKCPoM33jzYYZbKbRDY4qhEDShFLqTrjaB8hKhUQH2Udp5utkizn3rqjEUafoTKseGRT", derivedAccount.toBase58())
        val derivedBip32 = masterNode.derive("m/44'/60'/0'/0")
        assertEquals("xprvA1cWiK7q6My2FrunnCP1BTVXwdgy3aW8bBVAVF2wxkKWvA8BSdUjpBzeCHdL8k1ycs6k9icQVQytN71m7povA6ynMniQCMUytF35Ej6BZjZ", derivedBip32.toBase58())

        TEST_DATA.forEachIndexed { index, value ->
            val privateKey = BigInteger(value.private, 16)
            val derivedChild = derivedBip32.deriveChild(index.toLong())
            val deriveChildCopy = masterNode.derive("m/44'/60'/0'/0/" + index)
            assertEquals(privateKey, derivedChild.keyPair.privKey)
            assertEquals(privateKey, deriveChildCopy.keyPair.privKey)
            assertEquals(derivedChild.toBase58(), deriveChildCopy.toBase58())
            assertEquals(ByteString.decodeHex(value.public), derivedChild.publicKey())
            assertArrayEquals(value.address.hexStringToByteArray(), derivedChild.keyPair.address)
        }
    }

    @Test
    fun deriveSelf() {
        // Create node -> author wire harbor elbow library sphere nothing receive team
        val masterNode = KeyGenerator.masterNode(ByteString.decodeHex("358ace7a8cff8d5d42e2b33b5a8584b785e317df5f7b2717b4c5da23faf1f1442a644117005b37874b1be21b7a76cf4cd942dd93aabf5e33f392944f83e8ce33"))
        assertEquals(masterNode, masterNode.derive("m"))
        assertEquals(masterNode, masterNode.derive("M"))
        assertEquals(masterNode, masterNode.derive("m'"))
        assertEquals(masterNode, masterNode.derive("M'"))
    }

    @Test
    fun deriveInvalid() {
        // Create node -> author wire harbor elbow library sphere nothing receive team
        val masterNode = KeyGenerator.masterNode(ByteString.decodeHex("358ace7a8cff8d5d42e2b33b5a8584b785e317df5f7b2717b4c5da23faf1f1442a644117005b37874b1be21b7a76cf4cd942dd93aabf5e33f392944f83e8ce33"))
        assertThrow({ masterNode.derive("x") }, "invalid first index")
        assertThrow({ masterNode.derive("z/44'") }, "invalid first index")
        assertThrow({ masterNode.derive("m/" + 0x80000000) }, "invalid child index")
    }

    companion object {
        val TEST_DATA = listOf(
                Data("ad6e370aecf06edc5ef5e0973fea6b4adc8731497317c3efd222c316a43b8139", "032cfa7ca396fd9cc021c68852e8ccfcaa653c048be964290402a351d69133cc87", "2BB214087AE5dD0a8eaFe03ACa93882dB72447f8"),
                Data("29ac087b1f80402d5dbda7f173f6b98a3f7d1274f631cf7c8a2bc2e8e2e6e534", "020d62c399b518d096b4281a694b77b648f976abe2b331dfbd9b979b4bc84bd0f2", "474E4bA7e4Fb1087aef7dDf514fE8ace4D1dc245"),
                Data("4166ced28dd516c066d04a541312392a836d9b0136bcc4e5896e1c3eb00c9bf4", "02666ca3d549b07a4cf12eb902cfe1286054beb8d18daf97dbf3bcfe3fac695c17", "7F866EECA744aAd89c93618f5ffac7E94AFF420a"),
                Data("61e1f8e5e603aee89e970a4742ce85f6b3890ad576f1385955c1245287c58c40", "02dfcc92120812420261a0ab354e8b04aa6d8b20a3de7fdfb14193aa95c29f7e58", "e3da1E136980e16500207337a3cd66D1fAD6DC08"),
                Data("6815077a5df07146e402ae755cc88fdcdc16fee7d66893524a5cbdf20eaada41", "02313550bbe07f9603f238a566a9153c96ab5671214473ffe573b17d7e08cb2864", "494d1D665cf7b7d57b799D7C3a30f0B814bF3E06"),
                Data("8730971d969566fc1f3f81f36d4fae0ddb8bdf9efe0a2fa9c0a96492b660a46c", "02652269029cd364cdef999afe81194801575417e59e4b9343bc6cdcc6ea205933", "B5E1ae61eB69acDcf450E337C127C3015Cc7eDb2"),
                Data("1c528f6df26bcbb7c7f3d326c8d692fc84ac9387aff104bf12e3c0d89d7f6d79", "03952bf2093c61ca32d2e3bfd22ffb0005f6e36ec0db17145a08f4ccf4a1637b42", "b923590B604d47dE4fFf26962CB368b5C9E3aF43"),
                Data("c7478817adbce6b7d6b636b3ac77e21e79a3ce6474874335598e05a58fd98d19", "027be679ec9daa7c5048515ecd93fca542172fb955b0a86b9dd6926bc4458f756f", "94Ea3B87A2A0e337779139988AF0C17f894FA61f"),
                Data("3ac56af4e3130d982c38995fbc4570bd5588265dafd98163a730f638ddf8c9dc", "02c60397167da62229a150644801f8d15e507ecfabbab801c975e480c186706d41", "F21F906670C4339C5750809DE91ad14b95b13D23"),
                Data("2102867ecaa68a28e5edd780b58da834197ae6146ea54e9339f3e89eb3bb8e4a", "02efbb40c4c7ffc91fcd2c05195a28b18674e09e855a04d24d379e88df373ccf99", "cdaF4d13cB2e9BC7113Fc8c1BA17C7bbfCFF72fD"),
                Data("9cdfdedca8725f70279ee1ff4d6a7617eb95fd35741c004f965b3b75660b66e6", "021f5c5ec1c88f0bdc342bd4aee6b66f25f172f0b44c5ed5344d21e29a93628ecb", "54263c3A93cc968e7B92410c9940BEA236eF3968"),
                Data("db65afa229a4bb540133c6af4b6ea8d5ec2962350b1c8773d6d525f76481eb28", "033a37f3545e9e1f576a884988531bad8c628515bef8dfad0f26e25d3cf44b7062", "F1b69317d40df3D27DC796EdcC93Ef672b2f3B74"),
                Data("a37bc10a1a0f7e8a70b307a5d8c4763d13ded86fe2222ab5fbb73e2c4b891b96", "0312e781a8a454f087a1b21e77ad5591e8db0365771fe5c15ef687e808d0ba8f24", "e2E63c255Fdc17d3228A04FAa7B3A444110BE278"),
                Data("8c260154be0d8850f7c36e3e819503daef0ec84565e4abace5cee9fc0f268656", "02dab4f21988b95769c06b9660e391a905ae8272ba413e997e2d94ff5ecabc642d", "aeFe5e5c3Fc58014F9D0878CD824571199244C3E")
        )
    }

    data class Data(val private: String, val public: String, val address: String)
}
