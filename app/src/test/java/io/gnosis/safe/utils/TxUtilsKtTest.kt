package io.gnosis.safe.utils

import io.gnosis.data.models.transaction.*
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_ADD_OWNER_WITH_THRESHOLD
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_CHANGE_MASTER_COPY
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_CHANGE_THRESHOLD
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_DISABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_ENABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_REMOVE_OWNER
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_SET_FALLBACK_HANDLER
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_SWAP_OWNER
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_IMPLEMENTATION_1_1_1
import io.gnosis.safe.R
import io.gnosis.safe.ui.transactions.details.view.ActionInfoItem
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.decimalAsBigInteger
import java.math.BigInteger

private val anyAddress = "0x0000000000000000000000000000000000001234".asEthereumAddress()!!
private val anotherAddress = "0x0000000000000000000000000000000000004321".asEthereumAddress()!!

class TxUtilsKtTest {

    private val balanceFormatter: BalanceFormatter = BalanceFormatter()
    private val DS = balanceFormatter.decimalSeparator

    @Test
    fun `formattedAmount (Unknown txInfo) should return 0 ETH`() {

        val txInfo = TransactionInfo.Unknown
        val result = txInfo.formattedAmount(balanceFormatter)

        assertEquals("0 ETH", result)
    }

    @Test
    fun `formattedAmount (Custom txInfo) should return 0 ETH`() {

        val txInfo = TransactionInfo.Custom(
            to = Solidity.Address(BigInteger.ZERO),
            dataSize = 0,
            value = BigInteger.ZERO
        )
        val result = txInfo.formattedAmount(balanceFormatter)

        assertEquals("0 ETH", result)
    }

    @Test
    fun `formattedAmount (Custom txInfo with amount) should return negative ETH amount`() {

        val txInfo = TransactionInfo.Custom(
            to = Solidity.Address(BigInteger.ZERO),
            dataSize = 0,
            value = "1000000000000000000".decimalAsBigInteger()
        )
        val result = txInfo.formattedAmount(balanceFormatter)

        assertEquals("-1 ETH", result)
    }

    @Test
    fun `formattedAmount (Outgoing Transfer 1 ETH) should return -1 ETH`() {

        val txInfo = TransactionInfo.Transfer(
            sender = Solidity.Address(BigInteger.ZERO),
            recipient = Solidity.Address(BigInteger.ONE),
            direction = TransactionDirection.OUTGOING,
            transferInfo = buildTransferInfo(value = "1000000000000000000".toBigInteger())
        )
        val result = txInfo.formattedAmount(balanceFormatter)

        assertEquals("-1 ETH", result)
    }

    @Test
    fun `formattedAmount (Outgoing Transfer 0 ETH) should return 0 ETH`() {

        val txInfo = TransactionInfo.Transfer(
            sender = Solidity.Address(BigInteger.ZERO),
            recipient = Solidity.Address(BigInteger.ONE),
            direction = TransactionDirection.OUTGOING,
            transferInfo = buildTransferInfo(value = BigInteger.ZERO)
        )
        val result = txInfo.formattedAmount(balanceFormatter)

        assertEquals("0 ETH", result)
    }

    @Test
    fun `formattedAmount (Incoming WETH Transfer) should return +0_1 WETH`() {

        val txInfo = TransactionInfo.Transfer(
            sender = Solidity.Address(BigInteger.ZERO),
            recipient = Solidity.Address(BigInteger.ONE),
            direction = TransactionDirection.INCOMING,
            transferInfo = buildErc20TransferInfo(value = BigInteger.ONE)
        )
        val result = txInfo.formattedAmount(balanceFormatter)

        assertEquals("+0${DS}1 WETH", result)
    }

    @Test
    fun `formattedAmount (null ERC20 tokenSymbol) should return +0_1 ERC20`() {

        val txInfo = TransactionInfo.Transfer(
            sender = Solidity.Address(BigInteger.ZERO),
            recipient = Solidity.Address(BigInteger.ONE),
            direction = TransactionDirection.INCOMING,
            transferInfo = buildErc20TransferInfo(value = BigInteger.ONE, tokenSymbol = null)
        )
        val result = txInfo.formattedAmount(balanceFormatter)

        assertEquals("+0${DS}1 ERC20", result)
    }

    @Test
    fun `formattedAmount (null ERC721 tokenSymbol) should return +1 NFT`() {

        val txInfo = TransactionInfo.Transfer(
            sender = Solidity.Address(BigInteger.ZERO),
            recipient = Solidity.Address(BigInteger.ONE),
            direction = TransactionDirection.INCOMING,
            transferInfo = buildErc721TransferInfo(tokenSymbol = null)
        )
        val result = txInfo.formattedAmount(balanceFormatter)

        assertEquals("+1 NFT", result)
    }

    @Test
    fun `logoUri (Ether transfer) load ethereum url`() {

        val txInfo = TransactionInfo.Transfer(
            sender = Solidity.Address(BigInteger.ZERO),
            recipient = Solidity.Address(BigInteger.ONE),
            direction = TransactionDirection.OUTGOING,
            transferInfo = buildTransferInfo(value = "1000000000000000000".toBigInteger())
        )
        val result = txInfo.logoUri()

        assertEquals("local::ethereum", result)
    }


    @Test
    fun `logoUri (WETH transfer) load ethereum url`() {

        val txInfo = TransactionInfo.Transfer(
            sender = Solidity.Address(BigInteger.ZERO),
            recipient = Solidity.Address(BigInteger.ONE),
            direction = TransactionDirection.OUTGOING,
            transferInfo = buildErc20TransferInfo(value = "1000000000000000000".toBigInteger())
        )
        val result = txInfo.logoUri()

        assertEquals("dummy", result)
    }


