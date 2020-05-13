package io.gnosis.data.backend.dto

import io.gnosis.data.models.Erc20Token
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import java.math.BigInteger

class ServiceTokenInfoTest {

    @Test
    fun `toErc20Token (ServiceTokenInfo all fields defined) should return Erc20Token`() {
        val serviceToken = ServiceTokenInfo(
            Solidity.Address(BigInteger.ZERO),
            15,
            "symbol",
            "name",
            "logoUri"
        )

        val expected = Erc20Token(
            Solidity.Address(BigInteger.ZERO),
            "name",
            "symbol",
            15,
            "logoUri"
        )

        val actual = serviceToken.toErc20Token()

        assertEquals(expected, actual)
    }

    @Test
    fun `toErc20Token (ServiceTokenInfo null LogoUri) should return Erc20Token`() {
        val tokenAddress = Solidity.Address(BigInteger.ZERO)

        val serviceToken = ServiceTokenInfo(
            tokenAddress,
            15,
            "symbol",
            "name",
            null
        )

        val expected = Erc20Token(
            tokenAddress,
            "name",
            "symbol",
            15,
            "https://gnosis-safe-token-logos.s3.amazonaws.com/${tokenAddress.asEthereumAddressChecksumString()}.png"
        )

        val actual = serviceToken.toErc20Token()

        assertEquals(expected, actual)
    }
}
