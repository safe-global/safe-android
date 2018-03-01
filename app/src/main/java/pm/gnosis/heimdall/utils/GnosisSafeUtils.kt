package pm.gnosis.heimdall.utils

import pm.gnosis.model.Solidity
import java.math.BigInteger

object GnosisSafeUtils {
    fun calculateThreshold(owners: Int): Int =
        Math.max(1, owners - 1)

    fun calculateThresholdAsUInt8(owners: Int): Solidity.UInt8 =
        Solidity.UInt8(BigInteger.valueOf(calculateThreshold(owners).toLong()))
}
