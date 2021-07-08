package io.gnosis.data.models.transaction

import org.junit.Assert.assertTrue
import org.junit.Test

class DataDecodedKtTest {

    @Test
    fun `getParamItemType (value) should return ParamType_VALUE`() {

        val result = getParamItemType("uint256[18]")
        assertTrue(result == ParamType.VALUE)
    }

    @Test
    fun `getParamItemType (address) should return ParamType_ADDRESS`() {

        val result = getParamItemType("address[14]")
        assertTrue(result == ParamType.ADDRESS)
    }

    @Test
    fun `getParamItemType (bytes) should return ParamType_BYTES`() {

        val result = getParamItemType("bytes32[5]")
        assertTrue(result == ParamType.BYTES)
    }

    @Test
    fun `getParamItemType (mixed) should return ParamType_MIXED`() {

        val result = getParamItemType("(")
        assertTrue(result == ParamType.MIXED)
    }
}
