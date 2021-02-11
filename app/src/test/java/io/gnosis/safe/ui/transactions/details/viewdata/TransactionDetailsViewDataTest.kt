package io.gnosis.safe.ui.transactions.details.viewdata

import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.*
import io.gnosis.safe.ui.transactions.AddressInfoData
import junit.framework.Assert.assertEquals
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

class TransactionDetailsViewDataTest {

    private val anyAddress = "0x0000000000000000000000000000000000000001".asEthereumAddress()!!
    private val anotherAddress = "0x0000000000000000000000000000000000000002".asEthereumAddress()!!

    @Test
    fun `toAddressInfoData() (Address matches local safe) should return AddressInfoData_Local`() {
        val addressInfo = AddressInfo("Foo", "https://www.foo.de/foo.png")
        val safes = listOf(Safe(anyAddress, "Local Name"))

        val result = addressInfo.toAddressInfoData(anyAddress, safes)

        assertEquals(AddressInfoData.Local("Local Name", "0x0000000000000000000000000000000000000001"), result)
    }

    @Test
    fun `toAddressInfoData() (Address does not match local safe) should return AddressInfoData_Remote`() {
        val addressInfo = AddressInfo("Foo", "https://www.foo.de/foo.png")
        val safes = listOf(Safe(anyAddress, "Local Name"))

        val result = addressInfo.toAddressInfoData(anotherAddress, safes)

        assertEquals(AddressInfoData.Remote("Foo", "https://www.foo.de/foo.png", "0x0000000000000000000000000000000000000002"), result)
    }

    @Test
    fun `toAddressInfoData() (No AddressInfo and emptySafeList) should return AddressInfoData_Default`() {
        val safes = emptyList<Safe>()

        val result = null.toAddressInfoData(anotherAddress, safes)

        assertEquals(AddressInfoData.Default, result)
    }

    @Test
    fun `toAddressInfoData() (No AddressInfo and non matching safeList) should return AddressInfoData_Default`() {
        val safes = listOf(Safe(anyAddress, "Local Name"))

        val result = null.toAddressInfoData(anotherAddress, safes)

        assertEquals(AddressInfoData.Default, result)
    }

    @Test
    fun `toSettingsInfoViewData() (DisableModule with AddressInfo) should return DisableModuleViewData `() {
        val settingsInfo = SettingsInfo.DisableModule(anyAddress, AddressInfo("Remote Name", null))
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = settingsInfo.toSettingsInfoViewData(safes)

        assertEquals(
            SettingsInfoViewData.DisableModule(
                anyAddress,
                AddressInfoData.Remote("Remote Name", null, anyAddress.asEthereumAddressChecksumString())
            ),
            result
        )
    }

    @Test
    fun `toSettingsInfoViewData() (SwapOwner) should return SwapOwnerViewData`() {
        val settingsInfo = SettingsInfo.SwapOwner(anyAddress, AddressInfo("Remote Old Owner Name", null), anotherAddress, null)
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = settingsInfo.toSettingsInfoViewData(safes)

        assertEquals(
            SettingsInfoViewData.SwapOwner(
                anyAddress,
                AddressInfoData.Remote("Remote Old Owner Name", null, anyAddress.asEthereumAddressChecksumString()),
                anotherAddress,
                AddressInfoData.Local("Local Name", anotherAddress.asEthereumAddressChecksumString())
            ),
            result
        )
    }

    @Test
    fun `toSettingsInfoViewData() (ChangeImplementation with AddressInfo) should return ChangeImplementationViewData `() {
        val settingsInfo = SettingsInfo.ChangeImplementation(anyAddress, AddressInfo("Remote Name", null))
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = settingsInfo.toSettingsInfoViewData(safes)

        assertEquals(
            SettingsInfoViewData.ChangeImplementation(
                anyAddress,
                AddressInfoData.Remote("Remote Name", null, anyAddress.asEthereumAddressChecksumString())
            ),
            result
        )
    }


    @Test
    fun `toTransactionInfoViewData() (TransactionInfo_Custom with AddressInfo) should return TransactionInfoViewData_Custom `() {
        val transactionInfo = TransactionInfo.Custom(anyAddress, AddressInfo("Remote Name", null), 1, BigInteger.ZERO, "dummyName")
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = transactionInfo.toTransactionInfoViewData(safes)

        assertEquals(
            TransactionInfoViewData.Custom(
                to = anyAddress,
                addressName = "Remote Name",
                addressUri = null,
                dataSize = 1,
                value = BigInteger.ZERO,
                methodName = "dummyName"
            ),
            result
        )
    }


    @Test
    fun `toTransactionInfoViewData() (TransactionInfo_SettingsChange with AddressInfo) should return TransactionInfoViewData_SettingsChange `() {
        val transactionInfo = TransactionInfo.SettingsChange(dummyDataDecoded, SettingsInfo.SetFallbackHandler(anyAddress, null))
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = transactionInfo.toTransactionInfoViewData(safes)

        assertEquals(
            TransactionInfoViewData.SettingsChange(dummyDataDecoded, SettingsInfoViewData.SetFallbackHandler(anyAddress, AddressInfoData.Default)),
            result
        )
    }

    @Test
    fun `toTransactionInfoViewData() (TransactionInfo_Transfer with AddressInfo) should return TransactionInfoViewData_Transfer `() {
        val transactionInfo = TransactionInfo.Transfer(
            anyAddress, AddressInfo("Sender Name", null), anotherAddress, null, TransferInfo.EtherTransfer(
                BigInteger.ONE
            ), TransactionDirection.INCOMING
        )
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = transactionInfo.toTransactionInfoViewData(safes)

        assertEquals(
            TransactionInfoViewData.Transfer(
                address = anyAddress,
                addressName = "Sender Name",
                addressUri = null,
                transferInfo = TransferInfo.EtherTransfer(
                    BigInteger.ONE
                ),
                direction = TransactionDirection.INCOMING
            ),
            result
        )
    }

    @Test
    fun `toTransactionInfoViewData() (TransactionInfo_Transfer direction outgoing) should return otherAddress `() {
        val transactionInfo = TransactionInfo.Transfer(
            anyAddress, AddressInfo("Sender Name", null), anotherAddress, null, TransferInfo.EtherTransfer(
                BigInteger.ONE
            ), TransactionDirection.OUTGOING
        )
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = transactionInfo.toTransactionInfoViewData(safes)

        assertEquals(
            TransactionInfoViewData.Transfer(
                address = anotherAddress,
                addressName = "Local Name",
                addressUri = null,
                transferInfo = TransferInfo.EtherTransfer(
                    BigInteger.ONE
                ),
                direction = TransactionDirection.OUTGOING
            ),
            result
        )
    }

    private val dummyDataDecoded = DataDecoded(
        method = "dummy",
        parameters = null
    )

}
