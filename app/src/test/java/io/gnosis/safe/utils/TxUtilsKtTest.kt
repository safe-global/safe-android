package io.gnosis.safe.utils

import android.content.res.Resources
import io.gnosis.data.BuildConfig
import io.gnosis.data.models.Chain
import io.gnosis.data.models.transaction.DataDecoded
import io.gnosis.data.models.transaction.TransactionDirection
import io.gnosis.data.models.transaction.TransferInfo
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.R
import io.gnosis.safe.ui.transactions.AddressInfoData
import io.gnosis.safe.ui.transactions.details.view.ActionInfoItem
import io.gnosis.safe.ui.transactions.details.viewdata.SettingsInfoViewData
import io.gnosis.safe.ui.transactions.details.viewdata.TransactionInfoViewData
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.decimalAsBigInteger
import java.math.BigInteger

private val anyAddress = "0x0000000000000000000000000000000000001234".asEthereumAddress()!!
private val anotherAddress = "0x0000000000000000000000000000000000004321".asEthereumAddress()!!
private val anyChain = Chain.DEFAULT_CHAIN

class TxUtilsKtTest {

    private val balanceFormatter: BalanceFormatter = BalanceFormatter()
    private val DS = balanceFormatter.decimalSeparator
    private val resources = mockk<Resources>(relaxed = true)

    @Before
    fun setUp() {
        resources.apply {
            every { getString(R.string.default_fallback_handler) } returns "DefaultFallbackHandler"
            every { getString(R.string.unknown_fallback_handler) } returns "Unknown"
            every { getString(R.string.tx_details_change_required_confirmations) } returns "Change required confirmations:"
        }
    }

    @Test
    fun `formattedAmount (Unknown txInfo) should return 0 ETH`() {

        val txInfo = TransactionInfoViewData.Unknown
        val result = txInfo.formattedAmount(anyChain, balanceFormatter)

        assertEquals("0 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}", result)
    }

    @Test
    fun `formattedAmount (Custom txInfo) should return 0 ETH`() {

        val txInfo = TransactionInfoViewData.Custom(
            to = Solidity.Address(BigInteger.ZERO),
            dataSize = 0,
            value = BigInteger.ZERO,
            methodName = null,
            actionInfoAddressName = null,
            actionInfoAddressUri = null
        )
        val result = txInfo.formattedAmount(anyChain, balanceFormatter)

        assertEquals("0 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}", result)
    }

    @Test
    fun `formattedAmount (Custom txInfo with amount) should return negative ETH amount`() {

        val txInfo = TransactionInfoViewData.Custom(
            to = Solidity.Address(BigInteger.ZERO),
            dataSize = 0,
            value = "1000000000000000000".decimalAsBigInteger(),
            methodName = null,
            actionInfoAddressName = null,
            actionInfoAddressUri = null
        )
        val result = txInfo.formattedAmount(anyChain, balanceFormatter)

        assertEquals("-1 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}", result)
    }

    @Test
    fun `formattedAmount (Outgoing Transfer 1 ETH) should return -1 ETH`() {

        val txInfo = TransactionInfoViewData.Transfer(
            address = Solidity.Address(BigInteger.ZERO),
            addressName = null,
            addressUri = null,
            direction = TransactionDirection.OUTGOING,
            transferInfo = buildTransferInfo(value = "1000000000000000000".toBigInteger())
        )
        val result = txInfo.formattedAmount(anyChain, balanceFormatter)

        assertEquals("-1 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}", result)
    }

    @Test
    fun `formattedAmount (Outgoing Transfer 0 ETH) should return 0 ETH`() {

        val txInfo = TransactionInfoViewData.Transfer(
            address = Solidity.Address(BigInteger.ZERO),
            addressName = null,
            addressUri = null,
            direction = TransactionDirection.OUTGOING,
            transferInfo = buildTransferInfo(value = BigInteger.ZERO)
        )
        val result = txInfo.formattedAmount(anyChain, balanceFormatter)

        assertEquals("0 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}", result)
    }

    @Test
    fun `formattedAmount (Incoming WETH Transfer) should return +0_1 WETH`() {

        val txInfo = TransactionInfoViewData.Transfer(
            address = Solidity.Address(BigInteger.ZERO),
            addressName = null,
            addressUri = null,
            direction = TransactionDirection.INCOMING,
            transferInfo = buildErc20TransferInfo(value = BigInteger.ONE)
        )
        val result = txInfo.formattedAmount(anyChain, balanceFormatter)

        assertEquals("+0${DS}1 WETH", result)
    }

