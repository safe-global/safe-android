package io.gnosis.safe.ui.transactions.details.models

import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Safe
import io.gnosis.safe.ui.transactions.AddressInfoData
import junit.framework.Assert.assertEquals
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress

class TransactionDetailsViewDataTest {

    @Test
    fun `toAddressInfoData() (Address matches local safe) should return AddressInfoData_Local`() {

        val addressInfo = AddressInfo("Foo", "https://www.foo.de/foo.png")
        val safes = listOf<Safe>(Safe("0x01".asEthereumAddress()!!, "Lokaler Name"))

        val result = addressInfo.toAddressInfoData("0x01".asEthereumAddress()!!, safes)

        assertEquals(AddressInfoData.Local("Lokaler Name", "0x0000000000000000000000000000000000000001"), result)
    }

    @Test
    fun `toAddressInfoData() (Address does not match local safe) should return AddressInfoData_Remote`() {

        val addressInfo = AddressInfo("Foo", "https://www.foo.de/foo.png")
        val safes = listOf(Safe("0x01".asEthereumAddress()!!, "Lokaler Name"))

        val result = addressInfo.toAddressInfoData("0x02".asEthereumAddress()!!, safes)

        assertEquals(AddressInfoData.Remote("Foo", "https://www.foo.de/foo.png", "0x0000000000000000000000000000000000000002"), result)
    }
}
