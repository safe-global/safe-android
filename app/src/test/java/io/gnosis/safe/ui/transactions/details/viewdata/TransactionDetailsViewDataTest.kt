package io.gnosis.safe.ui.transactions.details.viewdata

import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.*
import io.gnosis.safe.ui.transactions.AddressInfoData
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

class TransactionDetailsViewDataTest {

    private val anyAddress = "0x0000000000000000000000000000000000000001".asEthereumAddress()!!
    private val anotherAddress = "0x0000000000000000000000000000000000000002".asEthereumAddress()!!
    private val aSafeAppInfo = SafeAppInfo("app name", "http://www.de", "http://www.de/image.png")

    @Test
    fun `toAddressInfoData() (Address matches local safe) should return AddressInfoData_Local`() {
        val addressInfo = AddressInfo(anyAddress, "Foo", "https://www.foo.de/foo.png")
        val safes = listOf(Safe(anyAddress, "Local Name"))

        val result = addressInfo.toAddressInfoData(safes = safes, safeAppInfo = aSafeAppInfo)

        assertEquals(AddressInfoData.Local("Local Name", "0x0000000000000000000000000000000000000001"), result)
    }

    @Test
    fun `toAddressInfoData() (Address does not match local safe) should return AddressInfoData_Remote`() {
        val addressInfo = AddressInfo(anotherAddress, "Foo", "https://www.foo.de/foo.png")
        val safes = listOf(Safe(anyAddress, "Local Name"))

        val result = addressInfo.toAddressInfoData(safes, null)

        assertEquals(AddressInfoData.Remote("Foo", "https://www.foo.de/foo.png", "0x0000000000000000000000000000000000000002"), result)
    }

    @Test
    fun `toAddressInfoData() (Address matches local owner) should return AddressInfoData_Local`() {
        val addressInfo = AddressInfo(anyAddress, "Foo", "https://www.foo.de/foo.png")
        val owners = listOf(Owner(address = anyAddress, name = "Local owner Name", type = Owner.Type.IMPORTED))

        val result = addressInfo.toAddressInfoData(safes = emptyList(), safeAppInfo = aSafeAppInfo, owners = owners)

        assertEquals(AddressInfoData.Local("Local owner Name", "0x0000000000000000000000000000000000000001"), result)
    }

    @Test
    fun `toAddressInfoData() (Address does not match any local owner) should return AddressInfoData_Remote`() {
        val addressInfo = AddressInfo(anotherAddress, "Foo", "https://www.foo.de/foo.png")
        val owners = listOf(Owner(address = anyAddress, name = "Local owner Name", type = Owner.Type.IMPORTED))

        val result = addressInfo.toAddressInfoData(emptyList(), null, owners)

        assertEquals(AddressInfoData.Remote("Foo", "https://www.foo.de/foo.png", "0x0000000000000000000000000000000000000002"), result)
    }

    @Test
    fun `toSettingsInfoViewData() (DisableModule with AddressInfo) should return DisableModuleViewData `() {
        val settingsInfo = SettingsInfo.DisableModule(AddressInfo(anyAddress, "Remote Name", null))
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = settingsInfo.toSettingsInfoViewData(safes, owners = emptyList(), safeAppInfo = null)

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
        val settingsInfo = SettingsInfo.SwapOwner(AddressInfo(anyAddress, "Remote Old Owner Name", null), AddressInfo(anotherAddress))
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = settingsInfo.toSettingsInfoViewData(safes, owners = emptyList(), safeAppInfo = null)

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
        val settingsInfo = SettingsInfo.ChangeImplementation(AddressInfo(anyAddress, "Remote Name", null))
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = settingsInfo.toSettingsInfoViewData(safes = safes, owners = emptyList())

        assertEquals(
            SettingsInfoViewData.ChangeImplementation(
                anyAddress,
                AddressInfoData.Remote("Remote Name", null, anyAddress.asEthereumAddressChecksumString())
            ),
            result
        )
    }


    @Test
    fun `toTransactionInfoViewData() (TransactionInfo_Custom with AddressInfo and isCancellation) should return TransactionInfoViewData_Rejection `() {
        val transactionInfo = TransactionInfo.Custom(AddressInfo(anyAddress, "Remote Name", null), 1, BigInteger.ZERO, "dummyName", null, true)
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = transactionInfo.toTransactionInfoViewData(safes = safes, owners = emptyList())

        assertEquals(
            TransactionInfoViewData.Rejection(
                to = anyAddress,
                addressName = "Remote Name",
                addressUri = null
            ),
            result
        )
    }