    @Test
    fun `formattedAmount (null ERC20 tokenSymbol) should return +0_1 ERC20`() {

        val txInfo = TransactionInfoViewData.Transfer(
            address = Solidity.Address(BigInteger.ZERO),
            addressName = null,
            addressUri = null,
            direction = TransactionDirection.INCOMING,
            transferInfo = buildErc20TransferInfo(value = BigInteger.ONE, tokenSymbol = null)
        )
        val result = txInfo.formattedAmount(anyChain, balanceFormatter)

        assertEquals("+0${DS}1 ERC20", result)
    }

    @Test
    fun `formattedAmount (null ERC721 tokenSymbol) should return +1 NFT`() {

        val txInfo = TransactionInfoViewData.Transfer(
            address = Solidity.Address(BigInteger.ZERO),
            addressName = null,
            addressUri = null,
            direction = TransactionDirection.INCOMING,
            transferInfo = buildErc721TransferInfo(tokenSymbol = null)
        )
        val result = txInfo.formattedAmount(anyChain, balanceFormatter)

        assertEquals("+1 NFT", result)
    }

    @Test
    fun `logoUri (Ether transfer) load ethereum url`() {

        val txInfo = TransactionInfoViewData.Transfer(
            address = Solidity.Address(BigInteger.ZERO),
            addressName = null,
            addressUri = null,
            direction = TransactionDirection.OUTGOING,
            transferInfo = buildTransferInfo(value = "1000000000000000000".toBigInteger())
        )
        val result = txInfo.logoUri(anyChain)

        assertEquals(anyChain.currency.logoUri, result)
    }

    @Test
    fun `logoUri (WETH transfer) load ethereum url`() {

        val txInfo = TransactionInfoViewData.Transfer(
            address = Solidity.Address(BigInteger.ZERO),
            addressName = null,
            addressUri = null,
            direction = TransactionDirection.OUTGOING,
            transferInfo = buildErc20TransferInfo(value = "1000000000000000000".toBigInteger())
        )
        val result = txInfo.logoUri(anyChain)

        assertEquals("dummy", result)
    }


    @Test
    fun `txActionInfoItems (SettingsChange addOwnerWithThreshold) should return Address and Value `() {
        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(
                dataDecoded = dummyDataDecoded,
                settingsInfo = SettingsInfoViewData.AddOwner(anyAddress, null, 1)
            )

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(2, result.size)
        assertEquals(R.string.tx_details_add_owner, result[0].itemLabel)
        val address = (result[0] as ActionInfoItem.Address)
        assertEquals(anyAddress, address.address)
        val value = (result[1] as ActionInfoItem.Value)
        assertEquals(R.string.tx_details_change_required_confirmations, value.itemLabel)
        assertEquals("1", value.value)

    }


    @Test
    fun `txActionInfoItems (Enable Module) result one address item`() {
        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(
                dataDecoded = dummyDataDecoded,
                settingsInfo = SettingsInfoViewData.EnableModule(anyAddress, null)
            )

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(R.string.tx_details_enable_module, result[0].itemLabel)
        assertEquals(1, result.size)
        val address = (result[0] as ActionInfoItem.Address)
        assertEquals(anyAddress, address.address)
    }

    @Test
    fun `txActionInfoItems (Disable Module) result one adress item`() {
        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(
                settingsInfo = SettingsInfoViewData.DisableModule(anyAddress, null),
                dataDecoded = dummyDataDecoded
            )

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(1, result.size)
        val address = (result[0] as ActionInfoItem.Address)
        assertEquals(R.string.tx_details_disable_module, address.itemLabel)
        assertEquals(anyAddress, address.address)
    }

    @Test
    fun `txActionInfoItems (Change Implementation) result one address with label item`() {
        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(
                dataDecoded = dummyDataDecoded,
                settingsInfo = SettingsInfoViewData.ChangeImplementation(SAFE_IMPLEMENTATION_1_1_1, AddressInfoData.Remote("1.1.1", null, null))
            )

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(R.string.tx_details_new_mastercopy, result[0].itemLabel)
        assertEquals(1, result.size)
        val address = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals("1.1.1", address.addressLabel)
        assertEquals(SAFE_IMPLEMENTATION_1_1_1.asEthereumAddressChecksumString(), address.address?.asEthereumAddressChecksumString())
    }