    @Test
    fun `txActionInfoItems (SettingsChange addOwnerWithThreshold) should return Address and Value `() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecoded(
                    method = METHOD_ADD_OWNER_WITH_THRESHOLD,
                    parameters = listOf(
                        Param.Value("uint256", "_threshold", "1"),
                        Param.Address("address", "owner", anyAddress)
                    )
                ),
                settingsInfo = null
            )

        val result = settingsChange.txActionInfoItems()

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
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecoded(
                    method = METHOD_ENABLE_MODULE,
                    parameters = listOf(
                        Param.Address("address", "module", anyAddress)
                    )
                ),
                settingsInfo = null
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(R.string.tx_details_enable_module, result[0].itemLabel)
        assertEquals(1, result.size)
        val address = (result[0] as ActionInfoItem.Address)
        assertEquals(anyAddress, address.address)
    }

    @Test
    fun `txActionInfoItems (Disable Module) result one adress item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecoded(
                    method = METHOD_DISABLE_MODULE,
                    parameters = listOf(
                        Param.Address("address", "module", anyAddress)
                    )
                ), settingsInfo = null
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(1, result.size)
        val address = (result[0] as ActionInfoItem.Address)
        assertEquals(R.string.tx_details_disable_module, address.itemLabel)
        assertEquals(anyAddress, address.address)
    }

    @Test
    fun `txActionInfoItems (Change Implementation) result one address with label item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecoded(
                    method = METHOD_CHANGE_MASTER_COPY,
                    parameters = listOf(
                        Param.Address("address", "_masterCopy", SAFE_IMPLEMENTATION_1_1_1)
                    )
                ), settingsInfo = null
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(R.string.tx_details_new_mastercopy, result[0].itemLabel)
        assertEquals(1, result.size)
        val address = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals(io.gnosis.data.R.string.implementation_version_1_1_1, address.addressLabel)
        assertEquals(SAFE_IMPLEMENTATION_1_1_1.asEthereumAddressChecksumString(), address.address?.asEthereumAddressChecksumString())
    }

    @Test
    fun `txActionInfoItems (Swap Owner) result two address item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecoded(
                    method = METHOD_SWAP_OWNER,
                    parameters = listOf(
                        Param.Address("address", "oldOwner", anyAddress),
                        Param.Address("address", "newOwner", anotherAddress)
                    )
                ), settingsInfo = null
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(2, result.size)
        val addressOldOwner = (result[0] as ActionInfoItem.Address)
        assertEquals(R.string.tx_details_remove_owner, addressOldOwner.itemLabel)
        assertEquals(anyAddress, addressOldOwner.address)

        val addressNewOwner = (result[1] as ActionInfoItem.Address)
        assertEquals(R.string.tx_details_add_owner, addressNewOwner.itemLabel)
        assertEquals(anotherAddress, addressNewOwner.address)

    }

    @Test
    fun `txActionInfoItems (Remove Owner) result one address item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecoded(
                    method = METHOD_REMOVE_OWNER,
                    parameters = listOf(
                        Param.Address("address", "owner", anyAddress),
                        Param.Value("uint256", "_threshold", "2")
                    )
                ), settingsInfo = null
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(2, result.size)
        val addressOwner = (result[0] as ActionInfoItem.Address)
        assertEquals(R.string.tx_details_remove_owner, addressOwner.itemLabel)
        assertEquals(anyAddress, addressOwner.address)

        val value = (result[1] as ActionInfoItem.Value)
        assertEquals(R.string.tx_details_change_required_confirmations, value.itemLabel)
        assertEquals("2", value.value)
    }

    @Test
    fun `txActionInfoItems (Set Fallback Handler ) result one address with label item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecoded(
                    method = METHOD_SET_FALLBACK_HANDLER,
                    parameters = listOf(
                        Param.Address("address", "handler", anyAddress)
                    )
                ), settingsInfo = null
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(1, result.size)
        val addressOwner = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals(R.string.tx_details_set_fallback_handler, addressOwner.itemLabel)
        assertEquals(anyAddress, addressOwner.address)
        assertEquals(io.gnosis.data.R.string.unknown_fallback_handler, addressOwner.addressLabel)
    }

    @Test
    fun `txActionInfoItems (Set Fallback Handler to default) result one address with label item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecoded(
                    method = METHOD_SET_FALLBACK_HANDLER,
                    parameters = listOf(
                        Param.Address(
                            "address",
                            "handler",
                            SafeRepository.DEFAULT_FALLBACK_HANDLER
                        )
                    )
                ), settingsInfo = null
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(1, result.size)
        val addressOwner = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals(R.string.tx_details_set_fallback_handler, addressOwner.itemLabel)
        assertEquals(SafeRepository.DEFAULT_FALLBACK_HANDLER, addressOwner.address)
        assertEquals(io.gnosis.data.R.string.default_fallback_handler, addressOwner.addressLabel)
    }

    @Test
    fun `txActionInfoItems (Change threshold ) result one value item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecoded(
                    method = METHOD_CHANGE_THRESHOLD,
                    parameters = listOf(
                        Param.Value("uint256", "_threshold", "4")
                    )
                ), settingsInfo = null
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(1, result.size)
        val addressOwner = (result[0] as ActionInfoItem.Value)
        assertEquals(R.string.tx_details_change_required_confirmations, addressOwner.itemLabel)
        assertEquals("4", addressOwner.value)
    }

    private fun buildTransferInfo(value: BigInteger): TransferInfo = TransferInfo.EtherTransfer(value)

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
}