    @Test
    fun `toTransactionInfoViewData() (TransactionInfo_SettingsChange with AddressInfo) should return TransactionInfoViewData_SettingsChange `() {
        val transactionInfo = TransactionInfo.SettingsChange(dummyDataDecoded, SettingsInfo.SetFallbackHandler(AddressInfo(anyAddress)))
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = transactionInfo.toTransactionInfoViewData(safes = safes, owners = emptyList())

        assertEquals(
            TransactionInfoViewData.SettingsChange(dummyDataDecoded, SettingsInfoViewData.SetFallbackHandler(anyAddress, AddressInfoData.Default)),
            result
        )
    }

    @Test
    fun `toTransactionInfoViewData() (TransactionInfo_Transfer with AddressInfo) should return TransactionInfoViewData_Transfer `() {
        val transactionInfo = TransactionInfo.Transfer(
            AddressInfo(anyAddress, "Sender Name", null), AddressInfo(anotherAddress), TransferInfo.NativeTransfer(
                BigInteger.ONE
            ), TransactionDirection.INCOMING
        )
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = transactionInfo.toTransactionInfoViewData(safes = safes, owners = emptyList())

        assertEquals(
            TransactionInfoViewData.Transfer(
                address = anyAddress,
                addressName = "Sender Name",
                addressUri = null,
                transferInfo = TransferInfo.NativeTransfer(
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
            AddressInfo(anyAddress, "Sender Name", null), AddressInfo(anotherAddress), TransferInfo.NativeTransfer(
                BigInteger.ONE
            ), TransactionDirection.OUTGOING
        )
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = transactionInfo.toTransactionInfoViewData(safes = safes, owners = emptyList(), safeAppInfo = aSafeAppInfo)

        assertEquals(
            TransactionInfoViewData.Transfer(
                address = anotherAddress,
                addressName = "Local Name",
                addressUri = null,
                transferInfo = TransferInfo.NativeTransfer(
                    BigInteger.ONE
                ),
                direction = TransactionDirection.OUTGOING
            ),
            result
        )
    }

    @Test
    fun `toTransactionInfoViewData() (TransactionInfo_Transfer with AddressInfo and safeAppInfo) should ignore safeAppInfo`() {
        val transactionInfo = TransactionInfo.Transfer(
            AddressInfo(anyAddress, "Sender Name", null), AddressInfo(anotherAddress), TransferInfo.NativeTransfer(
                BigInteger.ONE
            ), TransactionDirection.INCOMING
        )
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val result = transactionInfo.toTransactionInfoViewData(safes = safes, owners = emptyList(), safeAppInfo = aSafeAppInfo)

        assertEquals(
            TransactionInfoViewData.Transfer(
                address = anyAddress,
                addressName = "Sender Name",
                addressUri = null,
                transferInfo = TransferInfo.NativeTransfer(
                    BigInteger.ONE
                ),
                direction = TransactionDirection.INCOMING
            ),
            result
        )
    }

    @Test
    fun `toTransactionInfoViewData() (TransactionInfo_Custom with non matching local name, AddressInfo and safeAppInfo) should return name and uri from safeAppInfo `() {
        val transactionInfo = TransactionInfo.Custom(AddressInfo(anyAddress, "Remote Name", null), 1, BigInteger.ZERO, "dummyName", null, false)
        val safes = listOf(Safe(anotherAddress, "Local Name"))

        val transactionInfoViewData = transactionInfo.toTransactionInfoViewData(safes = safes, owners = emptyList(), safeAppInfo = aSafeAppInfo)

        assertEquals(
            TransactionInfoViewData.Custom(
                to = anyAddress,
                actionInfoAddressName = "Remote Name",
                actionInfoAddressUri = null,
                statusTitle = "app name",
                statusIconUri = "http://www.de/image.png",
                safeApp = true,
                value = BigInteger.ZERO,
                dataSize = 1,
                methodName = "dummyName"
            ),
            transactionInfoViewData
        )
    }

    @Test
    fun `toTransactionInfoViewData() (TransactionInfo_Custom with matching local name and safeAppInfo) should return local name for actionInfo and safeAppInfo for statusTitle`() {
        val transactionInfo = TransactionInfo.Custom(AddressInfo(anyAddress, "Remote Name", null), 1, BigInteger.ZERO, "dummyName", null, false)
        val safes = listOf(Safe(anyAddress, "Local Name"))

        val transactionInfoViewData = transactionInfo.toTransactionInfoViewData(safes = safes, owners = emptyList(), safeAppInfo = aSafeAppInfo)

        assertEquals(
            TransactionInfoViewData.Custom(
                to = anyAddress,
                statusTitle = "app name",
                statusIconUri = "http://www.de/image.png",
                actionInfoAddressName = "Local Name",
                actionInfoAddressUri = null,
                safeApp = true,
                value = BigInteger.ZERO,
                dataSize = 1,
                methodName = "dummyName"
            ),
            transactionInfoViewData
        )
    }

    private val dummyDataDecoded = DataDecoded(
        method = "dummy",
        parameters = null
    )

}