    @Test
    fun `txActionInfoItems (Swap Owner) result two address item`() {
        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(
                dataDecoded = dummyDataDecoded,
                settingsInfo = SettingsInfoViewData.SwapOwner(anyAddress, null, anotherAddress, null)
            )

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(2, result.size)
        val addressOldOwner = (result[0] as ActionInfoItem.Address)
        assertEquals(R.string.tx_details_remove_owner, addressOldOwner.itemLabel)
        assertEquals(anyAddress, addressOldOwner.address)

        val addressNewOwner = (result[1] as ActionInfoItem.Address)
        assertEquals(R.string.tx_details_add_owner, addressNewOwner.itemLabel)
        assertEquals(anotherAddress, addressNewOwner.address)
    }

    @Test
    fun `txActionInfoItems (Swap Owner, local names) result two address items with label`() {
        val localName = "foobar_local"
        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(
                dataDecoded = dummyDataDecoded,
                settingsInfo = SettingsInfoViewData.SwapOwner(
                    oldOwner = anyAddress,
                    oldOwnerInfo = AddressInfoData.Local(
                        name = localName,
                        address = anyAddress.asEthereumAddressChecksumString()
                    ),
                    newOwner = anotherAddress,
                    newOwnerInfo = AddressInfoData.Local(
                        name = localName,
                        address = anyAddress.asEthereumAddressChecksumString()
                    )
                )
            )

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(2, result.size)
        val addressOldOwner = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals(R.string.tx_details_remove_owner, addressOldOwner.itemLabel)
        assertEquals(anyAddress, addressOldOwner.address)

        val addressNewOwner = (result[1] as ActionInfoItem.AddressWithLabel)
        assertEquals(R.string.tx_details_add_owner, addressNewOwner.itemLabel)
        assertEquals(anotherAddress, addressNewOwner.address)
    }

    @Test
    fun `txActionInfoItems (Swap Owner, remote names) result two address item with label`() {
        val remoteName = "foobar_remote"

        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(
                dataDecoded = dummyDataDecoded,
                settingsInfo = SettingsInfoViewData.SwapOwner(
                    oldOwner = anyAddress, oldOwnerInfo = AddressInfoData.Remote(
                        name = remoteName,
                        appInfo = false,
                        address = anyAddress.asEthereumAddressChecksumString(),
                        addressLogoUri = null
                    ), newOwner = anotherAddress,
                    newOwnerInfo = AddressInfoData.Remote(
                        name = remoteName,
                        appInfo = false,
                        address = anyAddress.asEthereumAddressChecksumString(),
                        addressLogoUri = null
                    )
                )
            )

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(2, result.size)
        val addressOldOwner = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals(R.string.tx_details_remove_owner, addressOldOwner.itemLabel)
        assertEquals(anyAddress, addressOldOwner.address)

        val addressNewOwner = (result[1] as ActionInfoItem.AddressWithLabel)
        assertEquals(R.string.tx_details_add_owner, addressNewOwner.itemLabel)
        assertEquals(anotherAddress, addressNewOwner.address)
    }

    @Test
    fun `txActionInfoItems (Remove Owner) result one address item`() {
        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(
                dataDecoded = dummyDataDecoded,
                settingsInfo = SettingsInfoViewData.RemoveOwner(anyAddress, null, 2)
            )

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(2, result.size)
        val addressOwner = (result[0] as ActionInfoItem.Address)
        assertEquals(R.string.tx_details_remove_owner, addressOwner.itemLabel)
        assertEquals(anyAddress, addressOwner.address)

        val value = (result[1] as ActionInfoItem.Value)
        assertEquals(R.string.tx_details_change_required_confirmations, value.itemLabel)
        assertEquals("2", value.value)
    }

