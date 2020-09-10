package io.gnosis.safe.utils

import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.ParamsDto
import io.gnosis.data.backend.dto.TransactionDirection
import io.gnosis.data.models.TransactionInfo
import io.gnosis.data.models.TransferInfo
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_ADD_OWNER_WITH_THRESHOLD
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_CHANGE_MASTER_COPY
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_CHANGE_THRESHOLD
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_DISABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_ENABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_REMOVE_OWNER
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_SET_FALLBACK_HANDLER
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_SWAP_OWNER
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_MASTER_COPY_1_1_1
import io.gnosis.safe.R
import io.gnosis.safe.ui.transactions.details.view.ActionInfoItem
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

private const val anyAddressString = "0x0000000000000000000000000000000000001234"
private const val anotherAddressString = "0x0000000000000000000000000000000000004321"

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
            transferInfo = buildErc20TransferInfo(value = BigInteger.ZERO)
        )
        val result = txInfo.formattedAmount(balanceFormatter)

        assertEquals("+0${DS}1 WETH", result)
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
    fun `txActionInfoItems (SettingsChange addOwnerWithThreÍhold) should return Addres & Value `() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecodedDto(
                    method = METHOD_ADD_OWNER_WITH_THRESHOLD,
                    parameters = listOf(ParamsDto("_threshold", "", "1"), ParamsDto("owner", "address", anyAddressString))
                )
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(2, result.size)
        assertEquals(R.string.tx_details_add_owner, result[0].itemLabel)
        val address = (result[0] as ActionInfoItem.Address)
        assertEquals(anyAddressString, address.address?.asEthereumAddressChecksumString())
        val value = (result[1] as ActionInfoItem.Value)
        assertEquals(R.string.tx_details_change_required_confirmations, value.itemLabel)
        assertEquals("1", value.value)

    }


    @Test
    fun `txActionInfoItems (Enable Module) result one address item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecodedDto(
                    method = METHOD_ENABLE_MODULE,
                    parameters = listOf(
                        ParamsDto("module", "address", anyAddressString)
                    )
                )
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(R.string.tx_details_enable_module, result[0].itemLabel)
        assertEquals(1, result.size)
        val address = (result[0] as ActionInfoItem.Address)
        assertEquals(anyAddressString, address.address?.asEthereumAddressChecksumString())
    }

    @Test
    fun `txActionInfoItems (Disable Module) result one adress item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecodedDto(
                    method = METHOD_DISABLE_MODULE,
                    parameters = listOf(
                        ParamsDto("module", "address", anyAddressString)
                    )
                )
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(1, result.size)
        val address = (result[0] as ActionInfoItem.Address)
        assertEquals(R.string.tx_details_disable_module, address.itemLabel)
        assertEquals(anyAddressString, address.address?.asEthereumAddressChecksumString())
    }

    @Test
    fun `txActionInfoItems (Change Implementation) result one address with label item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecodedDto(
                    method = METHOD_CHANGE_MASTER_COPY,
                    parameters = listOf(
                        ParamsDto("_masterCopy", "address", SAFE_MASTER_COPY_1_1_1.asEthereumAddressChecksumString())
                    )
                )
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(R.string.tx_details_new_mastercopy, result[0].itemLabel)
        assertEquals(1, result.size)
        val address = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals("1.1.1", address.addressLabel)
        assertEquals(SAFE_MASTER_COPY_1_1_1.asEthereumAddressChecksumString(), address.address?.asEthereumAddressChecksumString())
    }

    @Test
    fun `txActionInfoItems (Swap Owner) result two address item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecodedDto(
                    method = METHOD_SWAP_OWNER,
                    parameters = listOf(
                        ParamsDto("oldOwner", "address", anyAddressString),
                        ParamsDto("newOwner", "address", anotherAddressString)
                    )
                )
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(2, result.size)
        val addressOldOwner = (result[0] as ActionInfoItem.Address)
        assertEquals(R.string.tx_details_remove_owner, addressOldOwner.itemLabel)
        assertEquals(anyAddressString.asEthereumAddress(), addressOldOwner.address)

        val addressNewOwner = (result[1] as ActionInfoItem.Address)
        assertEquals(R.string.tx_details_add_owner, addressNewOwner.itemLabel)
        assertEquals(anotherAddressString.asEthereumAddress(), addressNewOwner.address)

    }

    @Test
    fun `txActionInfoItems (Remove Owner) result one address item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecodedDto(
                    method = METHOD_REMOVE_OWNER,
                    parameters = listOf(
                        ParamsDto("owner", "address", anyAddressString),
                        ParamsDto("_threshold", "", "2")
                    )
                )
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(2, result.size)
        val addressOwner = (result[0] as ActionInfoItem.Address)
        assertEquals(R.string.tx_details_remove_owner, addressOwner.itemLabel)
        assertEquals(anyAddressString.asEthereumAddress(), addressOwner.address)

        val value = (result[1] as ActionInfoItem.Value)
        assertEquals(R.string.tx_details_change_required_confirmations, value.itemLabel)
        assertEquals("2", value.value)
    }

    @Test
    fun `txActionInfoItems (Set Fallback Handler ) result one address with label item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecodedDto(
                    method = METHOD_SET_FALLBACK_HANDLER,
                    parameters = listOf(
                        ParamsDto("handler", "address", anyAddressString)
                    )
                )
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(1, result.size)
        val addressOwner = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals(R.string.tx_details_set_fallback_handler, addressOwner.itemLabel)
        assertEquals(anyAddressString.asEthereumAddress(), addressOwner.address)
        assertEquals(null, addressOwner.addressLabel)
        assertEquals(R.string.tx_list_default_fallback_handler_unknown, addressOwner.addressLabelRes)
    }

    @Test
    fun `txActionInfoItems (Set Fallback Handler to default) result one address with label item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecodedDto(
                    method = METHOD_SET_FALLBACK_HANDLER,
                    parameters = listOf(
                        ParamsDto("handler", "address", SafeRepository.DEFAULT_FALLBACK_HANDLER.asEthereumAddressString())
                    )
                )
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(1, result.size)
        val addressOwner = (result[0] as ActionInfoItem.AddressWithLabel)
        assertEquals(R.string.tx_details_set_fallback_handler, addressOwner.itemLabel)
        assertEquals(SafeRepository.DEFAULT_FALLBACK_HANDLER, addressOwner.address)
        assertEquals(null, addressOwner.addressLabel)
        assertEquals(R.string.tx_list_default_fallback_handler, addressOwner.addressLabelRes)
    }

    @Test
    fun `txActionInfoItems (Change threshold ) result one value item`() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecodedDto(
                    method = METHOD_CHANGE_THRESHOLD,
                    parameters = listOf(
                        ParamsDto("_threshold", "", "4")
                    )
                )
            )

        val result = settingsChange.txActionInfoItems()

        assertEquals(1, result.size)
        val addressOwner = (result[0] as ActionInfoItem.Value)
        assertEquals(R.string.tx_details_change_required_confirmations, addressOwner.itemLabel)
        assertEquals("4", addressOwner.value)
    }

    private fun buildTransferInfo(value: BigInteger): TransferInfo = TransferInfo.EtherTransfer(value)
    private fun buildErc20TransferInfo(value: BigInteger): TransferInfo =
        TransferInfo.Erc20Transfer(
            tokenAddress = Solidity.Address(BigInteger.ONE),
            value = BigInteger.ONE,
            logoUri = "dummy",
            decimals = 1,
            tokenName = "",
            tokenSymbol = "WETH"
        )
}
