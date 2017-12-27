package pm.gnosis.heimdall.data.repositories.models

import pm.gnosis.models.Wei
import java.math.BigInteger


data class GasEstimate(val gasCosts: BigInteger, val gasPrice: Wei)