    @Test
    fun `txActionInfoItems (Remove Owner with remote name) result one address item with label`() {
        val remoteName = "foobar"
        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(
                dataDecoded = dummyDataDecoded,
                settingsInfo = SettingsInfoViewData.RemoveOwner(
                    owner = anyAddress,
                    ownerInfo = AddressInfoData.Remote(
                        name = remoteName,
                        appInfo = false,
                        address = anyAddress.asEthereumAddressChecksumString(),
                        addressLogoUri = null
                    ),
                    threshold = 2
                )
            )

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(2, result.size)
        val addressOwner = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals(R.string.tx_details_remove_owner, addressOwner.itemLabel)
        assertEquals(anyAddress, addressOwner.address)
        assertEquals(remoteName, addressOwner.addressLabel)

        val value = (result[1] as ActionInfoItem.Value)
        assertEquals(R.string.tx_details_change_required_confirmations, value.itemLabel)
        assertEquals("2", value.value)
    }

    @Test
    fun `txActionInfoItems (Remove Owner with local name) result a local address item with label`() {
        val localName = "foobar"
        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(
                dataDecoded = dummyDataDecoded,
                settingsInfo = SettingsInfoViewData.RemoveOwner(
                    anyAddress,
                    AddressInfoData.Local(
                        name = localName,
                        address = anyAddress.asEthereumAddressChecksumString()
                    ),
                    2
                )
            )

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(2, result.size)
        val addressOwner = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals(R.string.tx_details_remove_owner, addressOwner.itemLabel)
        assertEquals(anyAddress, addressOwner.address)
        assertEquals(localName, addressOwner.addressLabel)

        val value = (result[1] as ActionInfoItem.Value)
        assertEquals(R.string.tx_details_change_required_confirmations, value.itemLabel)
        assertEquals("2", value.value)
    }

    @Test
    fun `txActionInfoItems (Set Fallback Handler ) result one address with label item`() {
        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(
                dataDecoded = dummyDataDecoded,
                settingsInfo = SettingsInfoViewData.SetFallbackHandler(anyAddress, null)
            )

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(1, result.size)
        val addressOwner = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals(R.string.tx_details_set_fallback_handler, addressOwner.itemLabel)
        assertEquals(anyAddress, addressOwner.address)
        assertEquals(resources.getString(R.string.unknown_fallback_handler), addressOwner.addressLabel)
    }

    @Test
    fun `txActionInfoItems (Set Fallback Handler to default) result one address with label item`() {
        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(
                dataDecoded = dummyDataDecoded,
                settingsInfo = SettingsInfoViewData.SetFallbackHandler(SafeRepository.DEFAULT_FALLBACK_HANDLER, null)
            )

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(1, result.size)
        val addressOwner = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals(R.string.tx_details_set_fallback_handler, addressOwner.itemLabel)
        assertEquals(SafeRepository.DEFAULT_FALLBACK_HANDLER, addressOwner.address)
        assertEquals(resources.getString(R.string.default_fallback_handler), addressOwner.addressLabel)
    }

    @Test
    fun `txActionInfoItems (Change threshold ) result one value item`() {
        val settingsChange: TransactionInfoViewData.SettingsChange =
            TransactionInfoViewData.SettingsChange(dataDecoded = dummyDataDecoded, settingsInfo = SettingsInfoViewData.ChangeThreshold(4))

        val result = settingsChange.txActionInfoItems(resources)

        assertEquals(1, result.size)
        val addressOwner = (result[0] as ActionInfoItem.Value)
        assertEquals(R.string.tx_details_change_required_confirmations, addressOwner.itemLabel)
        assertEquals("4", addressOwner.value)
    }

    private fun buildTransferInfo(value: BigInteger): TransferInfo = TransferInfo.NativeTransfer(value)

    private fun buildErc20TransferInfo(value: BigInteger, tokenSymbol: String? = "WETH"): TransferInfo =
        TransferInfo.Erc20Transfer(
            tokenAddress = Solidity.Address(BigInteger.ONE),
            value = value,
            logoUri = "dummy",
            decimals = 1,
            tokenName = "",
            tokenSymbol = tokenSymbol
        )

    private fun buildErc721TransferInfo(tokenSymbol: String? = "CK"): TransferInfo =
        TransferInfo.Erc721Transfer(
            tokenAddress = Solidity.Address(BigInteger.ONE),
            logoUri = "dummy",
            tokenName = "",
            tokenSymbol = tokenSymbol,
            tokenId = "dummy"
        )

    private val dummyDataDecoded = DataDecoded(
        method = "dummy",
        parameters = null
    )

    companion object {
        private val SAFE_IMPLEMENTATION_1_1_1 = "0xb6029EA3B2c51D09a50B53CA8012FeEB05bDa35A".asEthereumAddress()!!
    }
}
